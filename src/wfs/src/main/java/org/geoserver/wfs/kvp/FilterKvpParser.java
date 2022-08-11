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

package org.geoserver.wfs.kvp;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.gml.GMLFilterDocument;
import org.geotools.gml.GMLFilterGeometry;
import org.geotools.xml.filter.FilterFilter;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.opengis.filter.Filter;
import org.vfny.geoserver.util.requests.FilterHandlerImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserAdapter;

/**
 * A base {@code FILTER} parameter parser that expects a subclass to provide the actual {@link
 * Parser} configuration for the expected OGC Filter Encoding spec version.
 *
 * @author Justin Deoliveira
 * @author Gabriel Roldan
 */
public abstract class FilterKvpParser extends KvpParser {

    private EntityResolverProvider entityResolverProvider;

    public FilterKvpParser(GeoServer geoServer) {
        super("filter", List.class);
        this.entityResolverProvider = new EntityResolverProvider(geoServer);
    }

    /**
     * Subclasses shall implement to provide the parse() method the appropriate parser Configuration
     * for the filter spec version they specialize on.
     *
     * @return The Configuration for the appropriate Filter spec version.
     */
    protected abstract Configuration getParserConfiguration();

    public Object parse(String value) throws Exception {
        // create the parser
        final Configuration configuration = getParserConfiguration();
        final Parser parser = new Parser(configuration);
        parser.setEntityResolver(entityResolverProvider.getEntityResolver());

        // seperate the individual filter strings
        List<String> unparsed = KvpUtils.readFlat(value, KvpUtils.OUTER_DELIMETER);
        List<Filter> filters = new ArrayList<>();

        Iterator<String> i = unparsed.listIterator();
        while (i.hasNext()) {
            String string = i.next();
            if ("".equals(string.trim())) {
                filters.add(Filter.INCLUDE);
            } else {
                try {
                    Filter filter = (Filter) parser.parse(new StringReader(string));

                    if (filter == null) {
                        throw new NullPointerException();
                    }

                    filters.add(filter);
                } catch (Exception e) {
                    // parsing failed, fall back to old parser
                    String msg = "Unable to parse filter: " + string;
                    LOGGER.log(Level.WARNING, msg, e);

                    Filter filter = parseXMLFilterWithOldParser(new StringReader(string));

                    if (filter != null) {
                        filters.add(filter);
                    }
                }
            }
        }

        return filters;
    }

    /**
     * Reads the Filter XML request into a geotools Feature object.
     *
     * <p>This uses the "old" filter parser and is around to maintain some backwards compatability
     * with cases in which the new parser chokes on a filter that hte old one could handle.
     *
     * @param rawRequest The plain POST text from the client.
     * @return The geotools filter constructed from rawRequest.
     * @throws WfsException For any problems reading the request.
     */
    protected Filter parseXMLFilterWithOldParser(Reader rawRequest) throws ServiceException {
        // translate string into a proper SAX input source
        InputSource requestSource = new InputSource(rawRequest);

        // instantiante parsers and content handlers
        FilterHandlerImpl contentHandler = new FilterHandlerImpl();
        contentHandler.setEntityResolver(entityResolverProvider.getEntityResolver());
        FilterFilter filterParser = new FilterFilter(contentHandler, null);
        GMLFilterGeometry geometryFilter = new GMLFilterGeometry(filterParser);
        GMLFilterDocument documentFilter = new GMLFilterDocument(geometryFilter);

        // read in XML file and parse to content handler
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            ParserAdapter adapter = new ParserAdapter(parser.getParser());
            adapter.setEntityResolver(entityResolverProvider.getEntityResolver());
            adapter.setContentHandler(documentFilter);
            adapter.parse(requestSource);
            LOGGER.fine("just parsed: " + requestSource);
        } catch (SAXException e) {
            throw new ServiceException(
                    e,
                    "XML getFeature request SAX parsing error",
                    XmlRequestReader.class.getName());
        } catch (IOException e) {
            throw new ServiceException(
                    e, "XML get feature request input error", XmlRequestReader.class.getName());
        } catch (ParserConfigurationException e) {
            throw new ServiceException(
                    e, "Some sort of issue creating parser", XmlRequestReader.class.getName());
        }

        LOGGER.fine("passing filter: " + contentHandler.getFilter());

        return contentHandler.getFilter();
    }
}
