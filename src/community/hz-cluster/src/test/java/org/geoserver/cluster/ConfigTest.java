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

package org.geoserver.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.cluster.hazelcast.HzSynchronizer;
import org.geoserver.cluster.hazelcast.HzSynchronizerTest;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author Alessio Fabiani, GeoSolutions */
public class ConfigTest extends HzSynchronizerTest {

    private Resource tmpDir;
    private Resource tmpDir1;
    private Resource tmpDir2;

    @Before
    public void createTempDirs() throws IOException {
        tmpDir = tmpDir();
        tmpDir1 = tmpDir();
        tmpDir2 = tmpDir();
    }

    @After
    public void deleteTempDirs() {
        FileUtils.deleteQuietly(tmpDir.dir());
        FileUtils.deleteQuietly(tmpDir1.dir());
        FileUtils.deleteQuietly(tmpDir2.dir());
    }

    @Test
    public void testConfigurationReload() throws IOException {
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(tmpDir.dir());
        GeoServerResourceLoader resourceLoader1 = new GeoServerResourceLoader(tmpDir1.dir());
        GeoServerResourceLoader resourceLoader2 = new GeoServerResourceLoader(tmpDir2.dir());
        Resources.directory(tmpDir.get("cluster"), true);
        Resources.directory(tmpDir1.get("cluster"), true);
        Resources.directory(tmpDir2.get("cluster"), true);

        this.cluster.setResourceStore(resourceLoader.getResourceStore());
        this.cluster.saveConfiguration(resourceLoader1);

        assertNotNull(cluster.getFileLocations());
        assertEquals(2, cluster.getFileLocations().size());

        assertTrue(
                "The file 'cluster.properties' does not exist!",
                Resources.exists(tmpDir1.get("cluster/cluster.properties")));
        assertTrue(
                "The file 'hazelcast.xml' does not exist!",
                Resources.exists(tmpDir1.get("cluster/hazelcast.xml")));

        this.cluster.saveConfiguration(resourceLoader2);

        assertTrue(
                "The file 'cluster.properties' does not exist!",
                Resources.exists(tmpDir2.get("cluster/cluster.properties")));
        assertTrue(
                "The file 'hazelcast.xml' does not exist!",
                Resources.exists(tmpDir2.get("cluster/hazelcast.xml")));

        assertEquals(
                lines(tmpDir1, "cluster/cluster.properties"),
                lines(tmpDir2, "cluster/cluster.properties"));

        assertEquals(
                lines(tmpDir1, "cluster/hazelcast.xml"), lines(tmpDir2, "cluster/hazelcast.xml"));
    }

    @Override
    protected HzSynchronizer getSynchronizer() {
        return null;
    }

    private static final List<String> lines(Resource dir, String path) throws IOException {
        try (InputStream input = dir.get(path).in()) {
            return IOUtils.readLines(input, StandardCharsets.UTF_8);
        }
    }
}
