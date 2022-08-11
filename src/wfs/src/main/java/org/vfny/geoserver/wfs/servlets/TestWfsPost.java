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

package org.vfny.geoserver.wfs.servlets;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * Simple tester for WFS post requests. Can be called two ways. If called with no parameters, it
 * displays the form, otherwise it displays the result page.
 *
 * @author Doug Cates: Moxi Media Inc.
 * @version 1.0
 */
public class TestWfsPost extends HttpServlet {

    /**
     * The path at which TestWfsPost is exposed. Used to find the full location of GeoServer without
     * doing complex and error prone string building
     */
    static final String TEST_WFS_POST_PATH = "/TestWfsPost";

    static final Logger LOGGER = Logging.getLogger(TestWfsPost.class);

    /** Initializes the servlet. */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /** Destroys the servlet. */
    public void destroy() {}

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet. */
    public String getServletInfo() {
        return "Tests a WFS post request using a form entry.";
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestString = request.getParameter("body");
        String urlString = request.getParameter("url");
        boolean doGet = (requestString == null) || requestString.trim().equals("");

        if ((urlString == null)) {
            @SuppressWarnings("PMD.CloseResource") // managed by the servlet
            PrintWriter out = response.getWriter();
            StringBuffer urlInfo = request.getRequestURL();

            if (urlInfo.indexOf("?") != -1) {
                urlInfo.delete(urlInfo.indexOf("?"), urlInfo.length());
            }

            String geoserverUrl =
                    urlInfo.substring(0, urlInfo.indexOf("/", 8)) + request.getContextPath();
            response.setContentType("text/html");
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>TestWfsPost</title>");
            out.println("</head>");
            out.println("<script language=\"JavaScript\">");
            out.println("function doNothing() {");
            out.println("}");
            out.println("function sendRequest() {");
            out.println("  if (checkURL()==true) {");
            out.print("    document.frm.action = \"");
            out.print(urlInfo.toString());
            out.print("\";\n");
            out.println("    document.frm.target = \"_blank\";");
            out.println("    document.frm.submit();");
            out.println("  }");
            out.println("}");
            out.println("function checkURL() {");
            out.println("  if (document.frm.url.value==\"\") {");
            out.println("    alert(\"Please give URL before you sumbit this form!\");");
            out.println("    return false;");
            out.println("  } else {");
            out.println("    return true;");
            out.println("  }");
            out.println("}");
            out.println("function clearRequest() {");
            out.println("document.frm.body.value = \"\";");
            out.println("}");
            out.println("</script>");
            out.println("<body>");
            out.println("<form name=\"frm\" action=\"JavaScript:doNothing()\" method=\"POST\">");
            out.println(
                    "<table align=\"center\" cellspacing=\"2\" cellpadding=\"2\" border=\"0\">");
            out.println("<tr>");
            out.println("<td><b>URL:</b></td>");
            out.print("<td><input name=\"url\" value=\"");
            out.print(geoserverUrl);
            out.print("/wfs/GetFeature\" size=\"70\" MAXLENGTH=\"100\"/></td>\n");
            out.println("</tr>");
            out.println("<tr>");
            out.println("<td><b>Request:</b></td>");
            out.println("<td><textarea cols=\"60\" rows=\"24\" name=\"body\"></textarea></td>");
            out.println("</tr>");
            out.println("</table>");
            out.println("<table align=\"center\">");
            out.println("<tr>");
            out.println(
                    "<td><input type=\"button\" value=\"Clear\" onclick=\"clearRequest()\"></td>");
            out.println(
                    "<td><input type=\"button\" value=\"Submit\" onclick=\"sendRequest()\"></td>");
            out.println("<td></td>");
            out.println("</tr>");
            out.println("</table>");
            out.println("</form>");
            out.println("</body>");
            out.println("</html>");
            out.close();
        } else {
            response.setContentType("application/xml");

            BufferedReader xmlIn = null;
            PrintWriter xmlOut = null;
            try {
                URL u = new URL(urlString);
                validateURL(request, urlString, getProxyBaseURL());
                java.net.HttpURLConnection acon = (java.net.HttpURLConnection) u.openConnection();
                acon.setAllowUserInteraction(false);

                if (!doGet) {
                    // System.out.println("set to post");
                    acon.setRequestMethod("POST");
                    acon.setRequestProperty("Content-Type", "application/xml");
                } else {
                    // System.out.println("set to get");
                    acon.setRequestMethod("GET");
                }

                acon.setDoOutput(true);
                acon.setDoInput(true);
                acon.setUseCaches(false);

                String username = request.getParameter("username");

                if ((username != null) && !username.trim().equals("")) {
                    String password = request.getParameter("password");
                    String up = username + ":" + password;
                    byte[] encoded = Base64.encodeBase64(up.getBytes());
                    String authHeader = "Basic " + new String(encoded);
                    acon.setRequestProperty("Authorization", authHeader);
                }

                if (!doGet) {
                    xmlOut =
                            new PrintWriter(
                                    new BufferedWriter(
                                            new OutputStreamWriter(acon.getOutputStream())));
                    xmlOut = new java.io.PrintWriter(acon.getOutputStream());

                    xmlOut.write(requestString);
                    xmlOut.flush();
                }

                // Above 400 they're all error codes, see:
                // http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
                if (acon.getResponseCode() >= 400) {
                    // Construct the full response before writing, so that we don't throw an
                    // exception partway through.
                    StringBuilder responseContent = new StringBuilder();
                    responseContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    responseContent.append("<servlet-exception>\n");
                    responseContent.append("HTTP response: ");
                    responseContent.append(acon.getResponseCode());
                    responseContent.append("\n");
                    if (acon.getResponseMessage() != null) {
                        responseContent.append(
                                URLDecoder.decode(acon.getResponseMessage(), "UTF-8"));
                    }
                    responseContent.append("</servlet-exception>\n");

                    @SuppressWarnings("PMD.CloseResource") // managed by the servlet
                    PrintWriter out = response.getWriter();
                    out.print(responseContent.toString());
                } else {
                    // xmlIn = new BufferedReader(new InputStreamReader(
                    // acon.getInputStream()));
                    // String line;

                    // System.out.println("got encoding from acon: "
                    // + acon.getContentType());
                    response.setContentType(acon.getContentType());
                    response.setHeader(
                            "Content-disposition", acon.getHeaderField("Content-disposition"));

                    @SuppressWarnings("PMD.CloseResource") // managed by the servlet
                    OutputStream output = response.getOutputStream();
                    int c;
                    try (InputStream in = acon.getInputStream()) {
                        while ((c = in.read()) != -1) output.write(c);
                    }
                }

                // while ((line = xmlIn.readLine()) != null) {
                //    out.print(line);
                // }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failure dealing with the request", e);
                @SuppressWarnings("PMD.CloseResource") // managed by the servlet
                PrintWriter out = response.getWriter();
                out.print(errorResponse(e));
            } finally {
                try {
                    if (xmlIn != null) {
                        xmlIn.close();
                    }
                } catch (Exception e1) {
                    LOGGER.log(Level.FINE, "Internal failure dealing with the request", e1);
                    @SuppressWarnings("PMD.CloseResource") // managed by the servlet
                    PrintWriter out = response.getWriter();
                    out.print(errorResponse(e1));
                }

                try {
                    if (xmlOut != null) {
                        xmlOut.close();
                    }
                } catch (Exception e2) {
                    LOGGER.log(Level.FINE, "Internal failure dealing with the request", e2);
                    @SuppressWarnings("PMD.CloseResource") // managed by the servlet
                    PrintWriter out = response.getWriter();
                    out.print(errorResponse(e2));
                }
            }
        }
    }

