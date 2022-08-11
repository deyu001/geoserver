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

import static org.geoserver.platform.resource.Paths.extension;
import static org.geoserver.platform.resource.Paths.name;
import static org.geoserver.platform.resource.Paths.names;
import static org.geoserver.platform.resource.Paths.parent;
import static org.geoserver.platform.resource.Paths.path;
import static org.geoserver.platform.resource.Paths.sidecar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import org.junit.Test;

public class PathsTest {

    final String BASE = "";

    final String DIRECTORY = "directory";

    final String FILE = "directory/file.txt";

    final String SIDECAR = "directory/file.prj";

    final String FILE2 = "directory/file2.txt";

    final String SUBFOLDER = "directory/folder";

    final String FILE3 = "directory/folder/file3.txt";

    @Test
    public void pathTest() {
        assertEquals(2, names("a/b").size());
        assertEquals(1, names("a/").size());
        assertEquals(1, names("a").size());
        assertEquals(0, names("").size());

        assertEquals(BASE, path(""));
        assertEquals("directory/file.txt", path("directory", "file.txt"));
        assertEquals("directory/folder/file3.txt", path("directory/folder", "file3.txt"));

        // handling invalid values
        assertNull(path((String) null)); // edge case
        assertEquals("foo", path("foo/"));

        try {
            assertEquals("foo", path(".", "foo"));
            fail(". invalid relative path");
        } catch (IllegalArgumentException expected) {
        }

        try {
            assertEquals("foo", path("foo/bar", ".."));
            fail(".. invalid relative path");
        } catch (IllegalArgumentException expected) {
        }

        // test path elements that are always valid regardless of strictPath
        for (String name : new String[] {"foo", "foo.txt", "directory/bar"}) {
            assertEquals(name, Paths.path(true, name));
            assertEquals(name, Paths.path(false, name));
        }
        // test path elements that are always invalid regardless of strictPath
        for (String name : new String[] {".", "..", "foo\\"}) {
            try {
                assertEquals(name, Paths.path(true, name));
                fail("invalid: " + name);
            } catch (IllegalArgumentException expected) {
                // ignore
            }
            try {
                assertEquals(name, Paths.path(false, name));
                fail("invalid: " + name);
            } catch (IllegalArgumentException expected) {
                // ignore
            }
        }
        // test path elements that are invalid if and only if strictPath is true
        for (char c : "*:,'&?\"<>|".toCharArray()) {
            for (String prefix : new String[] {"foo", ""}) {
                for (String suffix : new String[] {"bar", ""}) {
                    String name = prefix + c + suffix;
                    try {
                        assertEquals(name, Paths.path(true, name));
                        fail("invalid: " + name);
                    } catch (IllegalArgumentException expected) {
                        // ignore
                    }
                    assertEquals(name, Paths.path(false, name));
                }
            }
        }
    }

    @Test
    public void validTest() {
        // test path elements that are always valid regardless of strictPath
        for (String name : new String[] {"foo", "foo.txt", "directory/bar"}) {
            assertEquals(name, Paths.valid(true, name));
            assertEquals(name, Paths.valid(false, name));
        }
        // test path elements that are always invalid regardless of strictPath
        for (String name : new String[] {".", "..", "foo\\"}) {
            try {
                assertEquals(name, Paths.valid(true, name));
                fail("invalid: " + name);
            } catch (IllegalArgumentException expected) {
                // ignore
            }
            try {
                assertEquals(name, Paths.valid(false, name));
                fail("invalid: " + name);
            } catch (IllegalArgumentException expected) {
                // ignore
            }
        }
        // test path elements that are invalid if and only if strictPath is true
        for (char c : "*:,'&?\"<>|".toCharArray()) {
            for (String prefix : new String[] {"foo", ""}) {
                for (String suffix : new String[] {"bar", ""}) {
                    String name = prefix + c + suffix;
                    try {
                        assertEquals(name, Paths.valid(true, name));
                        fail("invalid: " + name);
                    } catch (IllegalArgumentException expected) {
                        // ignore
                    }
                    assertEquals(name, Paths.valid(false, name));
                }
            }
        }
    }

    @Test
    public void parentTest() {
        assertEquals(DIRECTORY, parent(FILE));
        assertEquals(BASE, parent(DIRECTORY));
        assertNull(parent(BASE));

        // handling invalid values
        assertNull(null, parent(null));
        assertEquals("foo", parent("foo/"));
    }

    @Test
    public void naming() {
        assertEquals("file.txt", name("directory/file.txt"));
        assertEquals("txt", extension("directory/file.txt"));

        assertEquals("directory/file.txt", sidecar("directory/file", "txt"));
        assertEquals("directory/file.prj", sidecar("directory/file.txt", "prj"));
    }

    @Test
    public void convert1() {
        File folder = new File("folder");
        File file1 = new File("file1");
        File file2 = new File(folder, "file2");

        assertEquals("folder", Paths.convert(folder.getPath()));
        assertEquals("folder/file2", Paths.convert(file2.getPath()));
        assertEquals("file1", Paths.convert(file1.getPath()));
    }

    @Test
    public void convert2() {
        File home = new File(System.getProperty("user.home"));
        File directory = new File(home, "directory");
        File folder = new File(directory, "folder");
        File file1 = new File(directory, "file1");
        File file2 = new File(folder, "file2");
        File relative = new File(new File(".."), "file1");

        assertEquals("folder", Paths.convert(directory, folder));
        assertEquals("folder/file2", Paths.convert(directory, file2));
        assertEquals("file1", Paths.convert(directory, file1));

        String relativePath = relative.getPath();
        assertEquals("file1", Paths.convert(directory, folder, relativePath));
    }
}
