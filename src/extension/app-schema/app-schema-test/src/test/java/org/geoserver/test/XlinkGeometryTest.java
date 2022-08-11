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

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author Niels Charlier, Curtin University Of Technology
 *     <p>Tests manual and automatic xlink:href for Geometries
 */
public class XlinkGeometryTest extends AbstractAppSchemaTestSupport {

    @Override
    protected XlinkGeometryMockData createTestData() {
        return new XlinkGeometryMockData();
    }

    /** Tests whether automatic and manual xlink:href is encoded in all Geometry Types */
    @Test
    public void testGeometry() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=ex:MyTestFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));

        // test manual xlink:href
        assertXpathEvaluatesTo(
                "xlinkvalue1",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:geometryref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue2",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:curveref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue3",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:pointref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue4",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:linestringref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue5",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:surfaceref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue6",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:polygonref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue7",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multicurveref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue8",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multipointref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue9",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multilinestringref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue10",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multisurfaceref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue11",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multipolygonref/@xlink:href",
                doc);

        // test auto xlink:href
        assertXpathEvaluatesTo(
                "#geom1",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:geometry/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom2",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:curve/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom3",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:point/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom4",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:linestring/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom5",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:surface/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom6",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:polygon/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom7",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multicurve/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom8",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipoint/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom9",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multilinestring/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom10",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multisurface/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom11",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipolygon/@xlink:href",
                doc);

        // test if nodes are empty
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:geometry/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:curve/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:point/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:linestring/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:surface/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:polygon/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multicurve/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipoint/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multilinestring/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multisurface/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipolygon/*",
                doc);

        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:geometryref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:curveref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:pointref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:linestringref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:surfaceref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:polygonref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multicurveref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipointref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multilinestringref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multisurfaceref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipolygonref/*",
                doc);
    }
}
