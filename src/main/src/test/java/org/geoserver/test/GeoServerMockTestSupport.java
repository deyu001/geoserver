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

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockCreator;
import org.geoserver.data.test.MockTestData;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;

/**
 * Base test class for GeoServer mock tests that work from mocked up configuration.
 *
 * <h2>Test Setup Frequency</h2>
 *
 * <p>By default the setup cycle is executed once for extensions of this class. Subclasses that
 * require a different test setup frequency should annotate themselves with the appropriate {@link
 * TestSetup} annotation. For example to implement a repeated setup: <code><pre>
 *  {@literal @}TestSetup(run=TestSetupFrequency.REPEATED}
 *  public class MyTest extends GeoServerMockTestSupport {
 *
 *  }
 * </pre></code> *
 *
 * <h2>Mock Customization</h2>
 *
 * <p>Subclasses extending this base class may customize the mock setup by setting a custom {@link
 * MockCreator} object to {@link #setMockCreator(MockCreator)}. Tests that utilize the one time
 * setup (which is the default for this class) may call this method from the {@link
 * GeoServerBaseTestSupport#setUp(TestData)} hook. For test classes requiring per test case mock
 * customization this method should be called from the test method itself, but the test class must
 * declare a setup frequency of {@link TestSetupFrequency#REPEAT}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerMockTestSupport extends GeoServerBaseTestSupport<MockTestData> {

    @Override
    protected MockTestData createTestData() throws Exception {
        return new MockTestData();
    }

    public Catalog getCatalog() {
        return getTestData().getCatalog();
    }

    public GeoServerSecurityManager getSecurityManager() {
        return getTestData().getSecurityManager();
    }

    /** Accessor for plain text password encoder. */
    protected GeoServerPlainTextPasswordEncoder getPlainTextPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class);
    }

    /** Accessor for digest password encoder. */
    protected GeoServerDigestPasswordEncoder getDigestPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
    }

    /** Accessor for regular (weak encryption) pbe password encoder. */
    protected GeoServerPBEPasswordEncoder getPBEPasswordEncoder() {
        return getSecurityManager()
                .loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false);
    }

    /** Accessor for strong encryption pbe password encoder. */
    protected GeoServerPBEPasswordEncoder getStrongPBEPasswordEncoder() {
        return getSecurityManager()
                .loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, true);
    }

    /** Forwards through to {@link MockTestData#setMockCreator(MockCreator)} */
    protected void setMockCreator(MockCreator mockCreator) {
        getTestData().setMockCreator(mockCreator);
    }
}
