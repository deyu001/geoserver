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

package org.geoserver.params.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

public final class Filter implements GeoServerFilter, ExtensionPriority {

    private static final Logger LOGGER = Logging.getLogger(Filter.class);

    // this becomes true if the filter is initialized from the web container
    // via web.xml, so that we know we should ignore the spring initialized filter
    // (there are always two, this static variable is how they know about each other state)
    static boolean USE_AS_SERVLET_FILTER = false;

    // marks the instance initialized via web.xml (if any) so that we can avoid
    // duplicate filter application by the spring instance
    private boolean servletInstance = false;

    private List<Rule> rules = new ArrayList<>();

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public Filter() {
        // this is called if we initialize the filter in web.xml, so let's turn on the
        // flags for this scenario
        USE_AS_SERVLET_FILTER = true;
        servletInstance = true;
        logFilterInitiation();
    }

    public Filter(GeoServerDataDirectory dataDirectory) {
        servletInstance = false;
        initRules(dataDirectory);
        logFilterInitiation();
    }

    /** Helper method to log parameters extractor filter initiation. * */
    private void logFilterInitiation() {
        Utils.info(
                LOGGER,
                "Parameters extractor filter initiated [USE_AS_SERVLET_FILTER=%s, SERVLET_INSTANCE=%s].",
                USE_AS_SERVLET_FILTER,
                servletInstance);
    }

    private void initRules(GeoServerDataDirectory dataDirectory) {
        if (dataDirectory != null) {
            Utils.info(LOGGER, "Initiating parameters extractor rules.");
            Resource resource = dataDirectory.get(RulesDao.getRulesPath());
            rules = RulesDao.getRules(() -> resource.in());
            resource.addListener(notify -> rules = RulesDao.getRules(() -> resource.in()));
        } else {
            // no rules were loaded
            Utils.info(
                    LOGGER,
                    "No data directory provided, no parameters extractor rules were loaded.");
        }
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    /**
     * This method is called only when the Filter is used as a standard web container Filter. When
     * this happens the related instance will be used instead of the one initialized by Spring.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        GeoServerDataDirectory dataDirectory =
                GeoServerExtensions.bean(GeoServerDataDirectory.class);
        Utils.info(LOGGER, "Initiating parameters extractor as a standard web container filter.");
        initRules(dataDirectory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (isEnabled()) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            if (!httpServletRequest.getRequestURI().contains("web/wicket")
                    && !httpServletRequest.getRequestURI().contains("geoserver/web")) {
                UrlTransform urlTransform =
                        new UrlTransform(
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getParameterMap());
                String originalRequest = urlTransform.toString();
                rules.forEach(rule -> rule.apply(urlTransform));
                Utils.debug(
                        LOGGER,
                        "About to evaluate request '%s' with parameters extractor (%d) rules.",
                        originalRequest,
                        rules.size());
                if (urlTransform.haveChanged()) {
                    Utils.info(
                            LOGGER,
                            "Request '%s' transformed to '%s'.",
                            originalRequest,
                            urlTransform.toString());
                    request = new RequestWrapper(urlTransform, httpServletRequest);
                } else {
                    // no parameters extractor rules matched the url
                    Utils.debug(
                            LOGGER,
                            "No parameters extractor rules matched with the request '%s'.",
                            originalRequest);
                }
            } else {
                // parameters extractor ignored the request
                Utils.debug(
                        LOGGER,
                        "Request '%s' ignored by parameters extractor.",
                        httpServletRequest.getRequestURI());
            }
        } else {
            // parameters extractor is disabled
            Utils.debug(LOGGER, "Parameters extractor is disabled.");
        }
        chain.doFilter(request, response);
    }

    boolean isEnabled() {
        return !USE_AS_SERVLET_FILTER || servletInstance;
    }

    @Override
    public void destroy() {}
}
