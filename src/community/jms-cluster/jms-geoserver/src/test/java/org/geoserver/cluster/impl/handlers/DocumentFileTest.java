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

package org.geoserver.cluster.impl.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.thoughtworks.xstream.XStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DocumentFileTest {

    private File rootDirectory;
    private FileSystemResourceStore resourceStore;

    @Before
    public void before() throws Exception {
        rootDirectory = Files.createTempDirectory("jsm-test-").toFile();
        resourceStore = new FileSystemResourceStore(rootDirectory);
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(rootDirectory);
    }

    @Test
    public void testSerializeDocumentFile() throws Exception {
        // creating a style in data directory
        Resource resource = addResourceToDataDir("styles/style.sld", "some style definition");
        // instantiating a document file representing the style file
        DocumentFile documentFile = new DocumentFile(resource);
        // serialising the file document
        DocumentFileHandlerSPI handler = new DocumentFileHandlerSPI(0, new XStream());
        String result = handler.createHandler().serialize(documentFile);
        // checking the serialization result
        assertThat(result, notNullValue());
        assertThat(
                XMLUnit.compareXML(readResourceFileContent("document_file_1.xml"), result)
                        .similar(),
                is(true));
    }

    /** Creates a resource path in the data directory and write the provided content in it. */
    private Resource addResourceToDataDir(String resourcePath, String resourceContent)
            throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        File resourceFile = new File(rootDirectory, resourcePath);
        resourceFile.getParentFile().mkdirs();
        Files.write(resourceFile.toPath(), resourceContent.getBytes());
        return resourceStore.get("styles/style.sld");
    }

    /** Helper method that will read the content of a resource file and return it as a String. */
    private String readResourceFileContent(String resourceFileName) throws Exception {
        try (InputStream input =
                        DocumentFileTest.class
                                .getClassLoader()
                                .getResourceAsStream(resourceFileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
