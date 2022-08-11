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


package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import org.geotools.data.complex.AppSchemaDataAccess;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test integration of {@link AppSchemaDataAccess} with GeoServer.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class XlinkWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected XlinkMockData createTestData() {
        return new XlinkMockData();
    }

    /** Test whether GetCapabilities returns wfs:WFS_Capabilities. */
    @Test
    public void testGetCapabilities() {
        Document doc = getAsDOM("wfs?request=GetCapabilities");
        LOGGER.info("WFS GetCapabilities response:\n" + prettyString(doc));
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
    }

    /** Test whether DescribeFeatureType returns xsd:schema. */
    @Test
    public void testDescribeFeatureType() {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&typename=gsml:MappedFeature");
        LOGGER.info("WFS DescribeFeatureType response:\n" + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() {

        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");

        LOGGER.info("WFS testGetFeatureContent response:\n" + prettyString(doc));

        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        assertXpathEvaluatesTo(
                "GUNTHORPE FORMATION", "//gsml:MappedFeature[@gml:id='mf1']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25699",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification/@xlink:href",
                doc);

        // mf2
        assertXpathEvaluatesTo(
                "MERCIA MUDSTONE GROUP", "//gsml:MappedFeature[@gml:id='mf2']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25678",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/@xlink:href",
                doc);

        // mf3
        assertXpathEvaluatesTo(
                "CLIFTON FORMATION", "//gsml:MappedFeature[@gml:id='mf3']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href",
                doc);

        // mf4
        assertXpathEvaluatesTo(
                "MURRADUC BASALT", "//gsml:MappedFeature[@gml:id='mf4']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25682",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/@xlink:href",
                doc);
    }
}
