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

package org.geoserver.gwc.wmts;

import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureIterator;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/** XML transformer for the get feature operation. */
class FeaturesTransformer extends TransformerBase {

    public FeaturesTransformer(WMS wms) {
        setIndentation(2);
        setEncoding(wms.getCharSet());
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new TranslatorSupport(handler);
    }

    class TranslatorSupport extends TransformerBase.TranslatorSupport {

        public TranslatorSupport(ContentHandler handler) {
            super(handler, null, null);
        }

        @Override
        public void encode(Object object) throws IllegalArgumentException {
            if (!(object instanceof Domains)) {
                throw new IllegalArgumentException(
                        "Expected domains info but instead got: "
                                + object.getClass().getCanonicalName());
            }
            Domains domains = (Domains) object;
            Attributes nameSpaces =
                    createAttributes(
                            new String[] {
                                "xmlns:xs", "http://www.w3.org/2001/XMLSchema",
                                "xmlns:gml", "http://www.opengis.net/gml",
                                "xmlns:wmts", "http://www.opengis.net/wmts/1.0"
                            });
            start("wmts:FeatureCollection", nameSpaces);
            FeatureIterator iterator = domains.getFeatureCollection().features();
            try {
                while (iterator.hasNext()) {
                    SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
                    handleFeature(simpleFeature, domains.getDimensions());
                }
            } finally {
                iterator.close();
            }
            end("wmts:FeatureCollection");
        }

        /** Encodes a feature in the XML. */
        private void handleFeature(SimpleFeature feature, List<Dimension> dimensions) {
            Attributes attributes = createAttributes(new String[] {"gml:id", feature.getID()});
            start("wmts:feature", attributes);
            start("wmts:footprint");
            // encode the geometry
            GeometryDescriptor geometryDescriptor =
                    feature.getFeatureType().getGeometryDescriptor();
            Geometry geometry = (Geometry) feature.getAttribute(geometryDescriptor.getName());
            handleGeometry(geometry);
            // encode the dimensions
            end("wmts:footprint");
            for (Dimension dimension : dimensions) {
                handleDimension(feature, dimension);
            }
            end("wmts:feature");
        }

        /** Encodes a Geometry in GML. */
        private void handleGeometry(Geometry geometry) {
            try {
                QName elementName = org.geotools.gml2.GML._Geometry;
                if (geometry instanceof Point) {
                    elementName = org.geotools.gml2.GML.Point;
                } else if (geometry instanceof LineString) {
                    elementName = org.geotools.gml2.GML.LineString;
                } else if (geometry instanceof Polygon) {
                    elementName = org.geotools.gml2.GML.Polygon;
                } else if (geometry instanceof MultiPoint) {
                    elementName = org.geotools.gml2.GML.MultiPoint;
                } else if (geometry instanceof MultiLineString) {
                    elementName = org.geotools.gml2.GML.MultiLineString;
                } else if (geometry instanceof MultiPolygon) {
                    elementName = org.geotools.gml2.GML.MultiPolygon;
                }
                Encoder encoder = new Encoder(new GMLConfiguration());
                encoder.encode(geometry, elementName, contentHandler);
            } catch (Exception exception) {
                throw new RuntimeException(
                        "Cannot transform the specified geometry in GML.", exception);
            }
        }

        /** Encodes a dimension extracting the dimension value from the feature. */
        private void handleDimension(SimpleFeature feature, Dimension dimension) {
            Object value = feature.getAttribute(dimension.getAttributes().first);
            Attributes attributes =
                    createAttributes(new String[] {"name", dimension.getDimensionName()});
            element("wmts:dimension", DimensionsUtils.formatDomainValue(value), attributes);
        }
    }
}
