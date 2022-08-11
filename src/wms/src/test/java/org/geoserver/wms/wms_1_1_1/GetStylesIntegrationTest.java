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

package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;
import org.junit.Test;

public class GetStylesIntegrationTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        String lakes = MockData.LAKES.getLocalPart();
        String forests = MockData.FORESTS.getLocalPart();
        String bridges = MockData.BRIDGES.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("lakesGroup");
        lg.getLayers().add(catalog.getLayerByName(lakes));
        lg.getStyles().add(catalog.getStyleByName(lakes));
        lg.getLayers().add(catalog.getLayerByName(forests));
        lg.getStyles().add(catalog.getStyleByName(forests));
        lg.getLayers().add(catalog.getLayerByName(bridges));
        lg.getStyles().add(catalog.getStyleByName(bridges));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);

        // makes the lakes layer a multi-style one
        LayerInfo lakesLayer = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakesLayer.getStyles().add(catalog.getStyleByName(MockData.FORESTS.getLocalPart()));
        catalog.save(lakesLayer);
    }

    @Test
    public void testSimple() throws Exception {
        try (InputStream stream =
                get(
                        "wms?service=WMS&version=1.1.1&&request=GetStyles&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&sldver=1.0.0")) {

            SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null));
            parser.setInput(stream);

            StyledLayerDescriptor sld = parser.parseSLD();
            assertEquals(1, sld.getStyledLayers().length);

            NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
            assertEquals(getLayerId(MockData.BASIC_POLYGONS), layer.getName());
            assertEquals(1, layer.styles().size());

            Style style = layer.styles().get(0);
            assertTrue(style.isDefault());
            assertEquals("BasicPolygons", style.getName());
        }
    }

    @Test
    public void testGroup() throws Exception {
        try (InputStream stream =
                get(
                        "wms?service=WMS&version=1.1.1&request=GetStyles&layers=lakesGroup&sldver=1.0.0")) {

            SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null));
            parser.setInput(stream);

            StyledLayerDescriptor sld = parser.parseSLD();
            assertEquals(1, sld.getStyledLayers().length);

            NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
            assertEquals("lakesGroup", layer.getName());

            // groups have no style
            assertEquals(0, layer.styles().size());
        }
    }

    @Test
    public void testMultiStyle() throws Exception {
        try (InputStream stream =
                get(
                        "wms?service=WMS&version=1.1.1&request=GetStyles&layers="
                                + getLayerId(MockData.LAKES)
                                + "&sldver=1.0.0")) {

            SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null));
            parser.setInput(stream);

            StyledLayerDescriptor sld = parser.parseSLD();
            assertEquals(1, sld.getStyledLayers().length);

            NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
            assertEquals(getLayerId(MockData.LAKES), layer.getName());
            assertEquals(2, layer.styles().size());

            Style style = layer.styles().get(0);
            assertTrue(style.isDefault());
            assertEquals("Lakes", style.getName());

            style = layer.styles().get(1);
            assertFalse(style.isDefault());
            assertEquals("Forests", style.getName());
        }
    }
}
