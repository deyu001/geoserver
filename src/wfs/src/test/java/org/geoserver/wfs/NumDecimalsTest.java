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

package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerInfo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NumDecimalsTest extends WFSTestSupport {

    @Before
    public void resetNumDecimals() {
        Catalog cat = getCatalog();
        FeatureTypeInfo ft1 = cat.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        FeatureTypeInfo ft2 = cat.getFeatureTypeByName("sf", "AggregateGeoFeature");
        ft1.setNumDecimals(0);
        ft2.setNumDecimals(0);
        cat.save(ft1);
        cat.save(ft2);
    }

    @Test
    public void testDefaults() throws Exception {
        Document dom =
                getAsDOM("wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008&version=1.0.0");
        runAssertions(dom, 3);
    }

    @Test
    public void testGlobal() throws Exception {
        Catalog cat = getCatalog();
        FeatureTypeInfo ft = cat.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        ft.setNumDecimals(-1);
        cat.save(ft);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setNumDecimals(1);
        getGeoServer().save(global);

        Document dom =
                getAsDOM("wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008&version=1.0.0");
        runAssertions(dom, 1);
    }

    @Test
    public void testPerFeatureType() throws Exception {
        Catalog cat = getCatalog();
        FeatureTypeInfo ft = cat.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        ft.setNumDecimals(1);
        cat.save(ft);

        Document dom =
                getAsDOM("wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008&version=1.0.0");
        runAssertions(dom, 1);
    }

    @Test
    public void testMultipleFeatureTypes() throws Exception {
        Catalog cat = getCatalog();
        FeatureTypeInfo ft1 = cat.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        FeatureTypeInfo ft2 = cat.getFeatureTypeByName("sf", "AggregateGeoFeature");
        ft1.setNumDecimals(3);
        cat.save(ft1);

        ft2.setNumDecimals(1);
        cat.save(ft2);

        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008,AggregateGeoFeature.f009&version=1.0.0");
        runAssertions(dom, 3);
    }

    void runAssertions(Document dom, int numdecimals) throws Exception {
        NodeList nl = dom.getElementsByTagName("gml:coordinates");
        for (int x = 0; x < nl.getLength(); x++) {
            Element e = (Element) nl.item(x);
            String[] tuples = e.getFirstChild().getNodeValue().split(" ");
            for (String tuple : tuples) {
                String[] coord = tuple.split(",");
                for (String s : coord) {
                    int dot = s.indexOf('.');
                    assertEquals(numdecimals, s.substring(dot + 1).length());
                }
            }
        }
    }
}
