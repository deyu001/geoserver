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

package org.geoserver.platform;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A servlet filter that allows for advanced dispatching.
 *
 * <p>This fiter allows for a single mapping from web.xml for all requests to the spring dispatcher.
 * It creates a wrapper around the servlet request object that "fakes" the serveltPath property to
 * make it look like the mapping was created in web.xml when in actuality it was created in spring.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AdvancedDispatchFilter implements Filter {

    public void destroy() {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            request = new AdvancedDispatchHttpRequest((HttpServletRequest) request);
        }
        chain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {}

    static class AdvancedDispatchHttpRequest extends HttpServletRequestWrapper {

        String servletPath = null;

        public AdvancedDispatchHttpRequest(HttpServletRequest delegate) {
            super(delegate);

            if (delegate.getClass().getSimpleName().endsWith("MockHttpServletRequest")) {
                return;
            }

            String path = delegate.getPathInfo();

            if (path == null) {
                return;
            }

            int slash = path.indexOf('/', 1);
            if (slash > -1) {
                this.servletPath = path.substring(0, slash);
            } else {
                this.servletPath = path;
            }

            int question = this.servletPath.indexOf('?');
            if (question > -1) {
                this.servletPath = this.servletPath.substring(0, question);
            }
        }

        public String getPathInfo() {
            HttpServletRequest delegate = (HttpServletRequest) getRequest();
            if (servletPath != null
                    && delegate.getPathInfo() != null
                    && delegate.getPathInfo().startsWith(servletPath))
                return delegate.getPathInfo().substring(servletPath.length());
            else return delegate.getPathInfo();
        }

        public String getServletPath() {
            return servletPath != null
                    ? servletPath
                    : ((HttpServletRequest) getRequest()).getServletPath();
        }
    }
}
