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


package org.geoserver.security.cas;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An abstract helper class for authentication against a Cas server
 *
 * @author christian
 */
public abstract class CasAuthenticationHelper {

    protected URL casUrlPrefix;
    /** true for an SSL (TLS) connection */
    protected boolean secure;

    protected HttpCookie ticketGrantingCookie, warningCookie;

    /** casUrlPrefix is the CAS Server URL including context root */
    public CasAuthenticationHelper(URL casUrlPrefix) {
        secure = "HTTPS".equalsIgnoreCase(casUrlPrefix.getProtocol());
        this.casUrlPrefix = casUrlPrefix;
    }

    /** create URL from a CAS protocol URI */
    protected URL createURLFromCasURI(String casUri) {
        URL retValue = null;
        try {
            retValue =
                    new URL(
                            casUrlPrefix.getProtocol(),
                            casUrlPrefix.getHost(),
                            casUrlPrefix.getPort(),
                            casUrlPrefix.getPath() + casUri);
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "Cannot build url from " + casUrlPrefix.toExternalForm() + " and " + casUri);
        }
        return retValue;
    }

    protected String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line = "";
            StringBuffer buff = new StringBuffer();
            while ((line = in.readLine()) != null) {
                buff.append(line);
            }
            return buff.toString();
        }
    }

    protected List<String> getResponseHeaderValues(HttpURLConnection conn, String hName) {
        List<String> result = new ArrayList<>();
        for (int i = 0; ; i++) {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);

            if (headerName == null && headerValue == null) {
                // No more headers
                break;
            }
            if (hName.equalsIgnoreCase(headerName)) {
                result.add(headerValue);
            }
        }
        return result;
    }

    protected List<HttpCookie> getCookies(HttpURLConnection conn) {
        List<HttpCookie> result = new ArrayList<>();
        List<String> cookieStrings = getResponseHeaderValues(conn, "Set-Cookie");
        for (String cookieString : cookieStrings) {
            result.addAll(HttpCookie.parse("Set-Cookie: " + cookieString));
        }
        cookieStrings = getResponseHeaderValues(conn, "Set-Cookie2");
        for (String cookieString : cookieStrings) {
            result.addAll(HttpCookie.parse("Set-Cookie2: " + cookieString));
        }
        return result;
    }

    protected HttpCookie getCookieNamed(List<HttpCookie> cookies, String cookieName) {
        for (HttpCookie c : cookies) {
            if (c.getName().equalsIgnoreCase(cookieName)) return c;
        }
        return null;
    }

    protected void writeParamsForPostAndSend(HttpURLConnection conn, Map<String, String> paramMap)
            throws IOException {
        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            StringBuffer buff = new StringBuffer();
            for (Entry<String, String> entry : paramMap.entrySet()) {
                if (buff.length() > 0) buff.append("&");
                buff.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "utf-8"));
            }

            out.writeBytes(buff.toString());
            out.flush();
        }
    }

    public HttpCookie getTicketGrantingCookie() {
        return ticketGrantingCookie;
    }

    public HttpCookie getWarningCookie() {
        return warningCookie;
    }

    /** Single logout from Cas server */
    public boolean ssoLogout() throws IOException {
        if (!secure) return true;
        if (ticketGrantingCookie == null) return true;

        URL logoutUrl = createURLFromCasURI(GeoServerCasConstants.LOGOUT_URI);
        HttpURLConnection conn = (HttpURLConnection) logoutUrl.openConnection();
        addCasCookies(conn);
        conn.getInputStream().close();
        extractCASCookies(getCookies(conn), conn);
        return getTicketGrantingCookie() != null && "".equals(getTicketGrantingCookie().getValue());
    }

    /** add Cas cookies to request */
    protected void addCasCookies(HttpURLConnection conn) {
        String cookieString = "";
        if (checkCookieForSend(warningCookie)) cookieString = warningCookie.toString();
        if (checkCookieForSend(ticketGrantingCookie)) {
            if (cookieString.length() > 0) cookieString += ",";
            cookieString += ticketGrantingCookie.toString();
        }
        if (cookieString.length() > 0) conn.setRequestProperty("Cookie", cookieString);
    }

    public boolean isSecure() {
        return secure;
    }

    protected boolean checkCookieForSend(HttpCookie cookie) {
        if (cookie == null) return false;
        if (cookie.hasExpired()) return false;
        if (isSecure() == false && cookie.getSecure()) {
            return false;
        }
        return true;
    }

    /**
     * The concrete login, after sucessful login, the cookies should be set using {@link
     * #extractCASCookies(List, HttpURLConnection)}
     */
    public abstract boolean ssoLogin() throws IOException;

    /**
     * Get a service ticket for the service
     *
     * <p>Precondition: successful log in wiht {@link #ssoLogin()} {@link #isSecure()} == true
     */
    public String getServiceTicket(URL service) throws IOException {

        if (getTicketGrantingCookie() == null || getTicketGrantingCookie().getValue().isEmpty()) {
            throw new IOException("not a valid TGC ");
        }

        URL loginUrl =
                createURLFromCasURI(
                        GeoServerCasConstants.LOGIN_URI + "?service=" + service.toExternalForm());
        HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
        conn.setInstanceFollowRedirects(false);
        addCasCookies(conn);
        conn.getInputStream().close();
        List<String> values = getResponseHeaderValues(conn, "Location");
        if (values.isEmpty()) {
            throw new IOException("No redirect received for " + loginUrl);
        }
        String redirectURL = values.get(0);
        String ticket = null;
        URL rURL = new URL(redirectURL);
        for (String kvp : rURL.getQuery().split("&")) {
            String[] tmp = kvp.split("=");
            if ("ticket".equalsIgnoreCase((tmp[0]).trim())) {
                ticket = tmp[1].trim();
                break;
            }
        }
        return ticket;
    }

    /** extract Cas cookies from all received cookies */
    public void extractCASCookies(List<HttpCookie> cookies, HttpURLConnection conn) {
        warningCookie = getCookieNamed(cookies, "CASPRIVACY");
        ticketGrantingCookie = getCookieNamed(cookies, "TGC");
    }
}
