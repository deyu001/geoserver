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
package org.geoserver.web.demo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;
import org.opengis.filter.Filter;

public class PreviewLayerProviderMixedModeTest extends GeoServerWicketTestSupport {

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/web/demo/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the users */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_mixed", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings =
                catalog.getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));

        // user in mixed mode
        tam.putLimits(
                "cite_mixed",
                buildings,
                new VectorAccessLimits(
                        CatalogMode.MIXED, null, Filter.EXCLUDE, null, Filter.EXCLUDE));
    }

    @Test
    public void testMixedMode() throws Exception {
        PreviewLayerProvider provider = new PreviewLayerProvider();

        // full access
        login("cite", "cite");
        assertTrue(previewHasBuildings(provider));

        // no access, but no exception either, since this is not a direct access
        login("cite_mixed", "cite");
        assertFalse(previewHasBuildings(provider));
    }

    private boolean previewHasBuildings(PreviewLayerProvider provider) {
        Iterator<PreviewLayer> it = provider.iterator(0, provider.size());
        String buildingsPrefixedName = getLayerId(SystemTestData.BUILDINGS);
        while (it.hasNext()) {
            PreviewLayer pl = it.next();
            if (buildingsPrefixedName.equals(pl.getName())) {
                return true;
            }
        }
        return false;
    }
}
