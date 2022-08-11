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

package org.geoserver.wms.dynamic.legendgraphic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.GetLegendGraphicKvpReader;
import org.geotools.process.raster.DynamicColorMapTest;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.xml.styling.SLDTransformer;
import org.junit.Test;

public class DynamicGetLegendGraphicsCallbackTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle(
                "style_rgb", "test-data/style_rgb.sld", DynamicColorMapTest.class, catalog);
        Map<LayerProperty, Object> properties = new HashMap<>();
        properties.put(LayerProperty.STYLE, "style_rgb");
        testData.addRasterLayer(
                new QName(MockData.DEFAULT_URI, "watertemp_dynamic", MockData.DEFAULT_PREFIX),
                "test-data/watertemp_dynamic.zip",
                null,
                properties,
                DynamicColorMapTest.class,
                catalog);
    }

    @Test
    public void testLegendExpasion() throws Exception {
        // manually parse a request
        GetLegendGraphicKvpReader requestReader =
                GeoServerExtensions.bean(GetLegendGraphicKvpReader.class);
        Map params = new KvpMap();
        params.put("VERSION", "1.0.0");
        params.put("REQUEST", "GetLegendGraphic");
        params.put("LAYER", "watertemp_dynamic");
        params.put("STYLE", "style_rgb");
        params.put("FORMAT", "image/png");
        GetLegendGraphicRequest getLegendGraphics =
                requestReader.read(new GetLegendGraphicRequest(), params, params);

        // setup to call the callback
        Service wmsService = (Service) GeoServerExtensions.bean("wms-1_1_1-ServiceDescriptor");
        Operation op =
                new Operation(
                        "getLegendGraphic", wmsService, null, new Object[] {getLegendGraphics});
        Request request = new Request();
        request.setKvp(params);
        request.setRawKvp(params);
        Dispatcher.REQUEST.set(request);
        DynamicGetLegendGraphicDispatcherCallback callback =
                GeoServerExtensions.bean(DynamicGetLegendGraphicDispatcherCallback.class);
        callback.operationDispatched(null, op);

        // get the style and check it has been transformed (we started with one having a
        // transformation, now
        // we have a static colormap)
        Style style = getLegendGraphics.getLegends().get(0).getStyle();
        FeatureTypeStyle fts = style.featureTypeStyles().get(0);
        assertNull(fts.getTransformation());
        RasterSymbolizer rs = (RasterSymbolizer) fts.rules().get(0).symbolizers().get(0);
        assertNotNull(rs.getColorMap());
    }

    void logStyle(Style style) {
        SLDTransformer tx = new SLDTransformer();
        tx.setIndentation(2);
        try {
            tx.transform(style, System.out);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
