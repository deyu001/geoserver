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

package org.geoserver.test.onlineTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class StationsMappingsSetup {

    public static final String MAPPING_FILE_NAME = "mappings.xml";
    public static final String INCLUDED_FILE_NAME = "includedTypes.xml";

    public void setupMapping(
            String solrUrl, String solrCoreName, PostgresqlProperties pgProps, File testDir) {
        // replace mappings.xml placeholders
        String mappingsContent = loadFileAsString("test-data/" + MAPPING_FILE_NAME);
        mappingsContent = StringUtils.replace(mappingsContent, "${solr_url}", solrUrl);
        mappingsContent = StringUtils.replace(mappingsContent, "${solr_core}", solrCoreName);
        mappingsContent = replacePgPlaceholders(mappingsContent, pgProps);
        // replace includedTypes.xml placeholders
        String includedContent = loadFileAsString("test-data/" + INCLUDED_FILE_NAME);
        includedContent = replacePgPlaceholders(includedContent, pgProps);
        try {
            // save as mappings.xml final file
            Path path = Paths.get(testDir.getAbsolutePath(), MAPPING_FILE_NAME);
            Files.write(path, mappingsContent.getBytes());
            // save as includedTypes.xml final file
            Path itpath = Paths.get(testDir.getAbsolutePath(), INCLUDED_FILE_NAME);
            Files.write(itpath, includedContent.getBytes());
            // create app-schema-cache directory
            Path dirpath = Paths.get(testDir.getAbsolutePath(), "app-schema-cache");
            Files.createDirectories(dirpath);
            copyRelatedFiles(testDir.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String replacePgPlaceholders(String mappingsContent, PostgresqlProperties pgProps) {
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_host}", pgProps.getHost());
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_port}", pgProps.getPort());
        mappingsContent =
                StringUtils.replace(mappingsContent, "${pg_database}", pgProps.getDatabase());
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_schema}", pgProps.getSchema());
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_user}", pgProps.getUser());
        mappingsContent =
                StringUtils.replace(mappingsContent, "${pg_password}", pgProps.getPassword());
        return mappingsContent;
    }

    /** Copy remaining related files from test-data to temp test directory */
    private void copyRelatedFiles(String testDirPath) throws IOException {
        // meteo.xsd
        copyFile("meteo.xsd", testDirPath);
    }

    private void copyFile(String fileName, String testDirPath) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("test-data/" + fileName);
        Path target = Paths.get(testDirPath, fileName);
        Files.copy(in, target);
    }

    public String loadFileAsString(String uri) {
        String content = null;
        try {
            content =
                    IOUtils.toString(
                            getClass().getClassLoader().getResourceAsStream(uri),
                            StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error loading mapping file.", e);
        }
        return content;
    }
}
