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

package org.geoserver.web.data.store.panel;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.model.Model;
import org.geoserver.data.test.MockData;
import org.geoserver.web.util.MapModel;
import org.junit.Before;
import org.junit.Test;

public class FileModelTest {

    File root;

    @Before
    public void init() throws IOException {
        root = File.createTempFile("file", "test", new File("target"));
        root.delete();
        root.mkdirs();
    }

    @Test
    public void testAbsolute() throws IOException {
        // create test file
        File f = new File("target/fileModelTest.xml");
        try {
            f.createNewFile();

            FileModel model = new FileModel(new Model<>(), root);
            model.setObject(f.getAbsolutePath());
            String path = model.getObject();
            assertEquals("file://" + f.getAbsolutePath(), path);
        } finally {
            f.delete();
        }
    }

    @Test
    public void testAbsoluteToRelative() throws IOException {
        // pick up an existing file

        File data = new File(root, "data");
        File cite = new File(data, MockData.CITE_PREFIX);
        File buildings = new File(cite, "Buildings.properties");

        FileModel model = new FileModel(new Model<>(), root);
        model.setObject(buildings.getAbsolutePath());
        String path = model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }

    @Test
    public void testRelativeUnmodified() throws IOException {
        FileModel model = new FileModel(new Model<>(), root);
        model.setObject("file:data/cite/Buildings.properties");
        String path = model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }

    @Test
    public void testURL() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("url", new URL("file:data/cite/Buildings.properties"));
        MapModel<String> mapModel = new MapModel<>(map, "url");
        FileModel model = new FileModel(mapModel, root);
        String path = model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }
}
