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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.geoserver.security.CatalogMode;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public class ReadOnlyDataStoreTest extends SecureObjectsTest {

    private DataStore ds;

    @Before
    public void setUp() throws Exception {
        SimpleFeatureStore fs = createNiceMock(SimpleFeatureStore.class);
        expect(fs.getSchema())
                .andReturn(DataUtilities.createType("test", "g:Polygon,name:String"))
                .anyTimes();
        expect(fs.getFeatures(Query.ALL)).andReturn(new DefaultFeatureCollection());
        replay(fs);
        ds = createNiceMock(DataStore.class);
        expect(ds.getFeatureSource("blah")).andReturn(fs);
        replay(ds);
    }

    @Test
    public void testDisallowedAPI() throws Exception {
        ReadOnlyDataStore ro = new ReadOnlyDataStore(ds, WrapperPolicy.hide(null));

        try {
            ro.createSchema(null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
            //
        }
        try {
            ro.updateSchema((String) null, null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
            //
        }

        try {
            ro.updateSchema((Name) null, null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
            //
        }
        try {
            ro.getFeatureWriter("states", Transaction.AUTO_COMMIT);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
            //
        }
        try {
            ro.getFeatureWriter("states", Filter.INCLUDE, Transaction.AUTO_COMMIT);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
            //
        }
        try {
            ro.getFeatureWriterAppend("states", Transaction.AUTO_COMMIT);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
            //
        }
    }

    @Test
    public void testChallenge() throws Exception {
        ReadOnlyDataStore ro = new ReadOnlyDataStore(ds, WrapperPolicy.readOnlyChallenge(null));

        try {
            ro.createSchema(null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.updateSchema((String) null, null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }

        try {
            ro.updateSchema((Name) null, null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.getFeatureWriter("states", Transaction.AUTO_COMMIT);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.getFeatureWriter("states", Filter.INCLUDE, Transaction.AUTO_COMMIT);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.getFeatureWriterAppend("states", Transaction.AUTO_COMMIT);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
    }

    @Test
    public void testReadOnlySource() throws Exception {
        ReadOnlyDataStore ro =
                new ReadOnlyDataStore(
                        ds,
                        WrapperPolicy.readOnlyHide(
                                new WorkspaceAccessLimits(CatalogMode.HIDE, true, false, false)));
        SimpleFeatureSource fs = ro.getFeatureSource("blah");
        // used to go boom here
        SimpleFeatureCollection fc = fs.getFeatures(Query.ALL);
        assertEquals(0, fc.size());
    }

    public static boolean isSpringSecurityException(Exception ex) {
        return SecurityUtils.isSecurityException(ex);
    }
}
