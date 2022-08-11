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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.w3c.dom.Element;

/**
 * Reads a legacy coverage info.xml file.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LegacyCoverageInfoReader {

    /** Root catalog element. */
    Element coverage;

    /** The directory containing the feature type info.xml file */
    File parentDirectory;

    /**
     * Parses the info.xml file into a DOM.
     *
     * <p>This method *must* be called before any other methods.
     *
     * @param file The info.xml file.
     * @throws IOException In event of a parser error.
     */
    public void read(File file) throws IOException {
        parentDirectory = file.getParentFile();

        try (Reader reader = XmlCharsetDetector.getCharsetAwareReader(new FileInputStream(file))) {
            coverage = ReaderUtils.parse(reader);
        }
    }

    public String format() {
        return coverage.getAttribute("format");
    }

    public String name() {
        return ReaderUtils.getChildText(coverage, "name");
    }

    public String description() {
        return ReaderUtils.getChildText(coverage, "description");
    }

    public String label() {
        return ReaderUtils.getChildText(coverage, "label");
    }

    public Map<String, String> metadataLink() {
        HashMap<String, String> ml = new HashMap<>();
        ml.put("about", ReaderUtils.getChildAttribute(coverage, "metadataLink", "about"));
        ml.put(
                "metadataType",
                ReaderUtils.getChildAttribute(coverage, "metadataLink", ",metadataType"));

        return ml;
    }

    public List<String> keywords() {
        String raw = ReaderUtils.getChildText(coverage, "keywords");
        StringTokenizer st = new StringTokenizer(raw, ", ");
        List<String> keywords = new ArrayList<>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }

        return keywords;
    }

    public String defaultStyle() throws Exception {
        Element styles = ReaderUtils.getChildElement(coverage, "styles");
        return ReaderUtils.getAttribute(styles, "default", true);
    }

    public List<String> styles() throws Exception {
        Element styleRoot = ReaderUtils.getChildElement(coverage, "styles");
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

    public Map<String, Object> envelope() throws Exception {
        Element envelopeElement = ReaderUtils.getChildElement(coverage, "envelope");
        HashMap<String, Object> e = new HashMap<>();

        String nativeCrsWkt = ReaderUtils.getAttribute(envelopeElement, "crs", false);
        nativeCrsWkt = nativeCrsWkt.replaceAll("'", "\"");

        e.put("crs", nativeCrsWkt);
        e.put("srsName", ReaderUtils.getAttribute(envelopeElement, "srsName", false));

        Element[] posElements = ReaderUtils.getChildElements(envelopeElement, "pos");
        String[] pos1 = posElements[0].getFirstChild().getTextContent().split(" ");
        String[] pos2 = posElements[1].getFirstChild().getTextContent().split(" ");

        e.put("x1", Double.parseDouble(pos1[0]));
        e.put("y1", Double.parseDouble(pos1[1]));
        e.put("x2", Double.parseDouble(pos2[0]));
        e.put("y2", Double.parseDouble(pos2[1]));

        return e;
    }

    public Map<String, Object> grid() throws Exception {
        Element gridElement = ReaderUtils.getChildElement(coverage, "grid");
        if (gridElement == null) {
            return null;
        }

        HashMap<String, Object> grid = new HashMap<>();

        grid.put(
                "dimension",
                Integer.parseInt(ReaderUtils.getAttribute(gridElement, "dimension", true)));

        Element lowElement = ReaderUtils.getChildElement(gridElement, "low");
        String[] lows = lowElement.getFirstChild().getTextContent().trim().split(" ");
        int[] low = new int[lows.length];
        for (int i = 0; i < low.length; i++) {
            low[i] = Integer.parseInt(lows[i]);
        }
        grid.put("low", low);

        Element highElement = ReaderUtils.getChildElement(gridElement, "high");
        String[] highs = highElement.getFirstChild().getTextContent().trim().split(" ");
        int[] high = new int[highs.length];
        for (int i = 0; i < high.length; i++) {
            high[i] = Integer.parseInt(highs[i]);
        }
        grid.put("high", high);

        Element[] axisNameElements = ReaderUtils.getChildElements(gridElement, "axisName");
        String[] axisName = new String[axisNameElements.length];
        for (int i = 0; i < axisName.length; i++) {
            axisName[i] = axisNameElements[i].getFirstChild().getTextContent();
        }
        grid.put("axisName", axisName);

        Element geoTransformElement = ReaderUtils.getChildElement(gridElement, "geoTransform");
        if (geoTransformElement != null) {
            Map<String, Double> geoTransform = new HashMap<>();
            String scaleX = ReaderUtils.getChildText(geoTransformElement, "scaleX");
            String scaleY = ReaderUtils.getChildText(geoTransformElement, "scaleY");
            String shearX = ReaderUtils.getChildText(geoTransformElement, "shearX");
            String shearY = ReaderUtils.getChildText(geoTransformElement, "shearY");
            String translateX = ReaderUtils.getChildText(geoTransformElement, "translateX");
            String translateY = ReaderUtils.getChildText(geoTransformElement, "translateY");

            geoTransform.put("scaleX", scaleX != null ? Double.valueOf(scaleX) : null);
            geoTransform.put("scaleY", scaleY != null ? Double.valueOf(scaleY) : null);
            geoTransform.put("shearX", shearX != null ? Double.valueOf(shearX) : null);
            geoTransform.put("shearY", shearY != null ? Double.valueOf(shearY) : null);
            geoTransform.put("translateX", translateX != null ? Double.valueOf(translateX) : null);
            geoTransform.put("translateY", translateY != null ? Double.valueOf(translateY) : null);

            grid.put("geoTransform", geoTransform);
        }
        return grid;
    }

    public List<Map<String, Object>> coverageDimensions() throws Exception {
        Element[] cdElements = ReaderUtils.getChildElements(coverage, "CoverageDimension");
        List<Map<String, Object>> cds = new ArrayList<>();
        for (Element cdElement : cdElements) {
            Map<String, Object> cd = new HashMap<>();
            cd.put("name", ReaderUtils.getChildText(cdElement, "name"));
            cd.put("description", ReaderUtils.getChildText(cdElement, "description"));

            Element intervalElement = ReaderUtils.getChildElement(cdElement, "interval");
            double min = Double.parseDouble(ReaderUtils.getChildText(intervalElement, "min"));
            double max = Double.parseDouble(ReaderUtils.getChildText(intervalElement, "max"));

            cd.put("min", min);
            cd.put("max", max);
            cds.add(cd);
        }

        return cds;
    }

    public List<String> requestCRSs() throws Exception {
        Element supportedCRS = ReaderUtils.getChildElement(coverage, "supportedCRSs");
        String[] requestCRS =
                ReaderUtils.getChildText(supportedCRS, "requestCRSs").trim().split(",");
        return Arrays.asList(requestCRS);
    }

    public List<String> responseCRSs() throws Exception {
        Element supportedCRS = ReaderUtils.getChildElement(coverage, "supportedCRSs");
        String[] responseCRS =
                ReaderUtils.getChildText(supportedCRS, "responseCRSs").trim().split(",");
        return Arrays.asList(responseCRS);
    }

    public String nativeFormat() throws Exception {
        Element supportedFormats = ReaderUtils.getChildElement(coverage, "supportedFormats");
        return ReaderUtils.getAttribute(supportedFormats, "nativeFormat", true);
    }

    public List<String> supportedFormats() throws Exception {
        Element supportedFormats = ReaderUtils.getChildElement(coverage, "supportedFormats");
        String[] formats = ReaderUtils.getChildText(supportedFormats, "formats").split(",");
        return Arrays.asList(formats);
    }

    public String defaultInterpolation() throws Exception {
        Element supportedFormats = ReaderUtils.getChildElement(coverage, "supportedInterpolations");
        return ReaderUtils.getAttribute(supportedFormats, "default", true);
    }

    public List<String> supportedInterpolations() throws Exception {
        Element supportedFormats = ReaderUtils.getChildElement(coverage, "supportedInterpolations");
        String[] interpolations =
                ReaderUtils.getChildText(supportedFormats, "interpolationMethods").split(",");
        return Arrays.asList(interpolations);
    }

    public Map<String, Serializable> parameters() {
        Element parameters = ReaderUtils.getChildElement(coverage, "parameters");
        if (parameters == null) {
            return Collections.emptyMap();
        }

        HashMap<String, Serializable> map = new HashMap<>();
        Element[] parameter = ReaderUtils.getChildElements(parameters, "parameter");
        for (Element element : parameter) {
            String name = element.getAttribute("name");
            String value = element.getAttribute("value");

            map.put(name, value);
        }

        return map;
    }

    public String wmsPath() {
        return ReaderUtils.getChildText(coverage, "wmspath");
    }

    public String parentDirectoryName() {
        return parentDirectory.getName();
    }
}
