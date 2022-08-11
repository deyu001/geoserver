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

package org.geoserver.ows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Iterators;
import java.util.Collections;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.CatalogFilterAccessManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.AbstractAuthorizationTest;
import org.geoserver.util.PropertyRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

public class LocalWorkspaceSecureCatalogTest extends AbstractAuthorizationTest {

    @Rule
    public PropertyRule inheritance = PropertyRule.system("GEOSERVER_GLOBAL_LAYER_GROUP_INHERIT");

    @Before
    public void setUp() throws Exception {
        LocalWorkspaceCatalogFilter.groupInherit = null;
        super.setUp();
        populateCatalog();
    }

    CatalogFilterAccessManager setupAccessManager() throws Exception {
        ResourceAccessManager defAsResourceManager = buildAccessManager("wideOpen.properties");
        CatalogFilterAccessManager mgr = new CatalogFilterAccessManager();
        mgr.setCatalogFilters(Collections.singletonList(new LocalWorkspaceCatalogFilter(catalog)));
        mgr.setDelegate(defAsResourceManager);
        return mgr;
    }

    @Test
    public void testAccessToLayer() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertNotNull(sc.getLayerByName("topp:states"));

        WorkspaceInfo ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertNull(sc.getWorkspaceByName("topp"));
        assertNull(sc.getResourceByName("topp:states", ResourceInfo.class));
        assertNull(sc.getLayerByName("topp:states"));
    }

    @Test
    public void testAccessToStyle() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertEquals(2, sc.getStyles().size());

        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertEquals(2, sc.getStyles().size());
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertEquals(1, sc.getStyles().size());
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testAccessToLayerGroup() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertEquals(catalog.getLayerGroups().size(), sc.getLayerGroups().size());

        // all groups in this one or global
        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertEquals(getWorkspaceAccessibleGroupSize("topp"), sc.getLayerGroups().size());
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertEquals(getWorkspaceAccessibleGroupSize("nurc"), sc.getLayerGroups().size());
        assertEquals("layerGroup", sc.getLayerGroups().get(0).getName());
        LocalWorkspace.remove();
    }

    private long getWorkspaceAccessibleGroupSize(String workspaceName) {
        return catalog.getLayerGroups()
                .stream()
                .filter(
                        lg ->
                                lg.getWorkspace() == null
                                        || workspaceName.equals(lg.getWorkspace().getName()))
                .count();
    }

    @Test
    public void testAccessToLayerGroupNoInheritance() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();
        inheritance.setValue("false");

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertThat(sc.getLayerGroups(), hasItem(equalTo(layerGroupGlobal)));
        assertThat(sc.getLayerGroups(), hasItem(equalTo(layerGroupTopp)));
        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertThat(sc.getLayerGroups(), not(hasItem(equalTo(layerGroupGlobal))));
        assertThat(sc.getLayerGroups(), hasItem(equalTo(layerGroupTopp)));
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertThat(sc.getLayerGroups(), not(hasItem(equalTo(layerGroupGlobal))));
        assertThat(sc.getLayerGroups(), not(hasItem(equalTo(layerGroupTopp))));
        LocalWorkspace.remove();
    }

    @Test
    public void testAccessToStyleAsIterator() throws Exception {
        // Getting the access manager
        CatalogFilterAccessManager mgr = setupAccessManager();

        // Defining a SecureCatalog with a user which is not admin
        SecureCatalogImpl sc =
                new SecureCatalogImpl(catalog, mgr) {
                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);

        // Get the iterator on the styles
        try (CloseableIterator<StyleInfo> styles = sc.list(StyleInfo.class, Filter.INCLUDE)) {
            int size = Iterators.size(styles);
            assertEquals(2, size);
        }

        // Setting the workspace "topp" and repeating the test
        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);

        try (CloseableIterator<StyleInfo> styles = sc.list(StyleInfo.class, Filter.INCLUDE)) {
            int size = Iterators.size(styles);
            assertEquals(2, size);
        }
        LocalWorkspace.remove();

        // Setting the workspace "nurc" and repeating the test
        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        try (CloseableIterator<StyleInfo> styles = sc.list(StyleInfo.class, Filter.INCLUDE)) {
            int size = Iterators.size(styles);
            assertEquals(1, size);
        }
    }

    @After
    public void tearDown() throws Exception {
        LocalWorkspace.remove();
    }
}
