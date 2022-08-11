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

package org.geoserver.platform.resource;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemResourceStoreTest {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private FileSystemResourceStore store;

    public @Before void before() {
        store = new FileSystemResourceStore(folder.getRoot());
    }

    @Test
    public void renameSameFileName() throws IOException, InterruptedException {
        String sameName = "Filea";

        attemptRenameFile(sameName, sameName);

        assertEquals(sameName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameFileNamesCaseDiffer() throws IOException, InterruptedException {
        String newName = "Filea";

        attemptRenameFile("FileA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameFileNamesDiffer() throws IOException, InterruptedException {
        String newName = "FileB";

        attemptRenameFile("FileA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameSameDirName() throws IOException, InterruptedException {
        String sameName = "Dira";

        attemptRenameDir(sameName, sameName);

        assertEquals(sameName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameDirNamesCaseDiffer() throws IOException, InterruptedException {
        String newName = "Dira";

        attemptRenameDir("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameDirNamesDiffer() throws IOException, InterruptedException {
        String newName = "DirB";

        attemptRenameDir("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    private void attemptRenameDir(String oldName, String newName) throws IOException {
        folder.newFolder(oldName);
        attemptRename(oldName, newName);
    }

    private void attemptRenameFile(String oldName, String newName) throws IOException {
        folder.newFile(oldName);
        attemptRename(oldName, newName);
    }

    private void attemptRename(String oldName, String newName) throws IOException {
        assertEquals(1, folder.getRoot().list().length);

        store.move(oldName, newName);

        assertEquals(1, folder.getRoot().list().length);
    }

    @Test
    public void testGetResourceNotificationDispatcher_AtomicLazyInitialization() {
        final int nThreads = 64;
        final int nTasks = 4 * nThreads;
        final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        try {
            Collection<Callable<FileSystemWatcher>> tasks =
                    IntStream.range(0, nTasks)
                            .mapToObj(
                                    i ->
                                            (Callable<FileSystemWatcher>)
                                                    () ->
                                                            ((FileSystemWatcher)
                                                                    store
                                                                            .getResourceNotificationDispatcher()))
                            .collect(Collectors.toList());

            List<FileSystemWatcher> watchers =
                    executorService
                            .invokeAll(tasks)
                            .stream()
                            .map(
                                    completedFuture -> {
                                        try {
                                            return completedFuture.get();
                                        } catch (InterruptedException | ExecutionException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .collect(Collectors.toList());

            assertEquals(nTasks, watchers.size());

            assertEquals(
                    "FileSystemWatcher initialization wasn't lazy and atomic",
                    1,
                    new HashSet<>(watchers).size());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdownNow();
        }
    }
}
