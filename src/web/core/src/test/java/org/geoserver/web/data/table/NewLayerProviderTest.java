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

package org.geoserver.web.data.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layer.NewLayerPageProvider;
import org.junit.Test;

public class NewLayerProviderTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testFeatureType() {
        StoreInfo cite = getCatalog().getStoreByName(MockData.CITE_PREFIX, StoreInfo.class);
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(cite.getId());
        provider.setShowPublished(true);
        assertTrue(provider.size() > 0);
        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }

    @Test
    public void testCoverages() {
        StoreInfo dem =
                getCatalog().getStoreByName(MockData.TASMANIA_DEM.getLocalPart(), StoreInfo.class);
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(dem.getId());
        provider.setShowPublished(true);
        assertTrue(provider.size() > 0);
        provider.setShowPublished(false);
        // todo: fix this
        // assertEquals(0, provider.size());
    }

    @Test
    public void testEmpty() {
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setShowPublished(true);
        assertEquals(0, provider.size());
        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }

    /**
     * As per GEOS-3120, if a resource is published but it's name changed, it should still show up
     * as published. It wasn't being the case due to comparing the resource's name instead of the
     * nativeName against the name the DataStore provides
     */
    @Test
    public void testPublishedUnpublishedWithChangedResourceName() {
        Catalog catalog = getCatalog();
        StoreInfo cite = catalog.getStoreByName(MockData.CITE_PREFIX, StoreInfo.class);

        List<FeatureTypeInfo> resources = catalog.getResourcesByStore(cite, FeatureTypeInfo.class);
        assertFalse(resources.isEmpty());

        final int numberOfPublishedResources = resources.size();

        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(cite.getId());
        provider.setShowPublished(false);
        assertEquals(0, provider.size());

        provider.setShowPublished(true);
        assertEquals(numberOfPublishedResources, provider.size());

        FeatureTypeInfo typeInfo = resources.get(0);
        typeInfo.setName("notTheNativeName");
        catalog.save(typeInfo);

        provider = new NewLayerPageProvider();
        provider.setStoreId(cite.getId());

        provider.setShowPublished(true);
        assertEquals(numberOfPublishedResources, provider.size());

        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }
}
