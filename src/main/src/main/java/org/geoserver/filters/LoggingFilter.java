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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.geoserver.ows.util.RequestUtils;

/**
 * Filter to log requests for debugging or statistics-gathering purposes.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class LoggingFilter implements Filter {
    protected Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.filters");

    protected boolean enabled = true;
    protected boolean logBodies = true;
    protected boolean logHeaders = true;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String message = "";
        String body = null;
        String path = "";

        if (enabled) {
            if (req instanceof HttpServletRequest) {
                HttpServletRequest hreq = (HttpServletRequest) req;

                path =
                        RequestUtils.getRemoteAddr(hreq)
                                + " \""
                                + hreq.getMethod()
                                + " "
                                + hreq.getRequestURI();
                if (hreq.getQueryString() != null) {
                    path += "?" + hreq.getQueryString();
                }
                path += "\"";

                message = "" + path;
                message += " \"" + noNull(hreq.getHeader("User-Agent"));
                message += "\" \"" + noNull(hreq.getHeader("Referer"));
                message += "\" \"" + noNull(hreq.getHeader("Content-type")) + "\" ";

                if (logHeaders) {
                    Enumeration<String> headerNames = hreq.getHeaderNames();
                    message += "\n  Headers:";
                    while (headerNames.hasMoreElements()) {
                        String headerName = headerNames.nextElement();
                        message += "\n    " + headerName + ": " + hreq.getHeader(headerName);
                    }
                }

                if (logBodies
                        && (hreq.getMethod().equals("PUT") || hreq.getMethod().equals("POST"))) {
                    message += " request-size: " + hreq.getContentLength();
                    message += " body: ";

                    String encoding = hreq.getCharacterEncoding();
                    if (encoding == null) {
                        // the default encoding for HTTP 1.1
                        encoding = "ISO-8859-1";
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] bytes;
                    try (InputStream is = hreq.getInputStream()) {
                        IOUtils.copy(is, bos);
                        bytes = bos.toByteArray();

                        body = new String(bytes, encoding);
                    }

                    req = new BufferedRequestWrapper(hreq, encoding, bytes);
                }
            } else {
                message = "" + req.getRemoteHost() + " made a non-HTTP request";
            }

            logger.info(message + (body == null ? "" : "\n" + body + "\n"));
            long startTime = System.currentTimeMillis();
            chain.doFilter(req, res);
            long requestTime = System.currentTimeMillis() - startTime;
            logger.info(path + " took " + requestTime + "ms");
        } else {
            chain.doFilter(req, res);
        }
    }

    public void init(FilterConfig filterConfig) {
        enabled = getConfigBool("enabled", filterConfig);
        logBodies = getConfigBool("log-request-bodies", filterConfig);
        logHeaders = getConfigBool("log-request-headers", filterConfig);
    }

    protected boolean getConfigBool(String name, FilterConfig conf) {
        try {
            String value = conf.getInitParameter(name);
            return Boolean.valueOf(value).booleanValue();
        } catch (Exception e) {
            return false;
        }
    }

    protected String noNull(String s) {
        if (s == null) return "";
        return s;
    }

    public void destroy() {}

    /** @return the enabled */
    public boolean isEnabled() {
        return enabled;
    }

    /** @param enabled the enabled to set */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return the logBodies */
    public boolean isLogBodies() {
        return logBodies;
    }

    /** @param logBodies the logBodies to set */
    public void setLogBodies(boolean logBodies) {
        this.logBodies = logBodies;
    }
}
