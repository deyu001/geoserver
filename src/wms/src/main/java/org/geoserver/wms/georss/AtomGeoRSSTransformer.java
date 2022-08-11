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
import java.util.Date;
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

public class AtomGeoRSSTransformer extends GeoRSSTransformerBase {

    private WMS wms;

    public AtomGeoRSSTransformer(WMS wms) {
        this.wms = wms;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new AtomGeoRSSTranslator(wms, handler);
    }

    public class AtomGeoRSSTranslator extends GeoRSSTranslatorSupport {

        private WMS wms;

        public AtomGeoRSSTranslator(WMS wms, ContentHandler contentHandler) {
            super(contentHandler, null, "http://www.w3.org/2005/Atom");
            this.wms = wms;
            nsSupport.declarePrefix("georss", "http://www.georss.org/georss");
        }

        public void encode(Object o) throws IllegalArgumentException {
            WMSMapContent map = (WMSMapContent) o;

            start("feed");

            // title
            element("title", AtomUtils.getFeedTitle(map));

            // TODO: Revist URN scheme
            element("id", AtomUtils.getFeedURI(map));

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, AtomUtils.getFeedURL(map));
            atts.addAttribute(null, "rel", "rel", null, "self");
            element("link", null, atts);

            // updated
            element("updated", AtomUtils.dateToRFC3339(new Date()));

            // entries
            try {
                encodeEntries(map);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            end("feed");
        }

        void encodeEntries(WMSMapContent map) throws IOException {
            List featureCollections = loadFeatureCollections(map);
            for (Object featureCollection : featureCollections) {
                SimpleFeatureCollection features = (SimpleFeatureCollection) featureCollection;

                try (FeatureIterator<SimpleFeature> iterator = features.features()) {

                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        try {
                            encodeEntry(feature, map);
                        } catch (Exception e) {
                            LOGGER.warning("Encoding failed for feature: " + feature.getID());
                            LOGGER.log(Level.FINE, "", e);
                        }
                    }
                }
            }
        }

        void encodeEntry(SimpleFeature feature, WMSMapContent map) {
            start("entry");

            // title
            element("title", feature.getID());

            start("author");
            element("name", wms.getGeoServer().getSettings().getContact().getContactPerson());
            end("author");

            // id
            element("id", AtomUtils.getEntryURI(wms, feature, map));

            String link = AtomUtils.getEntryURL(wms, feature, map);
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, link);
            atts.addAttribute(null, "rel", "rel", null, "self");
            element("link", null, atts);

            // updated
            element("updated", AtomUtils.dateToRFC3339(new Date()));

            // content
            atts = new AttributesImpl();
            atts.addAttribute(null, "type", "type", null, "html");
            element("content", AtomUtils.getFeatureDescription(feature), atts);

            // where
            if (geometryEncoding == GeometryEncoding.LATLONG
                    || !(feature.getDefaultGeometry() instanceof GeometryCollection)) {
                start("georss:where");
                geometryEncoding.encode((Geometry) feature.getDefaultGeometry(), this);
                end("georss:where");
                end("entry");
            } else {
                GeometryCollection col = (GeometryCollection) feature.getDefaultGeometry();
                start("georss:where");
                geometryEncoding.encode(col.getGeometryN(0), this);
                end("georss:where");
                end("entry");

                for (int i = 1; i < col.getNumGeometries(); i++) {
                    encodeRelatedGeometryEntry(
                            col.getGeometryN(i), feature.getID(), link, link + "#" + i);
                }
            }
        }

        void encodeRelatedGeometryEntry(Geometry g, String title, String link, String id) {
            start("entry");
            element("id", id);
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, link);
            atts.addAttribute(null, "rel", "rel", null, "related");
            element("link", null, atts);
            element("title", title);
            start("georss:where");
            geometryEncoding.encode(g, this);
            end("georss:where");
            end("entry");
        }
    }
}
