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

package org.geoserver.taskmanager.external;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.external.impl.FileServiceImpl;
import org.geoserver.taskmanager.util.LookupService;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test data methods.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class FileServiceDataTest extends AbstractTaskManagerTest {

    @Autowired LookupService<FileService> fileServiceRegistry;

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testFileRegistry() {
        Assert.assertEquals(2, fileServiceRegistry.names().size());

        FileService fs = fileServiceRegistry.get("temp-directory");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof FileServiceImpl);
        Assert.assertEquals("/tmp", ((FileServiceImpl) fs).getRootFolder());
    }

    @Test
    public void testFileService() throws IOException {
        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(FileUtils.getTempDirectoryPath());

        String filename = System.currentTimeMillis() + "-test.txt";
        String path = FileUtils.getTempDirectoryPath() + "/" + filename;

        Assert.assertFalse(Files.exists(Paths.get(path)));
        String content = "test the file service";
        service.create(filename, IOUtils.toInputStream(content, "UTF-8"));
        Assert.assertTrue(Files.exists(Paths.get(path)));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        String actualContent = IOUtils.toString(service.read(filename), "UTF-8");
        Assert.assertEquals(content, actualContent);

        service.delete(filename);
        Assert.assertFalse(Files.exists(Paths.get(path)));
    }

    @Test
    public void testFileServicePrepare() throws IOException, InterruptedException {
        // this test only works in linux because it uses a linux script
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);

        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(FileUtils.getTempDirectoryPath());

        // create the script and make executable
        File scriptFile = new File(tempFolder.getRoot(), "prepare.sh");
        try (OutputStream out = new FileOutputStream(scriptFile)) {
            IOUtils.copy(FileServiceDataTest.class.getResourceAsStream("prepare.sh"), out);
        }
        Process p = Runtime.getRuntime().exec("chmod u+x " + scriptFile.getAbsolutePath());
        p.waitFor();
        service.setPrepareScript(scriptFile.getAbsolutePath());

        String filename = System.currentTimeMillis() + "-test.txt";
        String path = FileUtils.getTempDirectoryPath() + "/" + filename;

        Assert.assertFalse(Files.exists(Paths.get(path)));
        String content = "test the file service";
        service.create(filename, IOUtils.toInputStream(content, "UTF-8"), true);
        Assert.assertTrue(Files.exists(Paths.get(path)));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        String actualContent = IOUtils.toString(service.read(filename), "UTF-8");
        // verify extra text!
        Assert.assertEquals(content + "extra text\n", actualContent);

        service.delete(filename);
        Assert.assertFalse(Files.exists(Paths.get(path)));
    }

    @Test
    public void testFileServiceCreateSubFolders() throws IOException {
        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(FileUtils.getTempDirectoryPath() + "/fileservicetest/");

        String filename = "subfolder/" + System.currentTimeMillis() + "-test.txt";
        String path = FileUtils.getTempDirectoryPath() + "/fileservicetest/" + filename;

        Assert.assertFalse(Files.exists(Paths.get(path)));
        service.create(filename, IOUtils.toInputStream("test the file service", "UTF-8"));
        Assert.assertTrue(Files.exists(Paths.get(path)));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        service.delete(filename);
        Assert.assertFalse(Files.exists(Paths.get(path)));

        List<String> folders = service.listSubfolders();
        Assert.assertEquals(1, folders.size());
    }

    @Test
    public void testFileServiceGetVersioned() throws IOException {
        new FileOutputStream(new File(FileUtils.getTempDirectoryPath(), "test.6.txt")).close();

        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(FileUtils.getTempDirectoryPath());

        FileReference ref = service.getVersioned("test.###.txt");
        assertEquals("test.6.txt", ref.getLatestVersion());
        assertEquals("test.7.txt", ref.getNextVersion());
    }

    @Test
    public void testListSubFolders() throws IOException {
        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(
                FileUtils.getTempDirectoryPath() + "/folder-" + System.currentTimeMillis() + "/");

        InputStream content = IOUtils.toInputStream("test the file service", "UTF-8");

        service.create("foo/a.txt", content);
        service.create("foo/bar/b.txt", content);
        service.create("foo/bar/foobar/barfoo/c.txt", content);
        service.create("hello/d.txt", content);
        service.create("e.txt", content);
        service.create("f.txt", content);

        List<String> folders = service.listSubfolders();

        Assert.assertEquals(5, folders.size());
        Assert.assertTrue(folders.contains("foo"));
        Assert.assertTrue(folders.contains(Paths.get("foo", "bar").toString()));
        Assert.assertTrue(folders.contains(Paths.get("foo", "bar", "foobar").toString()));
        Assert.assertTrue(folders.contains(Paths.get("foo", "bar", "foobar", "barfoo").toString()));
        Assert.assertTrue(folders.contains("hello"));
    }

    @Test
    public void testVersionedPath() {
        assertEquals("myfile.###", FileService.versioned("myfile"));
        assertEquals("myfile.###.ext", FileService.versioned("myfile.ext"));
        assertEquals("/path/to/myfile.###.ext", FileService.versioned("/path/to/myfile.ext"));
        assertEquals("my###file.ext", FileService.versioned("my###file.ext"));
    }
}
