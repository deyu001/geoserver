/*
 *==Description==
 *GeoServer is an open source software server written in Java that allows users
 *          to share and edit geospatial data.Designed for interoperability,
 *          it publishes data from any major spatial data source using open standards.
 *
 *Being a community-driven project, GeoServer is developed, tested, and supported by
 *      a diverse group of individuals and organizations from around the world.
 *
 *GeoServer is the reference implementation of the Open Geospatial Consortium (OGC)
 *          Web Feature Service (WFS) and Web Coverage Service (WCS) standards, as well as
 *          a high performance certified compliant Web Map Service (WMS), compliant
 *          Catalog Service for the Web (CSW) and implementing Web Processing Service (WPS).
 *          GeoServer forms a core component of the Geospatial Web.
 *
 *==License==
 *GeoServer is distributed under the GNU General Public License Version 2.0 license:
 *
 *    GeoServer, open geospatial information server
 *    Copyright (C) 2014-2020 Open Source Geospatial Foundation.
 *    Copyright (C) 2001-2014 OpenPlans
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version (collectively, "GPL").
 *
 *    As an exception to the terms of the GPL, you may copy, modify,
 *    propagate, and distribute a work formed by combining GeoServer with the
 *    EMF and XSD Libraries, or a work derivative of such a combination, even if
 *    such copying, modification, propagation, or distribution would otherwise
 *    violate the terms of the GPL. Nothing in this exception exempts you from
 *    complying with the GPL in all respects for all of the code used other
 *    than the EMF and XSD Libraries. You may include this exception and its grant
 *    of permissions when you distribute GeoServer.  Inclusion of this notice
 *    with such a distribution constitutes a grant of such permissions.  If
 *    you do not wish to grant these permissions, remove this paragraph from
 *    your distribution. "GeoServer" means the GeoServer software licensed
 *    under version 2 or any later version of the GPL, or a work based on such
 *    software and licensed under the GPL. "EMF and XSD Libraries" means
 *    Eclipse Modeling Framework Project and XML Schema Definition software
 *    distributed by the Eclipse Foundation, all licensed
 *    under the Eclipse Public License Version 1.0 ("EPL"), or a work based on
 *    such software and licensed under the EPL.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA 02110-1335  USA
 *
 *==More Information==
 *Visit the website or read the docs.
 */

package org.geoserver.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.security.config.BruteForcePreventionConfig;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureProviderNotFoundEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Brute force attack preventer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class BruteForceListener
        implements ApplicationListener<AbstractAuthenticationEvent>, GeoServerLifecycleHandler {

    static final Logger LOGGER = Logging.getLogger(BruteForceListener.class);

    /**
     * Simple single node delayed login tracker. Should be made pluggable to allow by some sort of
     * network service for a clustered installation
     */
    Map<String, AtomicInteger> delayedUsers = new ConcurrentHashMap<>();

    private GeoServerSecurityManager securityManager;

    public BruteForceListener(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    private BruteForcePreventionConfig getConfig() {
        BruteForcePreventionConfig config =
                securityManager.getSecurityConfig().getBruteForcePrevention();
        if (config == null) {
            return BruteForcePreventionConfig.DEFAULT;
        } else {
            return config;
        }
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        // is it enabled?
        BruteForcePreventionConfig config = getConfig();
        if (!config.isEnabled()) {
            return;
        }

        // some addresses can be whitelisted and allowed to login anyways
        HttpServletRequest request = GeoServerSecurityFilterChainProxy.REQUEST.get();
        if (requestAddressInWhiteList(request, config)) {
            return;
        }

        // Yes, enabled, check for concurrent login attempt
        Authentication authentication = event.getAuthentication();
        String name = getUserName(authentication);
        if (name == null) {
            LOGGER.warning(
                    "Brute force attack prevention enabled, but Spring Authentication "
                            + "does not provide a user name, skipping: "
                            + authentication);
        }

        // do we have a delayed login in flight already? If so, kill this login attempt
        // no matter if successful or not
        final AtomicInteger counter = delayedUsers.get(name);
        if (counter != null) {
            int count = counter.incrementAndGet();
            logFailedRequest(request, name, count);
            throw new ConcurrentAuthenticationException(name, count);
        }

        if (event instanceof AuthenticationFailureBadCredentialsEvent
                || event instanceof AuthenticationFailureProviderNotFoundEvent) {
            // are we above the max number of blocked threads already?
            int maxBlockedThreads = config.getMaxBlockedThreads();
            if (maxBlockedThreads > 0 && delayedUsers.size() > maxBlockedThreads) {
                throw new MaxBlockedThreadsException(1);
            }

            delayedUsers.put(name, new AtomicInteger(1));
            try {
                logFailedRequest(request, name, 0);
                long delay = computeDelay(config);
                if (delay > 0) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info(
                                "Brute force attack prevention, delaying login for "
                                        + delay
                                        + "ms");
                    }
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                // duh
            } finally {
                delayedUsers.remove(name);
            }
        }
    }

    private boolean requestAddressInWhiteList(
            HttpServletRequest request, BruteForcePreventionConfig config) {
        // is there a white list?
        if (config.getWhitelistAddressMatchers() == null) {
            return false;
        }

        return config.getWhitelistAddressMatchers()
                .stream()
                .anyMatch(matcher -> matcher.matches(request));
    }

    private long computeDelay(BruteForcePreventionConfig config) {
        long min = config.getMinDelaySeconds() * 1000;
        long max = config.getMaxDelaySeconds() * 1000;
        return min + (long) ((max - min) * Math.random());
    }

    /** Returns the username for this authentication, or null if missing or cannot be determined */
    private String getUserName(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal != null) {
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }
        }
        return authentication.getName();
    }

    private void logFailedRequest(HttpServletRequest request, String name, int count) {
        StringBuilder sb = new StringBuilder("Failed login, user ").append(name).append(" from ");
        sb.append(request.getRemoteAddr());
        // log x-forwarded-for too, but not exclusively as it can be spoofed
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            sb.append(", forwarded for ").append(forwardedFor);
        }
        if (count > 0) {
            sb.append(", stopped ")
                    .append(count)
                    .append(" concurrent logins during authentication delay");
        }

        LOGGER.warning(sb.toString());
    }

    @Override
    public void onReset() {
        delayedUsers.clear();
    }

    @Override
    public void onDispose() {
        // nothing to do
    }

    @Override
    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onReload() {
        delayedUsers.clear();
    }
}
