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

package org.geogig.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.util.HashSet;
import org.geogig.geoserver.HeapResourceStore;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Integration test suite that simulates a clustered setup where two {@link RepositoryManager}/
 * {@link ConfigStore} combos work against the same {@link ResourceStore}, making sure events issued
 * by the {@code ResourceStore} are properly handled both by the node that triggered it and the one
 * that didn't.
 */
public class RepositoryManagerConfigStoreIntegrationTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    private ResourceStore dataDir;

    /**
     * Do not access directly but through {@link #store1()}/ {@link #store2()}. They're lazily
     * created to let test cases decide when they get initialized
     */
    private ConfigStore _store1, _store2;

    /**
     * Do not access directly but through {@link #repoManager1()}/ {@link #repoManager2()}. They're
     * lazily created to let test cases decide when they get initialized. Calling these methods
     * imply the corresponding {@link #store1()} or {@link #store2()}
     */
    private RepositoryManager _repoManager1, _repoManager2;

    private RepositoryInfo repo1;

    private RepositoryInfo repo2;

    private RepositoryInfo repo3;

    private RepositoryInfo repo4;

    @Before
    public void before() {
        dataDir = new HeapResourceStore();

        repo1 = dummy(1);
        repo2 = dummy(2);
        repo3 = dummy(3);
        repo4 = dummy(4);
    }

    private ConfigStore store1() {
        if (_store1 == null) {
            _store1 = new ConfigStore(dataDir);
        }
        return _store1;
    }

    private ConfigStore store2() {
        if (_store2 == null) {
            _store2 = new ConfigStore(dataDir);
        }
        return _store2;
    }

    private RepositoryManager repoManager1() {
        if (_repoManager1 == null) {
            _repoManager1 = spy(new RepositoryManager());
            _repoManager1.init(store1(), dataDir);
            _repoManager1.setCatalog(mock(Catalog.class, RETURNS_DEEP_STUBS));
        }
        return _repoManager1;
    }

    private RepositoryManager repoManager2() {
        if (_repoManager2 == null) {
            _repoManager2 = spy(new RepositoryManager());
            _repoManager2.init(store2(), dataDir);
            _repoManager2.setCatalog(mock(Catalog.class, RETURNS_DEEP_STUBS));
        }
        return _repoManager2;
    }

    private RepositoryInfo dummy(int i) {
        Preconditions.checkArgument(i > -1 && i < 10);
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        final String id = dummyId.substring(0, dummyId.length() - 1) + String.valueOf(i);
        RepositoryInfo info = new RepositoryInfo();
        info.setId(id);
        info.setLocation(URI.create("file:/parent/directory/" + i + "/name-" + i));
        return info;
    }

    /**
     * repos added to {@link #store1()} are immediately available on {@link #store2()} when it
     * "joins the cluster"
     */
    public @Test void testJoinCluster() {
        ConfigStore store1 = store1();

        store1.save(repo1.clone());
        store1.save(repo2.clone());
        store1.save(repo3.clone());
        store1.save(repo4.clone());

        ImmutableSet<RepositoryInfo> expected = ImmutableSet.of(repo1, repo2, repo3, repo4);
        assertEquals(expected, new HashSet<>(store1.getRepositories()));

        // "join cluster" since it's lazily created
        ConfigStore store2 = store2();
        assertEquals(expected, new HashSet<>(store2.getRepositories()));
    }

    /**
     * both {@link #store1()} and {@link #store2()} are in the cluster, repos added to #1 get
     * reflected in #2
     */
    public @Test void verifyResourceCreateEvents() {
        ConfigStore store1 = store1();
        ConfigStore store2 = store2();

        store1.save(repo1.clone());
        store1.save(repo2.clone());
        store1.save(repo3.clone());
        store1.save(repo4.clone());

        assertEquals(4, store1.getRepositories().size());
        ImmutableSet<RepositoryInfo> expected = ImmutableSet.of(repo1, repo2, repo3, repo4);
        assertEquals(expected, new HashSet<>(store1.getRepositories()));
        assertEquals(4, store2.getRepositories().size());
        assertEquals(expected, new HashSet<>(store2.getRepositories()));
    }

    /**
     * both {@link #store1()} and {@link #store2()} are in the cluster, repos changed on #1 get
     * reflected in #2
     */
    public @Test void verifyResourceEditEvents() {
        ConfigStore store1 = store1();
        ConfigStore store2 = store2();

        store1.save(repo1.clone());
        store1.save(repo2.clone());

        // preflight asserts
        ImmutableSet<RepositoryInfo> expected = ImmutableSet.of(repo1, repo2);
        assertEquals(expected, new HashSet<>(store1.getRepositories()));
        assertEquals(expected, new HashSet<>(store2.getRepositories()));

        URI uri1 = URI.create(repo1.getLocation().toString() + "-changed");

        // make a change to #1
        repo1.setLocation(uri1);
        assertNotEquals(repo1, store1.get(repo1.getId()));
        assertNotEquals(repo1, store2.get(repo1.getId()));

        RepositoryManager repoManager2 = repoManager2();

        store1.save(repo1);

        assertEquals(repo1, store1.get(repo1.getId()));
        assertEquals(repo1, store2.get(repo1.getId()));

        verify(repoManager2, times(1)).invalidate(eq(repo1.getId()));
    }

    /**
     * both {@link #store1()} and {@link #store2()} are in the cluster, repos removed on #2 get
     * reflected in #1
     */
    public @Test void verifyResourceRemovedEvents() {
        ConfigStore store1 = store1();
        ConfigStore store2 = store2();

        store1.save(repo1.clone());
        store1.save(repo2.clone());

        store2.save(repo3.clone());
        store2.save(repo4.clone());

        // preflight asserts
        ImmutableSet<RepositoryInfo> expected = ImmutableSet.of(repo1, repo2, repo3, repo4);
        assertEquals(expected, new HashSet<>(store1.getRepositories()));
        assertEquals(expected, new HashSet<>(store2.getRepositories()));

        RepositoryManager repoManager1 = repoManager1();
        RepositoryManager repoManager2 = repoManager2();

        repoManager1.delete(repo1.getId());
        verify(repoManager2, times(1)).invalidate(eq(repo1.getId()));

        repoManager2.delete(repo2.getId());
        verify(repoManager1, times(1)).invalidate(eq(repo2.getId()));
    }
}
