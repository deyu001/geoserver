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

package org.geoserver.security.decorators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.AbstractAuthorizationTest;
import org.junit.Test;

public class ReadSecuredCatalogDecoratorsTest extends AbstractAuthorizationTest {

    @Test
    public void testSecuredLayerInfoFeatures() {
        SecuredLayerInfo ro = new SecuredLayerInfo(statesLayer, WrapperPolicy.hide(null));

        assertFalse(statesLayer.getResource() instanceof SecuredFeatureTypeInfo);
        assertTrue(ro.getResource() instanceof SecuredFeatureTypeInfo);
        assertSame(ro.policy, ((SecuredFeatureTypeInfo) ro.getResource()).policy);
    }

    @Test
    public void testSecuredLayerInfoCoverages() {
        SecuredLayerInfo ro = new SecuredLayerInfo(arcGridLayer, WrapperPolicy.hide(null));

        assertFalse(arcGridLayer.getResource() instanceof SecuredCoverageInfo);
        assertTrue(ro.getResource() instanceof SecuredCoverageInfo);
        assertSame(ro.policy, ((SecuredCoverageInfo) ro.getResource()).policy);
    }

    @Test
    public void testSecuredFeatureTypeInfoHide() throws Exception {
        SecuredFeatureTypeInfo ro = new SecuredFeatureTypeInfo(states, WrapperPolicy.hide(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(null, null);
        assertEquals(SecuredFeatureSource.class, fs.getClass());
        assertTrue(fs.policy.isHide());
        SecuredDataStoreInfo store = (SecuredDataStoreInfo) ro.getStore();
        assertTrue(store.policy.isHide());
    }

    @Test
    public void testSecuredFeatureTypeInfoMetadata() throws Exception {
        SecuredFeatureTypeInfo ro =
                new SecuredFeatureTypeInfo(states, WrapperPolicy.metadata(null));
        try {
            ro.getFeatureSource(null, null);
            fail("This should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        SecuredDataStoreInfo store = (SecuredDataStoreInfo) ro.getStore();
        assertTrue(store.policy.isMetadata());
    }

    @Test
    public void testSecuredTypeInfoReadOnly() throws Exception {
        SecuredFeatureTypeInfo ro =
                new SecuredFeatureTypeInfo(states, WrapperPolicy.readOnlyChallenge(null));
        SecuredFeatureStore fs = (SecuredFeatureStore) ro.getFeatureSource(null, null);
        assertTrue(fs.policy.isReadOnlyChallenge());
        SecuredDataStoreInfo store = (SecuredDataStoreInfo) ro.getStore();
        assertTrue(store.policy.isReadOnlyChallenge());
    }

    @Test
    public void testSecuredDataStoreInfoHide() throws Exception {
        SecuredDataStoreInfo ro = new SecuredDataStoreInfo(statesStore, WrapperPolicy.hide(null));
        ReadOnlyDataStore dataStore = (ReadOnlyDataStore) ro.getDataStore(null);
        assertTrue(dataStore.policy.isHide());
    }

    @Test
    public void testSecuredDataStoreInfoMetadata() throws Exception {
        SecuredDataStoreInfo ro =
                new SecuredDataStoreInfo(statesStore, WrapperPolicy.metadata(null));
        try {
            ro.getDataStore(null);
            fail("This should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
    }
}
