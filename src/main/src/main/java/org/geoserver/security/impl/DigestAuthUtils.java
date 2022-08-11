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

package org.geoserver.security.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This is an exact copy of org.springframework.security.web.authentication.www.DigestAuthUtils;
 *
 * <p>The Spring class has package visibility, no idea why. The functionally is used for test cases
 * and may be used by a client agent using the geoserver library
 *
 * @author mcr
 */
public class DigestAuthUtils {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static String encodePasswordInA1Format(String username, String realm, String password) {
        String a1 = username + ":" + realm + ":" + password;
        String a1Md5 = md5Hex(a1);

        return a1Md5;
    }

    public static String[] splitIgnoringQuotes(String str, char separatorChar) {
        if (str == null) {
            return null;
        }

        int len = str.length();

        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }

        List<String> list = new ArrayList<>();
        int i = 0;
        int start = 0;
        boolean match = false;

        while (i < len) {
            if (str.charAt(i) == '"') {
                i++;
                while (i < len) {
                    if (str.charAt(i) == '"') {
                        i++;
                        break;
                    }
                    i++;
                }
                match = true;
                continue;
            }
            if (str.charAt(i) == separatorChar) {
                if (match) {
                    list.add(str.substring(start, i));
                    match = false;
                }
                start = ++i;
                continue;
            }
            match = true;
            i++;
        }
        if (match) {
            list.add(str.substring(start, i));
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Computes the <code>response</code> portion of a Digest authentication header. Both the server
     * and user agent should compute the <code>response</code> independently. Provided as a static
     * method to simplify the coding of user agents.
     *
     * @param passwordAlreadyEncoded true if the password argument is already encoded in the correct
     *     format. False if it is plain text.
     * @param username the user's login name.
     * @param realm the name of the realm.
     * @param password the user's password in plaintext or ready-encoded.
     * @param httpMethod the HTTP request method (GET, POST etc.)
     * @param uri the request URI.
     * @param qop the qop directive, or null if not set.
     * @param nonce the nonce supplied by the server
     * @param nc the "nonce-count" as defined in RFC 2617.
     * @param cnonce opaque string supplied by the client when qop is set.
     * @return the MD5 of the digest authentication response, encoded in hex
     * @throws IllegalArgumentException if the supplied qop value is unsupported.
     */
    public static String generateDigest(
            boolean passwordAlreadyEncoded,
            String username,
            String realm,
            String password,
            String httpMethod,
            String uri,
            String qop,
            String nonce,
            String nc,
            String cnonce)
            throws IllegalArgumentException {
        String a1Md5 = null;
        String a2 = httpMethod + ":" + uri;
        String a2Md5 = md5Hex(a2);

        if (passwordAlreadyEncoded) {
            a1Md5 = password;
        } else {
            a1Md5 = DigestAuthUtils.encodePasswordInA1Format(username, realm, password);
        }

        String digest;

        if (qop == null) {
            // as per RFC 2069 compliant clients (also reaffirmed by RFC 2617)
            digest = a1Md5 + ":" + nonce + ":" + a2Md5;
        } else if ("auth".equals(qop)) {
            // As per RFC 2617 compliant clients
            digest = a1Md5 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + a2Md5;
        } else {
            throw new IllegalArgumentException("This method does not support a qop: '" + qop + "'");
        }

        String digestMd5 = new String(md5Hex(digest));

        return digestMd5;
    }

    /**
     * Takes an array of <code>String</code>s, and for each element removes any instances of <code>
     * removeCharacter</code>, and splits the element based on the <code>delimiter</code>. A <code>
     * Map</code> is then generated, with the left of the delimiter providing the key, and the right
     * of the delimiter providing the value.
     *
     * <p>Will trim both the key and value before adding to the <code>Map</code>.
     *
     * @param array the array to process
     * @param delimiter to split each element using (typically the equals symbol)
     * @param removeCharacters one or more characters to remove from each element prior to
     *     attempting the split operation (typically the quotation mark symbol) or <code>null</code>
     *     if no removal should occur
     * @return a <code>Map</code> representing the array contents, or <code>null</code> if the array
     *     to process was null or empty
     */
    public static Map<String, String> splitEachArrayElementAndCreateMap(
            String[] array, String delimiter, String removeCharacters) {
        if ((array == null) || (array.length == 0)) {
            return null;
        }

        Map<String, String> map = new HashMap<>();

        for (String s : array) {
            String postRemove;

            if (removeCharacters == null) {
                postRemove = s;
            } else {
                postRemove = StringUtils.replace(s, removeCharacters, "");
            }

            String[] splitThisArrayElement = split(postRemove, delimiter);

            if (splitThisArrayElement == null) {
                continue;
            }

            map.put(splitThisArrayElement[0].trim(), splitThisArrayElement[1].trim());
        }

        return map;
    }

    /**
     * Splits a <code>String</code> at the first instance of the delimiter.
     *
     * <p>Does not include the delimiter in the response.
     *
     * @param toSplit the string to split
     * @param delimiter to split the string up with
     * @return a two element array with index 0 being before the delimiter, and index 1 being after
     *     the delimiter (neither element includes the delimiter)
     * @throws IllegalArgumentException if an argument was invalid
     */
    public static String[] split(String toSplit, String delimiter) {
        Assert.hasLength(toSplit, "Cannot split a null or empty string");
        Assert.hasLength(delimiter, "Cannot use a null or empty delimiter to split a string");

        if (delimiter.length() != 1) {
            throw new IllegalArgumentException("Delimiter can only be one character in length");
        }

        int offset = toSplit.indexOf(delimiter);

        if (offset < 0) {
            return null;
        }

        String beforeDelimiter = toSplit.substring(0, offset);
        String afterDelimiter = toSplit.substring(offset + 1);

        return new String[] {beforeDelimiter, afterDelimiter};
    }

    public static String md5Hex(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        }

        return new String(Hex.encode(digest.digest(data.getBytes())));
    }
}
