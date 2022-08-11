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

package org.geoserver.taskmanager.external.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.util.SecuredImpl;
import org.geotools.util.logging.Logging;
import org.springframework.web.context.ServletContextAware;

/**
 * Local file storage. All actions are relative to the rootFolder. If the data directory is
 * configured the root folder is placed in the data directory.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class FileServiceImpl extends SecuredImpl implements FileService, ServletContextAware {

    private static final long serialVersionUID = -1948411877746516243L;

    private static final Logger LOGGER = Logging.getLogger(FileServiceImpl.class);

    private Path dataDirectory;

    private Path rootFolder;

    private String description;

    private String prepareScript;

    @Override
    public String getDescription() {
        return "Local File System: " + (description == null ? description : getName());
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrepareScript() {
        return prepareScript;
    }

    public void setPrepareScript(String prepareScript) {
        this.prepareScript = prepareScript;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = Paths.get(rootFolder);
    }

    @Override
    public String getRootFolder() {
        return rootFolder.toString();
    }

    @Override
    public boolean checkFileExists(String filePath) {
        return Files.exists(getAbsolutePath(filePath));
    }

    @Override
    public void create(String filePath, InputStream content, boolean doPrepare) throws IOException {
        // Check parameters
        if (content == null) {
            throw new IllegalArgumentException("Content of a file can not be null.");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("Name of a file can not be null.");
        }
        if (checkFileExists(filePath)) {
            throw new IllegalArgumentException("The file already exists");
        }

        File targetFile = new File(getAbsolutePath(filePath).toUri());
        FileUtils.copyInputStreamToFile(content, targetFile);

        if (doPrepare && prepareScript != null) {
            Process p =
                    Runtime.getRuntime().exec(prepareScript + " " + targetFile.getAbsolutePath());
            LOGGER.info(new String(IOUtils.toByteArray(p.getInputStream())));
            LOGGER.warning(new String(IOUtils.toByteArray(p.getErrorStream())));
            try {
                int e = p.waitFor();
                if (e != 0) {
                    throw new IOException("Preparation script ended with exit code " + e);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public boolean delete(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("Name of a filePath can not be null.");
        }
        if (checkFileExists(filePath)) {
            File file = new File(getAbsolutePath(filePath).toUri());
            return file.delete();
        } else {
            return false;
        }
    }

    @Override
    public InputStream read(String filePath) throws IOException {
        if (checkFileExists(filePath)) {
            File file = new File(getAbsolutePath(filePath).toUri());
            return FileUtils.openInputStream(file);
        } else {
            throw new IOException("The file does not exist:" + filePath);
        }
    }

    @Override
    public List<String> listSubfolders() {
        File file = new File(rootFolder.toUri());
        file.mkdirs();
        ArrayList<String> paths = listFolders(file.toURI(), file);
        return paths;
    }

    private ArrayList<String> listFolders(URI rootfolder, File file) {
        String[] folders = file.list(FileFilterUtils.directoryFileFilter());
        ArrayList<String> paths = new ArrayList<>();
        if (folders != null) {
            for (String folder : folders) {
                paths.add(
                        Paths.get(rootfolder)
                                .relativize(Paths.get(file.toString(), folder))
                                .toString());
                paths.addAll(
                        listFolders(
                                rootfolder, new File(Paths.get(file.toString(), folder).toUri())));
            }
        }
        return paths;
    }

    @Override
    public FileReference getVersioned(String filePath) {
        if (filePath.indexOf(FileService.PLACEHOLDER_VERSION) < 0) {
            return new FileReferenceImpl(this, filePath, filePath);
        }

        Path parent = getAbsolutePath(filePath).getParent();
        String[] fileNames =
                parent.toFile()
                        .list(
                                new WildcardFileFilter(
                                        filePath.replace(FileService.PLACEHOLDER_VERSION, "*")));

        SortedSet<Integer> set = new TreeSet<Integer>();
        Pattern pattern =
                Pattern.compile(
                        Pattern.quote(filePath)
                                .replace(FileService.PLACEHOLDER_VERSION, "\\E(.*)\\Q"));
        for (String fileName : fileNames) {
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()) {
                try {
                    set.add(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "could not parse version in versioned file " + fileName,
                            e);
                }
            } else {
                LOGGER.log(
                        Level.WARNING,
                        "this shouldn't happen: couldn't find version in versioned file "
                                + fileName);
            }
        }
        int last = set.isEmpty() ? 0 : set.last();
        return new FileReferenceImpl(
                this,
                filePath.replace(FileService.PLACEHOLDER_VERSION, last + ""),
                filePath.replace(FileService.PLACEHOLDER_VERSION, (last + 1) + ""));
    }

    @Override
    public URI getURI(String filePath) {
        Path absolutePath = getAbsolutePath(filePath);
        if (dataDirectory != null && absolutePath.startsWith(dataDirectory)) {
            return toURI(dataDirectory.relativize(absolutePath));
        } else {
            return absolutePath.toUri();
        }
    }

    private static URI toURI(Path path) {
        try {
            return new URI(
                    "file:" + URLEncoder.encode(path.toString(), "UTF-8").replaceAll("%2F", "/"));
        } catch (UnsupportedEncodingException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path getAbsolutePath(String file) {
        if (rootFolder == null) {
            throw new IllegalStateException(
                    "No rootFolder is not configured in this file service.");
        }
        return rootFolder.resolve(Paths.get(file));
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        String dataDirectory = GeoServerResourceLoader.lookupGeoServerDataDirectory(servletContext);
        if (dataDirectory != null) {
            this.dataDirectory = Paths.get(dataDirectory);
        } else {
            throw new IllegalStateException("Unable to determine data directory");
        }
    }
}
