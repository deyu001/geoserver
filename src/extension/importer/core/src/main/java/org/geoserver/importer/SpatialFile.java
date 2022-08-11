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

package org.geoserver.importer;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.IOUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SpatialFile extends FileData {

    private static final long serialVersionUID = -280215815681792790L;

    static EPSGCodeLookupCache EPSG_LOOKUP_CACHE = new EPSGCodeLookupCache();

    /** .prj file */
    File prjFile;

    /** style file */
    File styleFile;

    /** supplementary files, like indexes, etc... */
    List<File> suppFiles = new ArrayList<>();

    /**
     * Create from file system
     *
     * @param file the spatial file
     */
    public SpatialFile(File file) {
        super(file);
    }

    public SpatialFile(SpatialFile other) {
        super(other);
        this.prjFile = other.getPrjFile();
        this.suppFiles.addAll(other.getSuppFiles());
    }

    public File getPrjFile() {
        return prjFile;
    }

    public void setPrjFile(File prjFile) {
        this.prjFile = prjFile;
    }

    public List<File> getSuppFiles() {
        return suppFiles;
    }

    public List<File> allFiles() {
        ArrayList<File> all = new ArrayList<>();
        all.add(file);
        if (prjFile != null) {
            all.add(prjFile);
        }
        if (styleFile != null) {
            all.add(styleFile);
        }
        all.addAll(suppFiles);
        return all;
    }

    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        // round up all the files with the same name
        suppFiles = new ArrayList<>();
        prjFile = null;
        styleFile = null;

        final List<String> styleExtensions =
                Lists.transform(
                        Styles.handlers(),
                        new Function<StyleHandler, String>() {
                            @Nullable
                            @Override
                            public String apply(@Nullable StyleHandler input) {
                                return input.getFileExtension();
                            }
                        });

        // getBaseName only gets the LAST extension so beware for .shp.aux.xml stuff
        final String baseName = getBaseName(file.getName());
        final String baseExtension = getExtension(file.getName());
        final String basePath = file.getParent();

        // look for style files
        Iterator styleExtensionsIt = styleExtensions.iterator();
        while (styleFile == null && styleExtensionsIt.hasNext()) {
            Object ext = styleExtensionsIt.next();
            File style = new File(basePath, baseName + "." + ext);
            if (style.exists()) {
                // TODO: deal with multiple style files? for now we just grab the first
                styleFile = style;
            }
        }

        // The previous version of the code was doing a File.listFiles,
        // looking for files with same name of the input file. However
        // this was resulting in very time consuming operation when importing
        // a file living into a directory containing thousands of files.
        // Especially on system doing continuous ingest of a new file every few minute
        // with a continuously growing directory.

        // Looking for supplemental files
        List<SupplementalFileExtensionsProvider> extensionsProviders =
                GeoServerExtensions.extensions(SupplementalFileExtensionsProvider.class);

        // different providers can provide same supplementalFile extension so let's use a set
        Set<File> supplementalSet = new HashSet<>();
        for (SupplementalFileExtensionsProvider provider : extensionsProviders) {
            // get extensions from each provider
            for (String extension : provider.getExtensions(baseExtension)) {
                File supplementalFile = new File(basePath, baseName + "." + extension);
                if (supplementalFile.exists()) {
                    if ("prj".equalsIgnoreCase(extension)) {
                        prjFile = supplementalFile;
                    } else {
                        supplementalSet.add(supplementalFile);
                    }
                }
            }
        }

        suppFiles = supplementalSet.stream().collect(Collectors.toList());

        if (format == null) {
            format = DataFormat.lookup(file);
        }

        // fix the prj file (match to official epsg wkt)
        try {
            fixPrjFile();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error fixing prj file", e);
        }
    }

    public void fixPrjFile() throws IOException {
        CoordinateReferenceSystem crs = readPrjToCRS();
        if (crs == null) {
            return;
        }

        try {
            CoordinateReferenceSystem epsgCrs = null;
            Integer epsgCode = EPSG_LOOKUP_CACHE.lookupEPSGCode(crs);
            if (epsgCode != null) {
                epsgCrs = CRS.decode("EPSG:" + epsgCode);
            }
            if (epsgCrs != null) {
                String epsgWKT = epsgCrs.toWKT();
                FileUtils.writeStringToFile(getPrjFile(), epsgWKT, "UTF-8");
            }
        } catch (FactoryException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    public CoordinateReferenceSystem readPrjToCRS() throws IOException {
        File prj = getPrjFile();
        if (prj == null || !prj.exists()) {
            return null;
        }

        String wkt = FileUtils.readFileToString(prj, "UTF-8");
        try {
            return CRS.parseWKT(wkt);
        } catch (Exception e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    @Override
    public void cleanup() throws IOException {
        File parentFolder = (file.isFile() ? file.getParentFile() : null);
        for (File file : allFiles()) {
            cleanupFile(file);
        }

        if (parentFolder != null && parentFolder.exists() && parentFolder.isDirectory()) {
            IOUtils.delete(parentFolder);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((suppFiles == null) ? 0 : suppFiles.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        SpatialFile other = (SpatialFile) obj;
        if (suppFiles == null) {
            if (other.suppFiles != null) return false;
        } else if (!suppFiles.equals(other.suppFiles)) return false;
        return true;
    }

    protected Object readResolve() {
        suppFiles = suppFiles == null ? new ArrayList<>() : suppFiles;
        return this;
    }

    public File getStyleFile() {
        return styleFile;
    }

    public void setStyleFile(File styleFile) {
        this.styleFile = styleFile;
    }
}
