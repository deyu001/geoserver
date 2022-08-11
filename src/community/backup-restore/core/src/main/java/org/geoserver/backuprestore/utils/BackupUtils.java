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

package org.geoserver.backuprestore.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.codehaus.plexus.util.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * Utility to work with compressed files
 *
 * <p>Based on Importer {@link VFSWorker} by Gabriel Roldan
 *
 * @author groldan
 * @autho Alessio Fabiani, GeoSolutions
 */
public class BackupUtils {

    private static final Logger LOGGER = Logging.getLogger(BackupUtils.class);

    /** Returns a random temp folder Resource inside the System Temp Directory. */
    public static Resource tmpDir() throws IOException {
        String tempPath = null;
        try {
            tempPath = System.getProperty("java.io.tmpdir");
            if (tempPath == null) {
                tempPath = IOUtils.createTempDirectory("backuputils").getAbsolutePath();
                LOGGER.warning(
                        "It was not possible to create a temporary folder into the System 'java.io.tmpdir'. Falling back to default TEMP ["
                                + tempPath
                                + "].");
            }
        } catch (Exception e) {
            tempPath = null;
            LOGGER.log(
                    Level.SEVERE,
                    "It was not possible to create a temporary folder! In order to fix the problem, please check the System 'java.io.tmpdir' point to a valid folder.",
                    e);
            throw new IOException(
                    "It was not possible to create a temporary folder! In order to fix the problem, please check the System 'java.io.tmpdir' point to a valid folder.",
                    e);
        }

        if (tempPath == null) {
            LOGGER.log(
                    Level.SEVERE,
                    "It was not possible to create or find a suitable temporary folder. 'tempPath' is NULL! In order to fix the problem, please check the System 'java.io.tmpdir' point to a valid folder.");
            throw new IOException(
                    "It was not possible to create or find a suitable temporary folder. 'tempPath' is NULL! In order to fix the problem, please check the System 'java.io.tmpdir' point to a valid folder.");
        }

        return createRandomResource(tempPath);
    }

    /** Returns a random temp folder Resource inside the GeoServer Temp Directory. */
    public static Resource geoServerTmpDir(GeoServerDataDirectory geoServerDataDirectory)
            throws IOException {
        String tempPath = geoServerDataDirectory.findOrCreateDir("temp").getAbsolutePath();

        return createRandomResource(tempPath);
    }

    /** */
    private static Resource createRandomResource(String tempPath) throws IOException {
        Resource root = Resources.fromPath(tempPath);
        Resource directory = Resources.createRandom("tmp", "", root);

        do {
            FileUtils.forceDelete(directory.dir());
        } while (Resources.exists(directory));

        FileUtils.forceMkdir(directory.dir());

        return Files.asResource(directory.dir());
    }

    /**
     * Extracts the archive file {@code archiveFile} to {@code targetFolder}; both shall previously
     * exist.
     */
    public static void extractTo(Resource archiveFile, Resource targetFolder) throws IOException {
        FileSystemManager manager = VFS.getManager();
        String sourceURI = resolveArchiveURI(archiveFile);

        FileObject source = manager.resolveFile(sourceURI);
        if (manager.canCreateFileSystem(source)) {
            source = manager.createFileSystem(source);
        }
        FileObject target =
                manager.createVirtualFileSystem(
                        manager.resolveFile(targetFolder.dir().getAbsolutePath()));

        FileSelector selector =
                new AllFileSelector() {
                    @Override
                    public boolean includeFile(FileSelectInfo fileInfo) {
                        LOGGER.fine(
                                "Uncompressing " + fileInfo.getFile().getName().getFriendlyURI());
                        return true;
                    }
                };
        target.copyFrom(source, selector);
        source.close();
        target.close();
        manager.closeFileSystem(source.getFileSystem());
    }

