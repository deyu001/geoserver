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

package org.geoserver.catalog.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.checkerframework.checker.units.qual.m;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geoserver.platform.resource.Resource;
import org.locationtech.jts.geom.Envelope;
import org.w3c.dom.Element;

/**
 * Reads a legacy GeoServer 1.x feature type info.xml file.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LegacyFeatureTypeInfoReader {

    /** Root featureType element. */
    Element featureType;

    /** The directory containing the feature type info.xml file */
    Resource parentDirectory;

    /**
     * Parses the info.xml file into a DOM.
     *
     * <p>This method *must* be called before any other methods.
     *
     * @param file The info.xml file.
     * @throws IOException In event of a parser error.
     */
    public void read(Resource file) throws IOException {
        parentDirectory = file.parent();

        try (Reader reader = XmlCharsetDetector.getCharsetAwareReader(file.in())) {
            featureType = ReaderUtils.parse(reader);
        }
    }

    public String dataStore() throws Exception {
        return ReaderUtils.getAttribute(featureType, "datastore", true);
    }

    public String name() {
        return ReaderUtils.getChildText(featureType, "name");
    }

    public String alias() {
        return ReaderUtils.getChildText(featureType, "alias");
    }

    public String srs() throws Exception {
        return ReaderUtils.getChildText(featureType, "SRS");
    }

    public int srsHandling() {
        String s = ReaderUtils.getChildText(featureType, "SRSHandling");
        if (s == null || "".equals(s)) {
            return -1;
        }

        return Integer.parseInt(s);
    }

    public String title() {
        return ReaderUtils.getChildText(featureType, "title");
    }

    public String abstrct() {
        return ReaderUtils.getChildText(featureType, "abstract");
    }

    public List<String> keywords() {
        String raw = ReaderUtils.getChildText(featureType, "keywords");
        if (raw == null || "".equals(raw)) {
            return new ArrayList<>();
        }
        StringTokenizer st = new StringTokenizer(raw, ", ");
        List<String> keywords = new ArrayList<>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }

        return keywords;
    }

    public List<Map<String, String>> metadataLinks() {
        List<Map<String, String>> links = new ArrayList<>();
        Element metadataLinks = ReaderUtils.getChildElement(featureType, "metadataLinks");
        if (metadataLinks != null) {
            Element[] metadataLink = ReaderUtils.getChildElements(metadataLinks, "metadataLink");
            for (Element e : metadataLink) {
                Map<String, String> m = new HashMap<>();
                m.put("metadataType", e.getAttribute("metadataType"));
                m.put("type", e.getAttribute("type"));
                if (e.getFirstChild() != null) {
                    m.put(null, e.getFirstChild().getNodeValue());
                }
                links.add(m);
            }
        }

        return links;
    }

    public Envelope latLonBoundingBox() throws Exception {
        Element box = ReaderUtils.getChildElement(featureType, "latLonBoundingBox");
        double minx = ReaderUtils.getDoubleAttribute(box, "minx", true);
        double miny = ReaderUtils.getDoubleAttribute(box, "miny", true);
        double maxx = ReaderUtils.getDoubleAttribute(box, "maxx", true);
        double maxy = ReaderUtils.getDoubleAttribute(box, "maxy", true);

        return new Envelope(minx, maxx, miny, maxy);
    }

    public Envelope nativeBoundingBox() throws Exception {
        Element box = ReaderUtils.getChildElement(featureType, "nativeBBox");
        boolean dynamic = ReaderUtils.getBooleanAttribute(box, "dynamic", false, true);
        if (dynamic) {
            return null;
        }

        double minx = ReaderUtils.getDoubleAttribute(box, "minx", true);
        double miny = ReaderUtils.getDoubleAttribute(box, "miny", true);
        double maxx = ReaderUtils.getDoubleAttribute(box, "maxx", true);
        double maxy = ReaderUtils.getDoubleAttribute(box, "maxy", true);

        return new Envelope(minx, maxx, miny, maxy);
    }

    public String defaultStyle() throws Exception {
        Element styles = ReaderUtils.getChildElement(featureType, "styles");
        return ReaderUtils.getAttribute(styles, "default", false);
    }

    public List<String> styles() throws Exception {
        Element styleRoot = ReaderUtils.getChildElement(featureType, "styles");
        if (styleRoot != null) {
            List<String> styleNames = new ArrayList<>();
            Element[] styles = ReaderUtils.getChildElements(styleRoot, "style");
            for (Element style : styles) {
                styleNames.add(style.getTextContent().trim());
            }
            return styleNames;
        } else {
            return Collections.emptyList();
        }
    }

    public Map<String, Object> legendURL() throws Exception {
        Element legendURL = ReaderUtils.getChildElement(featureType, "LegendURL");

        if (legendURL != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("width", Integer.parseInt(ReaderUtils.getAttribute(legendURL, "width", true)));
            map.put(
                    "height",
                    Integer.parseInt(ReaderUtils.getAttribute(legendURL, "height", true)));
            map.put("format", ReaderUtils.getChildText(legendURL, "Format", true));
            map.put(
                    "onlineResource",
                    ReaderUtils.getAttribute(
                            ReaderUtils.getChildElement(legendURL, "OnlineResource", true),
                            "xlink:href",
                            true));
            return map;
        }

        return null;
    }

    public boolean cachingEnabled() {
        Element cacheInfo = ReaderUtils.getChildElement(featureType, "cacheinfo");
        if (cacheInfo != null) {
            try {
                return "true".equals(ReaderUtils.getAttribute(cacheInfo, "enabled", false));
            } catch (Exception e) {
            }
        }
        return false;
    }

    public String cacheAgeMax() {
        Element cacheInfo = ReaderUtils.getChildElement(featureType, "cacheinfo");
        if (cacheInfo != null) {
            try {
                return ReaderUtils.getAttribute(cacheInfo, "maxage", false);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public boolean searchable() {
        Element searchable = ReaderUtils.getChildElement(featureType, "searchable");
        if (searchable != null) {
            try {
                return "true".equals(ReaderUtils.getAttribute(searchable, "enabled", false));
            } catch (Exception e) {
            }
        }

        return false;
    }

    public String regionateAttribute() {
        Element regionateAttribute = ReaderUtils.getChildElement(featureType, "regionateAttribute");
        if (regionateAttribute != null) {
            return regionateAttribute.getAttribute("value");
        }

        return null;
    }

    public String regionateStrategy() {
        Element regionateStrategy = ReaderUtils.getChildElement(featureType, "regionateStrategy");
        if (regionateStrategy != null) {
            return regionateStrategy.getAttribute("value");
        }

        return null;
    }

    public int regionateFeatureLimit() {
        Element regionateFeatureLimit =
                ReaderUtils.getChildElement(featureType, "regionateFeatureLimit");
        try {
            return Integer.valueOf(regionateFeatureLimit.getAttribute("value"));
        } catch (Exception e) {
            return 10;
        }
    }

    public int maxFeatures() {
        Element maxFeatures = ReaderUtils.getChildElement(featureType, "maxFeatures");
        try {
            return Integer.valueOf(maxFeatures.getTextContent());
        } catch (Exception e) {
            return 0;
        }
    }

    public String wmsPath() {
        return ReaderUtils.getChildText(featureType, "wmspath");
    }

    public String parentDirectoryName() {
        return parentDirectory.name();
    }
}
