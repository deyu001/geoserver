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

package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.Test;
import org.w3c.dom.Document;

public class GPXPPIOTest extends GeoServerTestSupport {

    private GPXPPIO ppio;

    private XpathEngine xpath;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("", "http://www.topografix.com/GPX/1/1");
        namespaces.put("gpx", "http://www.topografix.com/GPX/1/1");
        namespaces.put("att", "http://www.geoserver.org");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpInternal() throws Exception {
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        SettingsInfo settings = global.getSettings();
        ContactInfo contact = settings.getContact();
        contact.setContactOrganization("GeoServer");
        contact.setOnlineResource("http://www.geoserver.org");
        gs.save(global);

        ppio = new GPXPPIO(gs);
    }

    @Test
    public void testEncodePolygon() throws IOException {
        FeatureTypeInfo fti =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) fti.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ppio.encode(fc, bos);
            fail("Should have thrown an exception");
        } catch (IOException e) {
            assert (e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testEncodeMultiLinestring() throws Exception {
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.ROAD_SEGMENTS));
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) fti.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppio.encode(fc, bos);
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);
        checkValidationErorrs(dom, "./src/test/resources/org/geoserver/wps/ppio/gpx.xsd");

        assertEquals("GeoServer", xpath.evaluate("/gpx:gpx/@creator", dom));
        assertEquals("GeoServer", xpath.evaluate("/gpx:gpx/gpx:metadata/gpx:link/gpx:text", dom));
        assertEquals(
                "http://www.geoserver.org",
                xpath.evaluate("/gpx:gpx/gpx:metadata/gpx:link/@href", dom));
        assertEquals(5, xpath.getMatchingNodes("/gpx:gpx/gpx:trk", dom).getLength());
        assertEquals("102", xpath.evaluate("/gpx:gpx/gpx:trk[1]/gpx:extensions/att:FID", dom));
        assertEquals("Route 5", xpath.evaluate("/gpx:gpx/gpx:trk[1]/gpx:extensions/att:NAME", dom));
    }

    @Test
    public void testEncodeLinestring() throws Exception {
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.LINES));
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) fti.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppio.encode(fc, bos);
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);
        checkValidationErorrs(dom, "./src/test/resources/org/geoserver/wps/ppio/gpx.xsd");

        assertEquals("GeoServer", xpath.evaluate("/gpx:gpx/@creator", dom));
        assertEquals("GeoServer", xpath.evaluate("/gpx:gpx/gpx:metadata/gpx:link/gpx:text", dom));
        assertEquals(
                "http://www.geoserver.org",
                xpath.evaluate("/gpx:gpx/gpx:metadata/gpx:link/@href", dom));
        assertEquals(1, xpath.getMatchingNodes("/gpx:gpx/gpx:rte", dom).getLength());
        assertEquals("t0001 ", xpath.evaluate("/gpx:gpx/gpx:rte[1]/gpx:extensions/att:id", dom));
        // check the data was reprojected to wgs84
        assertEquals("4.523789", xpath.evaluate("//gpx:rte/gpx:rtept[1]/@lat", dom));
        assertEquals("-92.998873", xpath.evaluate("//gpx:rte/gpx:rtept[1]/@lon", dom));
        assertEquals("4.524241", xpath.evaluate("//gpx:rte/gpx:rtept[2]/@lat", dom));
        assertEquals("-92.998422", xpath.evaluate("//gpx:rte/gpx:rtept[2]/@lon", dom));
    }

    @Test
    public void testEncodePoints() throws Exception {
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POINTS));
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) fti.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppio.encode(fc, bos);
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);
        checkValidationErorrs(dom, "./src/test/resources/org/geoserver/wps/ppio/gpx.xsd");
        assertEquals("GeoServer", xpath.evaluate("/gpx:gpx/@creator", dom));
        assertEquals("GeoServer", xpath.evaluate("/gpx:gpx/gpx:metadata/gpx:link/gpx:text", dom));
        assertEquals(
                "http://www.geoserver.org",
                xpath.evaluate("/gpx:gpx/gpx:metadata/gpx:link/@href", dom));
        assertEquals(1, xpath.getMatchingNodes("/gpx:gpx/gpx:wpt", dom).getLength());
        assertEquals("t0000", xpath.evaluate("/gpx:gpx/gpx:wpt[1]/gpx:extensions/att:id", dom));
    }
}
