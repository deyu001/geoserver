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

package org.geoserver.filters;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import org.geoserver.security.filter.GeoServerSecurityContextPersistenceFilter;
import org.geotools.util.logging.Logging;

/**
 * Utility filter that will dump a stack trace identifying any session creation outside of the user
 * interface (OGC and REST services are supposed to be stateless, session creation is harmful to
 * scalability)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SessionDebugFilter implements Filter {

    static final Logger LOGGER = Logging.getLogger(SessionDebugWrapper.class);

    public void destroy() {
        // nothing to do
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            chain.doFilter(new SessionDebugWrapper(request), res);
        } else {
            chain.doFilter(req, res);
        }
    }

    /**
     * {@link HttpServletRequest} wrapper that will dump a full trace for any session creation
     * attempt
     *
     * @author Andrea Aime - GeoSolutions
     */
    class SessionDebugWrapper extends HttpServletRequestWrapper {

        public SessionDebugWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession() {
            return this.getSession(true);
        }

        @Override
        public HttpSession getSession(boolean create) {
            // first off, try to grab an existing session
            HttpSession session = super.getSession(false);

            if (session != null || !create) {
                return session;
            }

            // ok, no session but the caller really wants one,
            // check for the hint passed by the GeoServerSecurityContextPersistenceFilter and
            // signal the issue in the logs

            Boolean allow =
                    (Boolean)
                            getAttribute(
                                    GeoServerSecurityContextPersistenceFilter
                                            .ALLOWSESSIONCREATION_ATTR);

            // are we creating the session in the web ui?
            if (getPathInfo().startsWith("/web") || Boolean.TRUE.equals(allow)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    Exception e = new Exception("Full stack trace for the session creation path");
                    e.fillInStackTrace();
                    LOGGER.log(
                            Level.FINE,
                            "Creating a new http session inside the web UI (normal behavior)",
                            e);
                }
            } else {
                if (LOGGER.isLoggable(Level.INFO)) {
                    Exception e = new Exception("Full stack trace for the session creation path");
                    e.fillInStackTrace();
                    LOGGER.log(
                            Level.INFO,
                            "Creating a new http session outside of the web UI! "
                                    + "(normally not desirable), the path is"
                                    + getPathInfo(),
                            e);
                }
            }

            // return the session
            session = super.getSession(true);
            return session;
        }
    }
}