    public String errorResponse(Exception e) {
        StringBuilder responseContent = new StringBuilder();
        responseContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        responseContent.append("<servlet-exception>\n");
        responseContent.append(ResponseUtils.encodeXML(e.toString()));
        responseContent.append("</servlet-exception>\n");

        return responseContent.toString();
    }

    String getProxyBaseURL() {
        GeoServer geoServer = getGeoServer();
        if (geoServer != null) {
            return geoServer.getGlobal().getSettings().getProxyBaseUrl();
        }
        return null;
    }

    @VisibleForTesting
    protected GeoServer getGeoServer() {
        return (GeoServer) GeoServerExtensions.bean("geoServer");
    }

    /**
     * Validates the destination URL parameter sent to the TestWfsPost to execute, to verify it is
     * valid, and the request is actually coming from geoserver.
     *
     * <p>Two cases are checked, depending on whether or not GeoServer's Proxy Base URL is set.
     *
     * <p>If the Proxy Base URL is set, the host of the url parameter should match that of the Proxy
     * Base URL (since the HTTP request is coming from inside GeoServer, while the url parameter is
     * external)
     *
     * <p>Otherwise, the host of the url parameter should match that of the HTTP request.
     *
     * <p>In both cases, the path of the request url should be that of the TestWfsPost servlet
     * endpoint.
     *
     * @param request The HTTP request sent from Wicket to the TestWfsPost servlet
     * @param urlString The url that the TestWfsPost servlet is being asked to send an OWS request
     *     to.
     * @param proxyBase The proxy base URL of GeoServer
     * @throws IllegalArgumentException - If the arguments are malformed or otherwise invalid
     * @throws IllegalStateException - If something else is wrong
     */
    void validateURL(HttpServletRequest request, String urlString, String proxyBase) {
        URL url;
        URL requestUrl;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid url requested; not a URL: " + urlString, e);
        }
        String requestString = request.getRequestURL().toString();
        try {
            requestUrl = new URL(requestString);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid request url; not a URL: " + requestString, e);
        }

        // this should not happen, but let's not make it an open proxy if it does
        if (!request.getServletPath().equals(TEST_WFS_POST_PATH)) {
            throw new IllegalStateException(
                    "Unepected, the TestWfsPost was accessed by a path not ending with TestWfsPost: "
                            + requestString);
        }
        if (null != requestUrl.getQuery()) {
            throw new IllegalStateException(
                    "Unepected, the TestWfsPost was accessed by a path not ending with TestWfsPost: "
                            + requestString);
        }
        if (!request.getContextPath().equals(this.getServletContext().getContextPath())) {
            throw new IllegalStateException(
                    "Unepected, the TestWfsPost was accessed by a path from a different servlet context: "
                            + requestString);
        }

        if (proxyBase != null) {
            try {
                URL proxyBaseUrl = new URL(proxyBase);

                if (!url.getHost().equals(proxyBaseUrl.getHost())) {
                    throw new IllegalArgumentException(
                            "Invalid url requested, the demo requests should be hitting: "
                                    + proxyBase);
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(
                        "Invalid Proxy Base URL; not a URL: " + proxyBase, e);
            }
        } else {
            if (!url.getHost().equals(requestUrl.getHost())) {
                throw new IllegalStateException(
                        "Invalid url requested, the demo requests should be hitting: "
                                + requestString.substring(
                                        0, requestString.lastIndexOf(TEST_WFS_POST_PATH)));
            }
        }
    }
}
