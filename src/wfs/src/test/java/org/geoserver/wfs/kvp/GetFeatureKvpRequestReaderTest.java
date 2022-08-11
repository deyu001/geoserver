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

package org.geoserver.wfs.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.WFSException;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;

public class GetFeatureKvpRequestReaderTest extends GeoServerSystemTestSupport {

    private static GetFeatureKvpRequestReader reader;

    @Override
    protected void onSetUp(SystemTestData data) throws Exception {
        reader =
                new GetFeatureKvpRequestReader(
                        GetFeatureType.class,
                        getGeoServer(),
                        CommonFactoryFinder.getFilterFactory(null));
    }

    /** https://osgeo-org.atlassian.net/browse/GEOS-1875 */
    @Test
    @SuppressWarnings("unchecked")
    public void testInvalidTypeNameBbox() throws Exception {
        Map raw = new HashMap();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("bbox", "-80.4864795578115,25.6176257083275,-80.3401307394915,25.7002737069969");
        raw.put("typeName", "cite:InvalidTypeName");

        Map parsed = parseKvp(raw);

        try {
            // before fix for GEOS-1875 this would bomb out with an NPE instead of the proper
            // exception
            reader.read(WfsFactory.eINSTANCE.createGetFeatureType(), parsed, raw);
        } catch (WFSException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("typeName", e.getLocator());
            // System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains("cite:InvalidTypeName"));
        }
    }

    /** Same as GEOS-1875, but let's check without bbox and without name prefix */
    @SuppressWarnings("unchecked")
    @Test
    public void testInvalidTypeName() throws Exception {
        Map raw = new HashMap();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", "InvalidTypeName");

        try {
            Map parsed = parseKvp(raw);
            reader.read(WfsFactory.eINSTANCE.createGetFeatureType(), parsed, raw);
        } catch (WFSException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("typeName", e.getLocator());
            // System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains("InvalidTypeName"));
        }
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-1875 */
    @SuppressWarnings("unchecked")
    @Test
    public void testUserProvidedNamespace() throws Exception {
        final String localPart = SystemTestData.MLINES.getLocalPart();
        final String namespace = SystemTestData.MLINES.getNamespaceURI();
        final String alternamePrefix = "ex";
        final String alternameTypeName = alternamePrefix + ":" + localPart;

        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", alternameTypeName);
        raw.put("namespace", "xmlns(" + alternamePrefix + "=" + namespace + ")");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        QueryType query = (QueryType) parsedReq.getQuery().get(0);
        List<QName> typeNames = query.getTypeName();
        assertEquals(1, typeNames.size());
        assertEquals(SystemTestData.MLINES, typeNames.get(0));
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-1875 */
    @SuppressWarnings("unchecked")
    @Test
    public void testUserProvidedDefaultNamespace() throws Exception {
        final QName qName = SystemTestData.STREAMS;
        final String typeName = qName.getLocalPart();
        final String defaultNamespace = qName.getNamespaceURI();

        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", typeName);
        raw.put("namespace", "xmlns(" + defaultNamespace + ")");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        QueryType query = (QueryType) parsedReq.getQuery().get(0);
        List<QName> typeNames = query.getTypeName();
        assertEquals(1, typeNames.size());
        assertEquals(qName, typeNames.get(0));
    }

    @Test
    public void testViewParams() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", getLayerId(SystemTestData.STREAMS));
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        assertEquals(1, parsedReq.getViewParams().size());
        List viewParams = parsedReq.getViewParams();
        assertEquals(1, viewParams.size());
        @SuppressWarnings("unchecked")
        Map<String, String> vp1 = (Map) viewParams.get(0);
        assertEquals("WHERE PERSONS > 1000000", vp1.get("where"));
        assertEquals("ABCD", vp1.get("str"));
    }

    @Test
    public void testViewParamsMulti() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put(
                "typeName",
                getLayerId(SystemTestData.STREAMS)
                        + ","
                        + getLayerId(SystemTestData.BASIC_POLYGONS));
        raw.put(
                "viewParams",
                "where:WHERE PERSONS > 1000000;str:ABCD,where:WHERE PERSONS > 10;str:FOO");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        List viewParams = parsedReq.getViewParams();
        assertEquals(2, viewParams.size());
        @SuppressWarnings("unchecked")
        Map<String, String> vp1 = (Map) viewParams.get(0);
        assertEquals("WHERE PERSONS > 1000000", vp1.get("where"));
        assertEquals("ABCD", vp1.get("str"));
        @SuppressWarnings("unchecked")
        Map<String, String> vp2 = (Map) viewParams.get(1);
        assertEquals("WHERE PERSONS > 10", vp2.get("where"));
        assertEquals("FOO", vp2.get("str"));
    }

    @Test
    public void testViewParamsFanOut() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put(
                "typeName",
                getLayerId(SystemTestData.STREAMS)
                        + ","
                        + getLayerId(SystemTestData.BASIC_POLYGONS));
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        List viewParams = parsedReq.getViewParams();
        assertEquals(2, viewParams.size());
        @SuppressWarnings("unchecked")
        Map<String, String> vp1 = (Map) viewParams.get(0);
        assertEquals("WHERE PERSONS > 1000000", vp1.get("where"));
        assertEquals("ABCD", vp1.get("str"));
        @SuppressWarnings("unchecked")
        Map<String, String> vp2 = (Map) viewParams.get(1);
        assertEquals("WHERE PERSONS > 1000000", vp2.get("where"));
        assertEquals("ABCD", vp2.get("str"));
    }
}
