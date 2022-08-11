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

package org.geoserver.opensearch.eo.response;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

/**
 * Based on the indications at
 * http://www.opensearch.org/Documentation/Developer_how_to_guide#How_to_indicate_errors encodes the
 * exception into a RSS document, e.g.
 *
 * <pre>{@code
 * <rss version="2.0" xmlns:openSearch="http://a9.com/-/spec/opensearch/1.1/">
 * <channel>
 * <title>title</title>
 * <link>link</link>
 * <description>description</description>
 * <openSearch:totalResults>1</openSearch:totalResults>
 * <openSearch:startIndex>1</openSearch:startIndex>
 * <openSearch:itemsPerPage>1</openSearch:itemsPerPage>
 * <item>
 * <title>Error</title>
 * <description>error message</description>
 * </item>
 * </channel>
 * }</pre>
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RSSExceptionTransformer extends LambdaTransformerBase {

    Request request;

    GeoServerInfo geoServer;

    public RSSExceptionTransformer(GeoServerInfo geoServer, Request request) {
        this.request = request;
        this.geoServer = geoServer;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ExceptionTranslator(handler);
    }

    public static String getDescription(GeoServerInfo geoServer, ServiceException e) {
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(e, sb, true);

        if (geoServer.getSettings().isVerboseExceptions()) {
            ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(stackTrace));

            sb.append("\nDetails:\n");
            sb.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
        }

        return sb.toString();
    }

    class ExceptionTranslator extends LambdaTranslatorSupport {

        public ExceptionTranslator(ContentHandler contentHandler) {
            super(contentHandler);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            ServiceException e = (ServiceException) o;
            element(
                    "rss",
                    () -> channel(e), //
                    attributes("xmlns:opensearch", "http://a9.com/-/spec/opensearch/1.1/"));
        }

        private void channel(ServiceException e) {
            element(
                    "channel",
                    () -> {
                        element("title", "OpenSearch for EO Error report");
                        element("link", buildSelfUrl());
                        element("opensearch:totalResults", "1");
                        element("opensearch:startIndex", "1");
                        element("opensearch:itemsPerPage", "1");
                        element("item", () -> itemContents(e));
                    });
        }

        private void itemContents(ServiceException e) {
            element("title", e.getMessage());
            element("description", getDescription(geoServer, e));
        }

        private String buildSelfUrl() {
            String baseURL = baseURL(request.getHttpRequest());
            return buildURL(baseURL, "oseo/description", null, URLType.SERVICE);
        }
    }
}
