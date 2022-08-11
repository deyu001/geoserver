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

package org.geoserver.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matchers;
import org.junit.Before;

/** Contains utility methods useful for testing isolated workspaces. */
public abstract class IsolatedWorkspacesTest extends GeoServerSystemTestSupport {

    // store the name of the workspaces created during a test execution
    protected final List<String> CREATED_WORKSPACES_PREFIXES = new ArrayList<>();

    @Before
    public void beforeTest() {
        // delete create workspaces
        CREATED_WORKSPACES_PREFIXES.forEach(this::removeWorkspace);
        // make sure that no local workspace is set
        LocalWorkspace.set(null);
    }

    /**
     * Helper method that updates the isolation state of an workspace and the corresponding
     * namespace.
     */
    protected void updateWorkspaceIsolationState(String prefix, boolean isolated) {
        Catalog catalog = getCatalog();
        // set the workspace isolation state using the provided one
        WorkspaceInfo workspace = catalog.getWorkspaceByName(prefix);
        workspace.setIsolated(isolated);
        catalog.save(workspace);
        // set the namespace isolation state using the provided one
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(prefix);
        namespace.setIsolated(isolated);
        catalog.save(namespace);
    }

    /** Helper method that checks that the provided workspace has the expected content. */
    protected void checkWorkspace(
            WorkspaceInfo workspace, String expectedPrefix, boolean expectedIsolation) {
        assertThat(workspace, notNullValue());
        assertThat(workspace.getName(), is(expectedPrefix));
        assertThat(workspace.isIsolated(), is(expectedIsolation));
    }

    /** Helper method that checks that the provided namespace has the expected content. */
    protected void checkNamespace(
            NamespaceInfo namespace,
            String expectedPrefix,
            String expectedNamespaceUri,
            boolean expectedIsolation) {
        assertThat(namespace, notNullValue());
        assertThat(namespace.getPrefix(), is(expectedPrefix));
        assertThat(namespace.getName(), is(expectedPrefix));
        assertThat(namespace.getURI(), is(expectedNamespaceUri));
        assertThat(namespace.isIsolated(), is(expectedIsolation));
    }

    /**
     * Helper functional interface to allow passing functions that don't receive anything as input
     * and don't provide anything as output.
     */
    @FunctionalInterface
    protected interface Statement {

        void execute();
    }

    /**
     * Helper method that executes a statement where an exception of a certain type is expected to
     * happen. This method will check hat the obtained exception contains the expected message and
     * is an instance expected type.
     */
    protected void executeAndValidateException(
            Statement statement, Class<?> expectedException, String expectedMessage) {
        boolean exceptionHappen = false;
        try {
            // execute the statement
            statement.execute();
        } catch (Exception exception) {
            // check that obtained exception matches the expectations
            assertThat(exception, instanceOf(expectedException));
            assertThat(exception.getMessage(), Matchers.containsString(expectedMessage));
            exceptionHappen = true;
        }
        // check that an exception actually happen
        assertThat(exceptionHappen, is(true));
    }

    /**
     * Helper method that creates a workspace and add it to the catalog. This method will first
     * create the namespace and then the workspace. The create workspaces prefixes are stored in
     * {@link #CREATED_WORKSPACES_PREFIXES}.
     */
    protected void createWorkspace(String prefix, String namespaceUri, boolean isolated) {
        Catalog catalog = getCatalog();
        // create the namespace
        NamespaceInfoImpl namespace = new NamespaceInfoImpl();
        namespace.setPrefix(prefix);
        namespace.setURI(namespaceUri);
        namespace.setIsolated(isolated);
        catalog.add(namespace);
        // create the workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName(prefix);
        workspace.setIsolated(isolated);
        catalog.add(workspace);
        // store created workspace
        CREATED_WORKSPACES_PREFIXES.add(prefix);
    }
}
