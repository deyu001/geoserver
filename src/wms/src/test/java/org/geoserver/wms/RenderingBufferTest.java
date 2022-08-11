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

package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests that the admin specified per layer buffer parameter is taken into account
 *
 * @author Andrea Aime - OpenGeo
 */
public class RenderingBufferTest extends WMSTestSupport {

    static final QName LINE_WIDTH_LAYER =
            new QName(MockData.CITE_URI, "LineWidth", MockData.CITE_PREFIX);

    static final String LINE_WIDTH_STYLE = "linewidth";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle(LINE_WIDTH_STYLE, "linewidth.sld", getClass(), getCatalog());
        Map<LayerProperty, Object> properties = new HashMap<>();
        properties.put(LayerProperty.STYLE, LINE_WIDTH_STYLE);
        testData.addVectorLayer(
                LINE_WIDTH_LAYER, properties, "LineWidth.properties", getClass(), getCatalog());
    }

    @Before
    public void resetBuffer() {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().remove(LayerInfo.BUFFER);
        catalog.save(layer);
    }

    @Test
    public void testGetMapNoBuffer() throws Exception {
        String request =
                "cite/wms?request=getmap&service=wms"
                        + "&layers="
                        + getLayerId(LINE_WIDTH_LAYER)
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());

        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        assertEquals(0, countNonBlankPixels("testGetMap", image, BG_COLOR));
    }

    @Test
    public void testGetFeatureInfoNoBuffer() throws Exception {
        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request =
                "cite/wms?request=getfeatureinfo&service=wms"
                        + "&layers="
                        + layerName
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers="
                        + layerName
                        + "&info_format=application/vnd.ogc.gml";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("0", "count(//gml:featureMember)", dom);
    }

    @Test
    public void testGetMapExplicitBuffer() throws Exception {
        String request =
                "cite/wms?request=getmap&service=wms"
                        + "&layers="
                        + getLayerId(LINE_WIDTH_LAYER)
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&buffer=30";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());

        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        int nonBlankPixels = countNonBlankPixels("testGetMap", image, BG_COLOR);
        assertTrue(nonBlankPixels > 0);
    }

    @Test
    public void testGetFeatureInfoExplicitBuffer() throws Exception {
        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request =
                "cite/wms?version=1.1.1&request=getfeatureinfo&service=wms"
                        + "&layers="
                        + layerName
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers="
                        + layerName
                        + "&info_format=application/vnd.ogc.gml&buffer=30";
        Document dom = getAsDOM(request);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
    }

    @Test
    public void testGetMapConfiguredBuffer() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().put(LayerInfo.BUFFER, 30);
        catalog.save(layer);

        String request =
                "cite/wms?request=getmap&service=wms"
                        + "&layers="
                        + getLayerId(LINE_WIDTH_LAYER)
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());

        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        assertTrue(countNonBlankPixels("testGetMap", image, BG_COLOR) > 0);
    }

    @Test
    public void testGetFeatureInfoConfiguredBuffer() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().put(LayerInfo.BUFFER, 30);
        catalog.save(layer);

        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request =
                "cite/wms?version=1.1.1&request=getfeatureinfo&service=wms"
                        + "&layers="
                        + layerName
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers="
                        + layerName
                        + "&info_format=application/vnd.ogc.gml";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
    }
}
