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

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.map.GIFMapResponse;
import org.geotools.data.FeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GetMapCallbackTest extends WMSDimensionsTestSupport {

    private GetMap getMap;

    @Before
    public void cleanupCallbacks() {
        getMap = applicationContext.getBean(GetMap.class);
        getMap.setGetMapCallbacks(Collections.emptyList());
    }

    @Test
    public void testStandardWorkflow() throws Exception {
        TestCallback callback = new TestCallback();
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss");
        assertXpathExists("rss/channel/title[text() = 'cite:Lakes,cite:Forests']", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(2, callback.layers.size());
        assertEquals(1, callback.mapContents.size());
        assertEquals(1, callback.maps.size());
        assertEquals(0, callback.exceptions.size());
    }

    @Test
    public void testBreakRequest() throws Exception {
        final String message = "This layer is not allowed";
        TestCallback callback =
                new TestCallback() {
                    @Override
                    public Layer beforeLayer(WMSMapContent content, Layer layer) {
                        throw new RuntimeException(message);
                    }
                };
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss&version=1.1.0");
        // print(dom);
        assertXpathExists("/ServiceExceptionReport", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(0, callback.layers.size());
        assertEquals(0, callback.mapContents.size());
        assertEquals(0, callback.maps.size());
        assertEquals(1, callback.exceptions.size());
        assertEquals(message, callback.exceptions.get(0).getMessage());
    }

    @Test
    public void testAddLayer() throws Exception {
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(MockData.BRIDGES));
        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                ft.getFeatureSource(null, null);
        Style style = getCatalog().getStyleByName("point").getStyle();
        final FeatureLayer layer = new FeatureLayer(fs, style);
        layer.setTitle("extra");
        TestCallback callback =
                new TestCallback() {
                    @Override
                    public WMSMapContent beforeRender(WMSMapContent mapContent) {
                        mapContent.addLayer(layer);
                        return super.beforeRender(mapContent);
                    }
                };
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss&version=1.1.0");
        // print(dom);
        assertXpathExists("rss/channel/title[text() = 'cite:Lakes,cite:Forests,extra']", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(3, callback.layers.size());
        assertEquals(1, callback.mapContents.size());
        assertEquals(1, callback.maps.size());
        assertEquals(0, callback.exceptions.size());

        assertEquals(layer, callback.layers.get(2));
    }

    @Test
    public void testRemoveLayer() throws Exception {
        TestCallback callback =
                new TestCallback() {
                    @Override
                    public Layer beforeLayer(WMSMapContent content, Layer layer) {
                        if ("cite:Lakes".equals(layer.getTitle())) {
                            return null;
                        } else {
                            return super.beforeLayer(content, layer);
                        }
                    }
                };
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss&version=1.1.0");
        // print(dom);
        assertXpathExists("rss/channel/title[text() = 'cite:Forests']", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(1, callback.layers.size());
        assertEquals(1, callback.mapContents.size());
        assertEquals(1, callback.maps.size());
        assertEquals(0, callback.exceptions.size());

        assertEquals("cite:Forests", callback.layers.get(0).getTitle());
    }

    @Test
    public void testAnimator() throws Exception {
        TestCallback callback = new TestCallback();
        getMap.setGetMapCallbacks(Arrays.asList(callback));
        String requestURL =
                "wms/animate?layers="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&aparam=fake_param&avalues=val0,val1,val2";

        MockHttpServletResponse resp = getAsServletResponse(requestURL);

        assertEquals("image/gif", resp.getContentType());

        // the three frames, plus the fake request the animator does to get the mime type and
        // map content for the output
        assertEquals(4, callback.requests.size());
        assertEquals(4, callback.mapContentsInited.size());
        assertEquals(4, callback.layers.size());
        assertEquals(4, callback.mapContents.size());
        assertEquals(4, callback.maps.size());
        assertEquals(0, callback.exceptions.size());
    }

    @Test
    public void testAnimatedGifDimensions() throws Exception {
        TestCallback callback = new TestCallback();
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-02,2011-05-04,2011-05-10&format="
                                + GIFMapResponse.IMAGE_GIF_SUBTYPE_ANIMATED);

        assertEquals("image/gif", response.getContentType());

        // the three frames in a single request
        assertEquals(1, callback.requests.size());
        assertEquals(3, callback.mapContentsInited.size());
        assertEquals(3, callback.layers.size());
        assertEquals(3, callback.mapContents.size());
        assertEquals(1, callback.maps.size());
        assertEquals(0, callback.exceptions.size());
    }

    private class TestCallback implements GetMapCallback {

        private List<GetMapRequest> requests = new ArrayList<>();

        private List<WMSMapContent> mapContentsInited = new ArrayList<>();

        private List<Layer> layers = new ArrayList<>();

        private List<WMSMapContent> mapContents = new ArrayList<>();

        private List<WebMap> maps = new ArrayList<>();

        private List<Throwable> exceptions = new ArrayList<>();

        @Override
        public synchronized GetMapRequest initRequest(GetMapRequest request) {
            requests.add(request);
            return request;
        }

        @Override
        public synchronized void initMapContent(WMSMapContent mapContent) {
            mapContentsInited.add(mapContent);
        }

        @Override
        public synchronized Layer beforeLayer(WMSMapContent content, Layer layer) {
            layers.add(layer);
            return layer;
        }

        @Override
        public synchronized WMSMapContent beforeRender(WMSMapContent mapContent) {
            mapContents.add(mapContent);
            return mapContent;
        }

        @Override
        public synchronized WebMap finished(WebMap map) {
            maps.add(map);
            return map;
        }

        @Override
        public synchronized void failed(Throwable t) {
            exceptions.add(t);
        }
    }
}
