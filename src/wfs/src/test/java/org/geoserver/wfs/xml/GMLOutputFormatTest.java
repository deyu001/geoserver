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

package org.geoserver.wfs.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.wfs.v2_0.WFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class GMLOutputFormatTest extends WFSTestSupport {
    private int defaultNumDecimals = -1;
    private boolean defaultForceDecimal = false;
    private boolean defaultPadWithZeros = false;

    @Before
    public void saveDefaultFormattingOptions() {
        if (defaultNumDecimals < 0) {
            FeatureTypeInfo info =
                    getGeoServer()
                            .getCatalog()
                            .getResourceByName(
                                    new NameImpl(
                                            MockData.BASIC_POLYGONS.getPrefix(),
                                            MockData.BASIC_POLYGONS.getLocalPart()),
                                    FeatureTypeInfo.class);
            defaultNumDecimals = info.getNumDecimals();
            defaultForceDecimal = info.getForcedDecimal();
            defaultPadWithZeros = info.getPadWithZeros();
        }
    }

    @After
    public void restoreDefaultFormattingOptions() {
        FeatureTypeInfo info =
                getGeoServer()
                        .getCatalog()
                        .getResourceByName(
                                new NameImpl(
                                        MockData.BASIC_POLYGONS.getPrefix(),
                                        MockData.BASIC_POLYGONS.getLocalPart()),
                                FeatureTypeInfo.class);
        info.setNumDecimals(defaultNumDecimals);
        info.setForcedDecimal(defaultForceDecimal);
        info.setPadWithZeros(defaultPadWithZeros);
    }

    @Test
    public void testGML2() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=gml2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=text/xml; subtype%3Dgml/2.1.2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=text/xml; subtype%3Dgml/2.1.2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));
    }

    @Test
    public void testGML2CoordinatesFormatting() throws Exception {
        enableCoordinatesFormatting();
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(
                "-2.0000,-1.0000 2.0000,6.0000",
                dom.getElementsByTagName("gml:coordinates").item(0).getTextContent());
    }

    private void enableCoordinatesFormatting() {
        FeatureTypeInfo info =
                getGeoServer()
                        .getCatalog()
                        .getResourceByName(
                                new NameImpl(
                                        MockData.BASIC_POLYGONS.getPrefix(),
                                        MockData.BASIC_POLYGONS.getLocalPart()),
                                FeatureTypeInfo.class);
        info.setNumDecimals(4);
        info.setForcedDecimal(true);
        info.setPadWithZeros(true);
        getGeoServer().getCatalog().save(info);
    }

    @Test
    public void testGML2GZIP() throws Exception {
        //        InputStream input = get(
        // "wfs?request=getfeature&version=1.0.0&outputFormat=gml2-gzip&typename=" +
        //            MockData.BASIC_POLYGONS.getPrefix() + ":" +
        // MockData.BASIC_POLYGONS.getLocalPart());
        //        GZIPInputStream zipped = new GZIPInputStream( input );
        //
        //        Document dom = dom( zipped );
        //        zipped.close();
        //
        //        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        //        assertNotNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        //        assertNull( getFirstElementByTagName(dom, "gml:exterior"));
    }

    @Test
    public void testGML3() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml3&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=gml3&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=text/xml; subtype%3Dgml/3.1.1&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=text/xml; subtype%3Dgml/3.1.1&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));
    }

    @Test
    public void testGML3CoordinatesFormatting() throws Exception {
        enableCoordinatesFormatting();
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml3&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(
                "-1.0000 0.0000 0.0000 1.0000 1.0000 0.0000 0.0000 -1.0000 -1.0000 0.0000",
                dom.getElementsByTagName("gml:posList").item(0).getTextContent());
    }

    @Test
    public void testGML32() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=2.0.0&outputFormat=gml32&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(WFS.NAMESPACE, dom.getDocumentElement().getNamespaceURI());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
    }

    @Test
    public void testGML32CoordinatesFormatting() throws Exception {
        enableCoordinatesFormatting();
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=2.0.0&outputFormat=gml32&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(
                "0.0000 -1.0000 1.0000 0.0000 0.0000 1.0000 -1.0000 0.0000 0.0000 -1.0000",
                dom.getElementsByTagName("gml:posList").item(0).getTextContent());
    }
}
