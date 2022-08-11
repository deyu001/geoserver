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

package org.geoserver.wms.georss;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.xml.transform.Translator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes an RSS feed tagged with geo information.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class RSSGeoRSSTransformer extends GeoRSSTransformerBase {

    private WMS wms;

    public RSSGeoRSSTransformer(WMS wms) {
        this.wms = wms;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new RSSGeoRSSTranslator(wms, handler);
    }

    class RSSGeoRSSTranslator extends GeoRSSTranslatorSupport {
        private WMS wms;

        public RSSGeoRSSTranslator(WMS wms, ContentHandler contentHandler) {
            super(contentHandler, null, null);
            this.wms = wms;
            nsSupport.declarePrefix("georss", "http://www.georss.org/georss");
            nsSupport.declarePrefix("atom", "http://www.w3.org/2005/Atom");
        }

        public void encode(Object o) throws IllegalArgumentException {
            WMSMapContent map = (WMSMapContent) o;

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "version", "version", null, "2.0");

            start("rss", atts);
            start("channel");

            element("title", AtomUtils.getFeedTitle(map));
            element("description", AtomUtils.getFeedDescription(map));

            start("link");
            cdata(AtomUtils.getFeedURL(map));
            end("link");

            atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, AtomUtils.getFeedURL(map));
            atts.addAttribute(null, "rel", "rel", null, "self");
            element("atom:link", null, atts);

            // items
            try {
                encodeItems(map);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            end("channel");
            end("rss");
        }

        void encodeItems(WMSMapContent map) throws IOException {
            List featureCollections = loadFeatureCollections(map);
            for (Object featureCollection : featureCollections) {
                SimpleFeatureCollection features = (SimpleFeatureCollection) featureCollection;

                try (FeatureIterator<SimpleFeature> iterator = features.features()) {

                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        try {
                            encodeItem(feature, map);
                        } catch (Exception e) {
                            LOGGER.warning("Encoding failed for feature: " + feature.getID());
                            LOGGER.log(Level.FINE, "", e);
                        }
                    }
                }
            }
        }

        void encodeItem(SimpleFeature feature, WMSMapContent map) throws IOException {
            start("item");

            String title = feature.getID();
            String link = null;

            try {
                title = AtomUtils.getFeatureTitle(feature);
                link = AtomUtils.getEntryURL(wms, feature, map);
            } catch (Exception e) {
                String msg = "Error occured executing title template for: " + feature.getID();
                LOGGER.log(Level.WARNING, msg, e);
            }

            element("title", title);

            // create the link as getFeature request with fid filter
            start("link");
            cdata(link);
            end("link");

            start("guid");
            cdata(link);
            end("guid");

            start("description");
            cdata(AtomUtils.getFeatureDescription(feature));
            end("description");

            GeometryCollection col =
                    feature.getDefaultGeometry() instanceof GeometryCollection
                            ? (GeometryCollection) feature.getDefaultGeometry()
                            : null;

            if (geometryEncoding == GeometryEncoding.LATLONG
                    || (col == null && feature.getDefaultGeometry() != null)) {
                geometryEncoding.encode((Geometry) feature.getDefaultGeometry(), this);
                end("item");
            } else if (col == null) {
                end("item");
            } else {
                geometryEncoding.encode(col.getGeometryN(0), this);
                end("item");

                for (int i = 1; i < col.getNumGeometries(); i++) {
                    encodeRelatedGeometryItem(col.getGeometryN(i), title, link, i);
                }
            }
        }

        void encodeRelatedGeometryItem(Geometry g, String title, String link, int count) {
            start("item");
            element("title", "Continuation of " + title);
            element("link", link);
            element("guid", link + "#" + count);
            element("description", "Continuation of " + title);
            geometryEncoding.encode(g, this);
            end("item");
        }
    }
}
