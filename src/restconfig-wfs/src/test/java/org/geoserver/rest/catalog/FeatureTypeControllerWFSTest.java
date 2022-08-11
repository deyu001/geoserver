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

package org.geoserver.rest.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class FeatureTypeControllerWFSTest extends CatalogRESTTestSupport {

    private static String BASEPATH = RestBaseController.ROOT_PATH;

    @Before
    public void removePropertyStores() {
        removeStore("gs", "pds");
        removeStore("gs", "ngpds");
    }

    @Before
    public void addPrimitiveGeoFeature() throws IOException {
        revertLayer(SystemTestData.PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testGetAllByWorkspace() throws Exception {
        Document dom = getAsDOM(BASEPATH + "/workspaces/sf/featuretypes.xml");
        assertEquals(
                catalog.getFeatureTypesByNamespace(catalog.getNamespaceByPrefix("sf")).size(),
                dom.getElementsByTagName("featureType").getLength());
    }

    void addPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsa.0='zero'|POINT(0 0)\n");
        writer.write("pdsa.1='one'|POINT(1 1)\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("pdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsb.0='two'|POINT(2 2)\n");
        writer.write("pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("pdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=" + (configureFeatureType ? "all" : "none");
        put(
                BASEPATH + "/workspaces/gs/datastores/pds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    void addGeomlessPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,intProperty:Integer\n");
        writer.write("ngpdsa.0='zero'|0\n");
        writer.write("ngpdsa.1='one'|1\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("ngpdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,intProperty:Integer\n");
        writer.write("ngpdsb.0='two'|2\n");
        writer.write("ngpdsb.1='trhee'|3\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("ngpdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=" + (configureFeatureType ? "all" : "none");
        put(
                BASEPATH + "/workspaces/gs/datastores/ngpds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    /** Add a property data store with multiple feature types, but only configure the first. */
    void addPropertyDataStoreOnlyConfigureFirst() throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsa.0='zero'|POINT(0 0)\n");
        writer.write("pdsa.1='one'|POINT(1 1)\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("pdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsb.0='two'|POINT(2 2)\n");
        writer.write("pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("pdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=first";
        put(
                BASEPATH + "/workspaces/gs/datastores/pds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    @Test
    public void testPostAsXML() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&typename=sf:pdsa");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addPropertyDataStore(false);
        String xml =
                "<featureType>"
                        + "<name>pdsa</name>"
                        + "<nativeName>pdsa</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<nativeBoundingBox>"
                        + "<minx>0.0</minx>"
                        + "<maxx>1.0</maxx>"
                        + "<miny>0.0</miny>"
                        + "<maxy>1.0</maxy>"
                        + "<crs>EPSG:4326</crs>"
                        + "</nativeBoundingBox>"
                        + "<store>pds</store>"
                        + "</featureType>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        BASEPATH + "/workspaces/gs/datastores/pds/featuretypes/", xml, "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/gs/datastores/pds/featuretypes/pdsa"));

        dom = getAsDOM("wfs?request=getfeature&typename=gs:pdsa");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(2, dom.getElementsByTagName("gs:pdsa").getLength());
    }

    @Test
    public void testPostAsXMLInlineStore() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&typename=sf:pdsa");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addPropertyDataStore(false);
        String xml =
                "<featureType>"
                        + "<name>pdsa</name>"
                        + "<nativeName>pdsa</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<nativeBoundingBox>"
                        + "<minx>0.0</minx>"
                        + "<maxx>1.0</maxx>"
                        + "<miny>0.0</miny>"
                        + "<maxy>1.0</maxy>"
                        + "<crs>EPSG:4326</crs>"
                        + "</nativeBoundingBox>"
                        + "<store>pds</store>"
                        + "</featureType>";
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/workspaces/gs/featuretypes/", xml, "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/gs/featuretypes/pdsa"));

        dom = getAsDOM("wfs?request=getfeature&typename=gs:pdsa");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(2, dom.getElementsByTagName("gs:pdsa").getLength());
    }

    @Test
    public void testPostAsJSON() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&typename=sf:pdsa");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addPropertyDataStore(false);
        String json =
                "{"
                        + "'featureType':{"
                        + "'name':'pdsa',"
                        + "'nativeName':'pdsa',"
                        + "'srs':'EPSG:4326',"
                        + "'nativeBoundingBox':{"
                        + "'minx':0.0,"
                        + "'maxx':1.0,"
                        + "'miny':0.0,"
                        + "'maxy':1.0,"
                        + "'crs':'EPSG:4326'"
                        + "},"
                        + "'nativeCRS':'EPSG:4326',"
                        + "'store':'pds'"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        BASEPATH + "/workspaces/gs/datastores/pds/featuretypes/",
                        json,
                        "text/json");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/gs/datastores/pds/featuretypes/pdsa"));

        dom = getAsDOM("wfs?request=getfeature&typename=gs:pdsa");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(2, dom.getElementsByTagName("gs:pdsa").getLength());
    }
}
