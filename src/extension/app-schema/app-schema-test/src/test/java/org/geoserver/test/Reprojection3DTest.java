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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test for 3D BBOXes in App-schema Support for offline as well as online testing
 *
 * @author Niels Charlier
 */
public class Reprojection3DTest extends AbstractAppSchemaTestSupport {

    @Override
    protected BBox3DMockData createTestData() {
        return new BBox3DMockData();
    }

    /** Tests re-projection of NonFeatureTypeProxy. */
    @Test
    public void testReprojection() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&srsName=EPSG:4891");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));

        assertEquals(
                "3",
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsDimension",
                        doc));
        assertEquals(
                "http://www.opengis.net/gml/srs/epsg.xml#4891",
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsName",
                        doc));

        String point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (112 - 7.91243825667) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (7 - 7.87193142436) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (5 - 7.97579399217) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (88 - 7.82161881682) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf5']/gsml:shape/gml:LineString/gml:posList",
                        doc);
        String[] coords = point.split(" ");
        assertTrue(Double.parseDouble(coords[2]) - (112 - 7.91243825667) < 0.00001);
        assertTrue(Double.parseDouble(coords[5]) - (7 - 7.87193142436) < 0.00001);
        assertTrue(Double.parseDouble(coords[8]) - (5 - 7.97579399217) < 0.00001);
        assertTrue(Double.parseDouble(coords[11]) - (88 - 7.82161881682) < 0.00001);
    }
}
