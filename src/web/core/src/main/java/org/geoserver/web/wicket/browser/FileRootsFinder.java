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

package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.wicket.browser.FileRootsFinder.PathSplitter;

/**
 * Support class to locate the file system roots the file chooser uses to locate files, along with
 * utility to match file paths in said roots
 */
public class FileRootsFinder implements Serializable {

    /**
     * Utility so split and rebuild paths accounting for ResourceStore little own illusion of
     * working on a *nix file system regardless of the actual file system
     */
    static class PathSplitter {

        String separator;
        boolean dataDirectoryPath;
        String base;
        String name;

        public PathSplitter(String input, boolean dataDirectoryPath) {
            // decide which separator to use based on data dir vs actual file system
            this.separator = dataDirectoryPath ? "/" : File.separator;
            this.dataDirectoryPath = dataDirectoryPath;

            // remove protocol part if needed (we have messy inputs stored that do not always start
            // with
            // file:// but sometimes with file:/ and sometimes with file: (no / at all)
            if (input.startsWith("file:")) {
                if (input.startsWith("file:/")) {
                    if (input.startsWith("file://")) {
                        input = input.substring(7);
                    } else {
                        input = input.substring(6);
                    }
                } else {
                    input = input.substring(5);
                }
            }

            // split input into base and last segment
            int idx = input.lastIndexOf(separator);
            if (idx >= 0) {
                base = input.substring(0, idx);
                name = input.substring(idx + 1, input.length()).toLowerCase();
            } else {
                base = separator;
                name = input.toLowerCase();
            }

            // fix base in case of data dir
            if (dataDirectoryPath) {
                base = Paths.convert(base);
            }
        }

        private String buildPath(String name) {
            // Data dir relative path weirdness, the protocol has to be
            // file:/ instead of file:// or it won't work.
            String prefix = dataDirectoryPath ? "file:" : "file://";
            // make data dir relative paths actually relative despite user's input
            String localBase = base;
            if (dataDirectoryPath && localBase.startsWith(separator)) {
                localBase = base.substring(1);
            }
            if (localBase.endsWith(separator)) {
                return prefix + localBase + name;
            } else {
                return prefix + localBase + separator + name;
            }
        }
    }

    private ArrayList<File> roots;
    private File dataDirectory;

    public FileRootsFinder(boolean includeDataDir) {
        this(GeoServerFileChooser.HIDE_FS, includeDataDir);
    }

    public FileRootsFinder(boolean hideFileSystem, boolean includeDataDir) {
        // build the roots
        roots = new ArrayList<>();
        if (!hideFileSystem) {
            roots.addAll(Arrays.asList(File.listRoots()));
        }
        Collections.sort(roots);

        // TODO: find a better way to deal with the data dir
        GeoServerResourceLoader loader = getLoader();
        dataDirectory = loader.getBaseDirectory();

        if (includeDataDir) {
            roots.add(0, dataDirectory);
        }

        // add the home directory as well if it was possible to determine it at all
        if (!hideFileSystem && GeoServerFileChooser.USER_HOME != null) {
            roots.add(1, GeoServerFileChooser.USER_HOME);
        }
    }

    public ArrayList<File> getRoots() {
        return roots;
    }

    public GeoServerResourceLoader getLoader() {
        return GeoServerExtensions.bean(GeoServerResourceLoader.class);
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Support method for autocomplete text boxes, given a input and an optional file filter returns
     * an a {@link Stream} containing the actual paths matching the provided input (any
     * file/directory starting with the same path as the input and containing the file name in a
     * case insensitive way)
     *
     * @param input A partial path, can be a single name or a full path (can be relative, will
     *     be24:14 matched against the data directory)
     * @param fileFilter An optional file filter to filter the returned files. The file filter
     *     should accept directories.
     */
    @SuppressWarnings("PMD.CloseResource")
    public Stream<String> getMatches(String input, FileFilter fileFilter) {
        // check the data directory (which lives in its own *nix dream, so paths need conversion)
        PathSplitter ddSplitter = new PathSplitter(input, true);
        GeoServerResourceLoader loader = getLoader();
        Resource resource = loader.get(ddSplitter.base);
        File dataDirectoryRoot = loader.get("/").dir();
        Stream<String> result =
                resource.list()
                        .stream()
                        .filter(r -> r.name().toLowerCase().contains(ddSplitter.name))
                        .filter(
                                r ->
                                        fileFilter == null
                                                || fileFilter.accept(
                                                        new File(dataDirectoryRoot, r.path())))
                        .map(r -> ddSplitter.buildPath(r.name()));

        // check all the roots
        PathSplitter fsSplitter = new PathSplitter(input, false);
        for (File root : getRoots()) {
            String pathInRoot = fsSplitter.base;
            if (pathInRoot.startsWith(root.getPath())) {
                pathInRoot = pathInRoot.substring(root.getPath().length());
                if (pathInRoot.startsWith(File.separator)) {
                    pathInRoot = pathInRoot.substring(1);
                }
            } else {
                continue;
            }

            File searchBase = new File(root, pathInRoot);
            String[] names =
                    searchBase.list(
                            (dir, fileName) -> fileName.toLowerCase().contains(fsSplitter.name));
            if (names != null) {
                Stream<String> rootPaths =
                        Arrays.stream(names)
                                .filter(
                                        fileName ->
                                                fileFilter == null
                                                        || fileFilter.accept(
                                                                new File(
                                                                        fsSplitter.base, fileName)))
                                .map(fileName -> fsSplitter.buildPath(fileName));
                result = Stream.concat(result, rootPaths);
            }
        }

        return result.distinct().sorted();
    }
}
