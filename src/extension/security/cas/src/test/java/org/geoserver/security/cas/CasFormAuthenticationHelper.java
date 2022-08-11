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

import java.io.IOException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class for authentication against a Cas server
 *
 * <p>supported authentication mechanisms
 *
 * <p>- Cas Form login
 *
 * @author christian
 */
public class CasFormAuthenticationHelper extends CasAuthenticationHelper {

    public static final String CAS_4_0_USER = "casuser";
    public static final String CAS_4_0_PW = "Mellon";

    String username, password;

    public CasFormAuthenticationHelper(URL casUrlPrefix, String username, String password) {
        super(casUrlPrefix);
        this.username = username;
        this.password = password;
    }

    public boolean ssoLogin() throws IOException {
        URL loginUrl = createURLFromCasURI("/login");
        HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
        String responseString = readResponse(conn);
        String execution = extractFormParameter(responseString, "\"execution\"");
        if (execution == null)
            throw new IOException(" No hidden execution field for: " + loginUrl.toString());

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("username", username);
        paramMap.put("password", password);
        paramMap.put("_eventId", "submit");
        paramMap.put("execution", execution);
        paramMap.put("geolocation", "");

        conn = (HttpURLConnection) loginUrl.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // conn.setRequestProperty("Cookie", sessionCookieSend);

        writeParamsForPostAndSend(conn, paramMap);
        if (conn.getResponseCode() == 401) return false;

        List<HttpCookie> cookies = getCookies(conn);
        readResponse(conn);

        extractCASCookies(cookies, conn);

        return ticketGrantingCookie != null;
    }

    protected String extractFormParameter(String formLoginHtml, String searchString) {
        int index = formLoginHtml.indexOf(searchString);
        if (index == -1) return null;
        index += searchString.length();
        index = formLoginHtml.indexOf("\"", index);
        int index2 = formLoginHtml.indexOf("\"", index + 1);
        return formLoginHtml.substring(index + 1, index2);
    }
}