    /**
     * Compress {@code sourceFolder} to the archive file {@code archiveFile}; both shall previously
     * exist.
     */
    public static void compressTo(Resource sourceFolder, Resource archiveFile) throws IOException {
        // See https://commons.apache.org/proper/commons-vfs/filesystems.html
        // for the supported filesystems

        FileSystemManager manager = VFS.getManager();

        FileObject sourceDir =
                manager.createVirtualFileSystem(
                        manager.resolveFile(sourceFolder.dir().getAbsolutePath()));

        try {
            if ("zip".equalsIgnoreCase(FileUtils.getExtension(archiveFile.path()))) {
                // apache VFS does not support ZIP as writable FileSystem

                OutputStream fos = archiveFile.out();

                // Create access to zip.
                ZipOutputStream zos = new ZipOutputStream(fos);

                // add entry/-ies.
                for (FileObject sourceFile : sourceDir.getChildren()) {
                    writeEntry(zos, sourceFile, null);
                }

                // Close streams
                zos.flush();
                zos.close();
                fos.close();
            } else {
                // Create access to archive.
                FileObject zipFile = manager.resolveFile(resolveArchiveURI(archiveFile));
                zipFile.createFile();
                ZipOutputStream zos = new ZipOutputStream(zipFile.getContent().getOutputStream());

                // add entry/-ies.
                for (FileObject sourceFile : sourceDir.getChildren()) {
                    writeEntry(zos, sourceFile, null);
                }

                // Close streams
                zos.flush();
                zos.close();
                zipFile.close();
                manager.closeFileSystem(zipFile.getFileSystem());
            }
        } finally {
            manager.closeFileSystem(sourceDir.getFileSystem());
        }
    }

    /** */
    private static void writeEntry(ZipOutputStream zos, FileObject sourceFile, String baseDir)
            throws FileSystemException, IOException {
        if (sourceFile.getType() == FileType.FOLDER) {
            // add entry/-ies.
            for (FileObject file : sourceFile.getChildren()) {
                writeEntry(zos, file, Paths.path(baseDir, sourceFile.getName().getBaseName()));
            }
        } else {
            String fileName =
                    (baseDir != null
                            ? Paths.path(baseDir, sourceFile.getName().getBaseName())
                            : sourceFile.getName().getBaseName());
            ZipEntry zipEntry = new ZipEntry(fileName);
            InputStream is = sourceFile.getContent().getInputStream();

            // Write to zip.
            byte[] buf = new byte[1024];
            zos.putNextEntry(zipEntry);
            for (int readNum; (readNum = is.read(buf)) != -1; ) {
                zos.write(buf, 0, readNum);
            }
            zos.closeEntry();
            is.close();
        }
    }

    /** */
    public static String resolveArchiveURI(final Resource archiveFile) {
        String archivePrefix = getArchiveURLProtocol(archiveFile);
        String absolutePath = archivePrefix + archiveFile.file().getAbsolutePath();
        return absolutePath;
    }

    /** */
    public static String getArchiveURLProtocol(final Resource file) {
        if (file.getType() == Type.DIRECTORY) {
            return "file://";
        }
        String name = file.name().toLowerCase();
        if (name.endsWith(".zip") || name.endsWith(".kmz")) {
            return "zip://";
        }
        if (name.endsWith(".tar")) {
            return "tar://";
        }
        if (name.endsWith(".tgz") || name.endsWith(".tar.gz")) {
            return "tgz://";
        }
        if (name.endsWith(".tbz2") || name.endsWith(".tar.bzip2") || name.endsWith(".tar.bz2")) {
            return "tbz2://";
        }
        if (name.endsWith(".gz")) {
            return "gz://";
        }
        if (name.endsWith(".bz2")) {
            return "bz2://";
        }
        if (name.endsWith(".jar")) {
            return "jar://";
        }
        return null;
    }

    /** */
    public static Resource dir(Resource baseDir, String subDir) {
        final Resource targetPath = Resources.fromPath(subDir, baseDir);
        return Resources.fromPath(
                Resources.directory(targetPath, !Resources.exists(targetPath)).getAbsolutePath());
    }
}
