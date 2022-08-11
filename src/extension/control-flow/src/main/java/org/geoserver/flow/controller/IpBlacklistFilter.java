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

package org.geoserver.flow.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;

/**
 * A class that allows the configuration of an ip black list, rejecting requests from ip addresses
 * configured in the controlflow.properties file
 *
 * @author Juan Marin, OpenGeo
 */
public class IpBlacklistFilter implements GeoServerFilter {

    static final Logger LOGGER = Logging.getLogger(IpBlacklistFilter.class);
    static final String PROPERTYFILENAME = "controlflow.properties";
    static final String BLPROPERTY = "ip.blacklist";
    static final String WLPROPERTY = "ip.whitelist";
    private Set<String> blackListedAddresses;
    private Set<String> whiteListedAddresses;

    private final PropertyFileWatcher configFile;

    /**
     * Constructor used for testing purposes
     *
     * @param props configuraiton properties
     */
    public IpBlacklistFilter(Properties props) {
        this.blackListedAddresses = loadConfiguration(props, BLPROPERTY);
        this.whiteListedAddresses = loadConfiguration(props, WLPROPERTY);
        configFile = null;
    }

    /** Default constructor */
    public IpBlacklistFilter() {
        try {
            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            Resource resource = loader.get(PROPERTYFILENAME);
            configFile = new PropertyFileWatcher(resource);
            blackListedAddresses = reloadConfiguration(BLPROPERTY);
            whiteListedAddresses = reloadConfiguration(WLPROPERTY);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /** Filters ip black list */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (isBlackListed(httpRequest)) {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "This IP has been blocked. Please contact the server administrator");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isBlackListed(HttpServletRequest httpRequest) throws IOException {
        if (configFile != null && configFile.isStale()) {
            synchronized (configFile) {
                if (configFile.isStale()) {
                    this.blackListedAddresses = reloadConfiguration(BLPROPERTY);
                    this.whiteListedAddresses = reloadConfiguration(WLPROPERTY);
                }
            }
        }
        if (blackListedAddresses.isEmpty()) {
            return false;
        }
        String incomingIp = IpFlowController.getRemoteAddr(httpRequest);
        boolean blocked = false;
        // Check IP on blackList roles (to block)
        for (String blackListRole : blackListedAddresses) {
            if (incomingIp.matches(blackListRole)) {
                blocked = true;
                break;
            }
        }

        // Check IP (if blocked) on whiteList roles (to unlock)
        if (blocked && !whiteListedAddresses.isEmpty()) {
            for (String whiteListRole : whiteListedAddresses) {
                if (incomingIp.matches(whiteListRole)) {
                    blocked = false;
                    break;
                }
            }
        }
        return blocked;
    }

    private Set<String> reloadConfiguration(String property) throws IOException {
        Properties props = configFile.getProperties();
        if (props == null) {
            // file doesn't exist
            return Collections.emptySet();
        }
        return loadConfiguration(props, property);
    }

    private Set<String> loadConfiguration(Properties props, String property) {
        String rawList = props.getProperty(property);
        if (null == rawList) {
            return Collections.emptySet();
        }
        Set<String> ipAddresses = new HashSet<>();
        for (String ip : rawList.split(",")) {
            ipAddresses.add(ip.trim().replaceAll("\\*", "(.{0,1}[0-9]+.{0,1}){0,4}"));
        }
        return ipAddresses;
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        // TODO Auto-generated method stub
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }
}
