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

package org.geoserver.security.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Configuration for brute force attack preventer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class BruteForcePreventionConfig implements SecurityConfig {

    static final Logger LOGGER = Logging.getLogger(BruteForcePreventionConfig.class);

    private static final long serialVersionUID = 5774047555637121124L;

    /** Default brute force attack configuration */
    public static final BruteForcePreventionConfig DEFAULT = new BruteForcePreventionConfig();

    boolean enabled;

    int minDelaySeconds;

    int maxDelaySeconds;

    int maxBlockedThreads;

    List<String> whitelistedMasks;

    transient List<IpAddressMatcher> whitelistedAddressMatchers;

    /** Configuration based on defaults */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public BruteForcePreventionConfig() {
        this.enabled = true;
        this.minDelaySeconds = 1;
        this.maxDelaySeconds = 5;
        this.whitelistedMasks = new ArrayList<>();
        this.whitelistedMasks.add("127.0.0.1");
        this.maxBlockedThreads = 100;
    }

    public BruteForcePreventionConfig(BruteForcePreventionConfig other) {
        this.enabled = other.enabled;
        this.minDelaySeconds = other.minDelaySeconds;
        this.maxDelaySeconds = other.maxDelaySeconds;
        this.whitelistedMasks =
                other.whitelistedMasks != null ? new ArrayList<>(other.whitelistedMasks) : null;
        this.maxBlockedThreads = other.maxBlockedThreads;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinDelaySeconds() {
        return minDelaySeconds;
    }

    public void setMinDelaySeconds(int minConfig) {
        this.minDelaySeconds = minConfig;
    }

    public int getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMaxDelaySeconds(int maxConfig) {
        this.maxDelaySeconds = maxConfig;
    }

    public List<String> getWhitelistedMasks() {
        return whitelistedMasks;
    }

    public void setWhitelistedMasks(List<String> whitelistedMasks) {
        this.whitelistedMasks = whitelistedMasks;
        if (whitelistedMasks == null) {
            this.whitelistedAddressMatchers = null;
        }
    }

    public List<IpAddressMatcher> getWhitelistAddressMatchers() {
        try {
            if (this.getWhitelistedMasks() != null && this.whitelistedAddressMatchers == null) {
                this.whitelistedAddressMatchers =
                        whitelistedMasks
                                .stream()
                                .map(mask -> new IpAddressMatcher(mask))
                                .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // an error here and no request can be made, best be cautious (yes, it actually
            // happened to me)
            LOGGER.log(Level.SEVERE, "Invalid netmask configuration, will skip it", e);
        }
        return this.whitelistedAddressMatchers;
    }

    public int getMaxBlockedThreads() {
        return maxBlockedThreads;
    }

    public void setMaxBlockedThreads(int maxBlockedThreads) {
        this.maxBlockedThreads = maxBlockedThreads;
    }

    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {
        BruteForcePreventionConfig clone = new BruteForcePreventionConfig(this);

        // allow parametrization of the whitelisted masks
        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);
        if (clone != null) {
            if (allowEnvParametrization
                    && gsEnvironment != null
                    && GeoServerEnvironment.allowEnvParametrization()) {
                List<String> resolvedMasks = new ArrayList<>();
                for (String mask : whitelistedMasks) {
                    String resolved = (String) gsEnvironment.resolveValue(mask);
                    if (resolved != null) {
                        Arrays.stream(resolved.split("\\s*,\\s*"))
                                .filter(s -> s != null && !s.trim().isEmpty())
                                .forEach(s -> resolvedMasks.add(s));
                    }
                }
                clone.setWhitelistedMasks(resolvedMasks);
            }
        }

        return clone;
    }
}
