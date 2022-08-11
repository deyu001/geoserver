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

package org.geoserver.ows.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;

/**
 * Utility class performing operations related to http requests.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *     <p>TODO: this class needs to be merged with org.vfny.geoserver.Requests.
 */
public class RequestUtils {

    /**
     * Pulls out the first IP address from the X-Forwarded-For request header if it was provided;
     * otherwise just gets the client IP address.
     *
     * @return the IP address of the client that sent the request
     */
    public static String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            String[] ips = forwardedFor.split(", ");
            return ips[0];
        } else {
            return req.getRemoteAddr();
        }
    }

    /**
     * Given a list of provided versions, and a list of accepted versions, this method will return
     * the negotiated version to be used for response according to the pre OWS 1.1 specifications,
     * that is, WMS 1.1, WMS 1.3, WFS 1.0, WFS 1.1 and WCS 1.0
     *
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" format)
     * @return the negotiated version to be used for response
     */
    public static String getVersionPreOws(List<String> providedList, List<String> acceptedList) {
        // first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }

        // if no accept list provided, we return the biggest
        if (acceptedList == null || acceptedList.isEmpty()) return provided.last().toString();

        // next figure out what the client accepts (and check they are good version numbers)
        TreeSet<Version> accepted = new TreeSet<>();
        for (String v : acceptedList) {
            checkVersionNumber(v, null);

            accepted.add(new Version(v));
        }

        // prune out those not provided
        for (Iterator<Version> v = accepted.iterator(); v.hasNext(); ) {
            Version version = v.next();

            if (!provided.contains(version)) {
                v.remove();
            }
        }

        // lookup a matching version
        String version = null;
        if (!accepted.isEmpty()) {
            // return the highest version provided
            version = accepted.last().toString();
        } else {
            for (String v : acceptedList) {
                accepted.add(new Version(v));
            }

            // if highest accepted less then lowest provided, send lowest
            if ((accepted.last()).compareTo(provided.first()) < 0) {
                version = (provided.first()).toString();
            }

            // if lowest accepted is greater then highest provided, send highest
            if ((accepted.first()).compareTo(provided.last()) > 0) {
                version = (provided.last()).toString();
            }

            if (version == null) {
                // go through from lowest to highest, and return highest provided
                // that is less than the highest accepted
                Iterator<Version> v = provided.iterator();
                Version last = v.next();

                while (v.hasNext()) {
                    Version current = v.next();

                    if (current.compareTo(accepted.last()) > 0) {
                        break;
                    }

                    last = current;
                }

                version = last.toString();
            }
        }

        return version;
    }

    /**
     * Given a list of provided versions, and a list of accepted versions, this method will return
     * the negotiated version to be used for response according to the OWS 1.1 specification (at the
     * time of writing, only WCS 1.1.1 is using it)
     *
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" format)
     * @return the negotiated version to be used for response
     */
    public static String getVersionOws11(List<String> providedList, List<String> acceptedList) {
        // first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }

        // if no accept list provided, we return the biggest supported version
        if (acceptedList == null || acceptedList.isEmpty()) return provided.last().toString();

        // next figure out what the client accepts (and check they are good version numbers)
        List<Version> accepted = new ArrayList<>();
        for (String v : acceptedList) {
            checkVersionNumber(v, "AcceptVersions");

            accepted.add(new Version(v));
        }

        // from the specification "The server, upon receiving a GetCapabilities request, shall scan
        // through this list and find the first version number that it supports"
        Version negotiated = null;
        for (Version version : accepted) {
            if (provided.contains(version)) {
                negotiated = version;
                break;
            }
        }

        // from the spec: "If the list does not contain any version numbers that the server
        // supports, the server shall return an Exception with
        // exceptionCode="VersionNegotiationFailed"
        if (negotiated == null)
            throw new ServiceException(
                    "Could not find any matching version "
                            + acceptedList
                            + " in supported list: "
                            + acceptedList,
                    "VersionNegotiationFailed");

        return negotiated.toString();
    }

    /**
     * Checks the validity of a version number (the specification version numbers, three dot
     * separated integers between 0 and 99). Throws a ServiceException if the version number is not
     * valid.
     *
     * @param v the version number (in string format)
     * @param locator The locator for the service exception (may be null)
     */
    public static void checkVersionNumber(String v, String locator) throws ServiceException {
        if (!v.matches("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{1,2}")) {
            String msg = v + " is an invalid version number";
            throw new ServiceException(msg, "VersionNegotiationFailed", locator);
        }
    }

    /**
     * Wraps an xml input xstream in a buffered reader specifying a lookahead that can be used to
     * preparse some of the xml document, resetting it back to its original state for actual
     * parsing.
     *
     * @param stream The original xml stream.
     * @param xmlLookahead The number of bytes to support for parse. If more than this number of
     *     bytes are preparsed the stream can not be properly reset.
     * @return The buffered reader.
     */
    public static BufferedReader getBufferedXMLReader(InputStream stream, int xmlLookahead)
            throws IOException {

        // create a buffer so we can reset the input stream
        BufferedInputStream input = new BufferedInputStream(stream);
        input.mark(xmlLookahead);

        // create object to hold encoding info
        EncodingInfo encoding = new EncodingInfo();

        // call this method to set the encoding info
        XmlCharsetDetector.getCharsetAwareReader(input, encoding);

        // call this method to create the reader
        @SuppressWarnings("PMD.CloseResource") // just a wrapper
        Reader reader = XmlCharsetDetector.createReader(input, encoding);

        // rest the input
        input.reset();

        return getBufferedXMLReader(reader, xmlLookahead);
    }

    /**
     * Wraps an xml reader in a buffered reader specifying a lookahead that can be used to preparse
     * some of the xml document, resetting it back to its original state for actual parsing.
     *
     * @param reader The original xml reader.
     * @param xmlLookahead The number of bytes to support for parse. If more than this number of
     *     bytes are preparsed the stream can not be properly reset.
     * @return The buffered reader.
     */
    public static BufferedReader getBufferedXMLReader(Reader reader, int xmlLookahead)
            throws IOException {
        // ensure the reader is a buffered reader

        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader);
        }

        // mark the input stream
        reader.mark(xmlLookahead);

        return (BufferedReader) reader;
    }
}
