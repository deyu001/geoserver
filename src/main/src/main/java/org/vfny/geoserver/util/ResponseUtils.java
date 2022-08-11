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

package org.vfny.geoserver.util;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.util.logging.Logging;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public final class ResponseUtils {

    static Logger LOGGER = Logging.getLogger(ResponseUtils.class);

    /*
    Profixies a link url interpreting a localhost url as a back reference to the server.
    */
    private static String proxifyLink(String content, String baseURL) {
        try {
            URI uri = new URI(content);
            try {
                if (uri.getHost() == null) {
                    // interpret no host as backreference to server
                    Map<String, String> kvp = null;
                    if (uri.getQuery() != null && !"".equals(uri.getQuery())) {
                        Map<String, Object> parsed =
                                KvpUtils.parseQueryString("?" + uri.getQuery());
                        kvp = new HashMap<>();
                        for (Entry<String, Object> entry : parsed.entrySet()) {
                            kvp.put(entry.getKey(), (String) entry.getValue());
                        }
                    }

                    content = buildURL(baseURL, uri.getPath(), kvp, URLType.RESOURCE);
                }
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to create proper back reference for url: " + content,
                        e);
            }
        } catch (URISyntaxException e) {
        }
        return content;
    }
    /**
     * Profixies a metadata link url interpreting a localhost url as a back reference to the server.
     *
     * <p>If <tt>link</tt> is not a localhost url it is left untouched.
     */
    public static String proxifyMetadataLink(MetadataLinkInfo link, String baseURL) {
        String content = link.getContent();
        content = proxifyLink(content, baseURL);
        return content;
    }

    /**
     * Profixies a data link url interpreting a localhost url as a back reference to the server.
     *
     * <p>If <tt>link</tt> is not a localhost url it is left untouched.
     */
    public static String proxifyDataLink(DataLinkInfo link, String baseURL) {
        String content = link.getContent();
        content = proxifyLink(content, baseURL);
        return content;
    }

    public static List validate(
            InputSource xml, URL schemaURL, boolean skipTargetNamespaceException) {
        return validate(xml, schemaURL, skipTargetNamespaceException, null);
    }

    public static List<SAXException> validate(
            InputSource xml,
            URL schemaURL,
            boolean skipTargetNamespaceException,
            EntityResolver entityResolver) {
        StreamSource source = null;
        if (xml.getCharacterStream() != null) {
            source = new StreamSource(xml.getCharacterStream());
        } else if (xml.getByteStream() != null) {
            source = new StreamSource(xml.getByteStream());
        } else {
            throw new IllegalArgumentException("Could not turn input source to stream source");
        }
        return validate(source, schemaURL, skipTargetNamespaceException, entityResolver);
    }

    public static List<SAXException> validate(
            Source xml, URL schemaURL, boolean skipTargetNamespaceException) {
        return validate(xml, schemaURL, skipTargetNamespaceException, null);
    }

    public static List<SAXException> validate(
            Source xml,
            URL schemaURL,
            boolean skipTargetNamespaceException,
            EntityResolver entityResolver) {
        try {
            Schema schema =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                            .newSchema(schemaURL);
            Validator v = schema.newValidator();
            if (entityResolver != null) {
                v.setResourceResolver(
                        new EntityResolverToLSResourceResolver(
                                v.getResourceResolver(), entityResolver));
            }
            Handler handler = new Handler(skipTargetNamespaceException, entityResolver);
            v.setErrorHandler(handler);
            v.validate(xml);
            return handler.errors;
        } catch (SAXException | IOException e) {
            return exception(e);
        }
    }

    // errors in the document will be put in "errors".
    // if errors.size() ==0  then there were no errors.
    private static class Handler extends DefaultHandler {
        public List<SAXException> errors = new ArrayList<>();

        boolean skipTargetNamespaceException;

        EntityResolver entityResolver;

        Handler(boolean skipTargetNamespaceExeption, EntityResolver entityResolver) {
            this.skipTargetNamespaceException = skipTargetNamespaceExeption;
            this.entityResolver = entityResolver;
        }

        public void error(SAXParseException exception) throws SAXException {
            if (skipTargetNamespaceException
                    && exception
                            .getMessage()
                            .startsWith(
                                    "TargetNamespace.2: Expecting no namespace, but the schema document has a target name")) {
                return;
            }

            errors.add(exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            errors.add(exception);
        }

        public void warning(SAXParseException exception) throws SAXException {
            // do nothing
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws IOException, SAXException {
            if (entityResolver != null) {
                return this.entityResolver.resolveEntity(publicId, systemId);
            } else {
                return super.resolveEntity(publicId, systemId);
            }
        }
    }

    static List<SAXException> exception(Exception e) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Validation error", e);
        }
        return Arrays.asList(new SAXParseException(e.getLocalizedMessage(), null));
    }
}
