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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMockData.DummyRasterMapProducer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.EnvFunction;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opengis.filter.FilterFactory;

/**
 * Unit test for {@link GetMap}
 *
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.5.x
 */
public class GetMapTest {

    private WMSMockData mockData;

    private GetMapRequest request;

    private GetMap getMapOp;

    @Before
    public void setUp() throws Exception {
        mockData = new WMSMockData();
        mockData.setUp();

        request = mockData.createRequest();
        // add a layer so its a valid request
        MapLayerInfo layer = mockData.addFeatureTypeLayer("testType", Point.class);
        request.setLayers(Arrays.asList(layer));

        getMapOp = new GetMap(mockData.getWMS());
    }

    @Test
    public void testExecuteNoExtent() {
        request.setBbox(null);
        assertInvalidMandatoryParam("MissingBBox");
    }

    @Test
    public void testExecuteEmptyExtent() {
        request.setBbox(new Envelope());
        assertInvalidMandatoryParam("InvalidBBox");
    }

    @Test
    public void testSingleVectorLayer() throws IOException {
        request.setFormat(DummyRasterMapProducer.MIME_TYPE);

        MapLayerInfo layer = mockData.addFeatureTypeLayer("testSingleVectorLayer", Point.class);
        request.setLayers(Arrays.asList(layer));

        final DummyRasterMapProducer producer = new DummyRasterMapProducer();
        final WMS wms =
                new WMS(mockData.getGeoServer()) {
                    @Override
                    public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                        if (DummyRasterMapProducer.MIME_TYPE.equals(mimeType)) {
                            return producer;
                        }
                        return null;
                    }
                };
        getMapOp = new GetMap(wms);
        getMapOp.run(request);
        assertTrue(producer.produceMapCalled);
    }

    @Test
    public void testExecuteNoLayers() throws Exception {
        request.setLayers(null);
        assertInvalidMandatoryParam("LayerNotDefined");
    }

    @Test
    public void testExecuteNoWidth() {
        request.setWidth(0);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");

        request.setWidth(-1);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");
    }

    @Test
    public void testExecuteNoHeight() {
        request.setHeight(0);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");

        request.setHeight(-1);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");
    }

    @Test
    public void testExecuteInvalidFormat() {
        request.setFormat("non-existent-output-format");
        assertInvalidMandatoryParam("InvalidFormat");
    }

    @Test
    public void testExecuteNoFormat() {
        request.setFormat(null);
        assertInvalidMandatoryParam("InvalidFormat");
    }

    @Test
    public void testExecuteNoStyles() {
        request.setStyles(null);
        assertInvalidMandatoryParam("StyleNotDefined");
    }

    @Test
    public void testEnviroment() {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        EnvFunction.setLocalValues(Collections.singletonMap("myParam", 23));

        final DummyRasterMapProducer producer =
                new DummyRasterMapProducer() {
                    @Override
                    public WebMap produceMap(WMSMapContent ctx)
                            throws ServiceException, IOException {
                        assertEquals(23, ff.function("env", ff.literal("myParam")).evaluate(null));
                        assertEquals(
                                10,
                                ff.function("env", ff.literal("otherParam"), ff.literal(10))
                                        .evaluate(null));
                        super.produceMapCalled = true;
                        return null;
                    }
                };
        final WMS wms =
                new WMS(mockData.getGeoServer()) {
                    @Override
                    public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                        if (DummyRasterMapProducer.MIME_TYPE.equals(mimeType)) {
                            return producer;
                        }
                        return null;
                    }
                };

        getMapOp = new GetMap(wms);
        getMapOp.run(request);
        assertTrue(producer.produceMapCalled);
        // there used to be a test that the values are reset right after
        // GetMap, but this is wrong, the producer can be streaming and thus
        // the env variable must stay until the full request lifecycle is done,
        // we now use a DispatcherCallback to clean up the env variables:
        // EnvVariableCleaner
    }

    private void assertInvalidMandatoryParam(String expectedExceptionCode) {
        try {
            getMapOp.run(request);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(expectedExceptionCode, e.getCode());
        }
    }
}
