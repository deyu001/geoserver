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

package org.geoserver.wps.ppio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.response.ShapeZipOutputFormat;
import org.geoserver.wps.resource.ShapefileResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.URLs;

/**
 * Handles input and output of feature collections as zipped shapefiles
 *
 * @author Andrea Aime - OpenGeo
 */
public class ShapeZipPPIO extends BinaryPPIO {

    private final GeoServer gs;
    private final Catalog catalog;
    private final GeoServerResourceLoader resourceLoader;
    WPSResourceManager resources;

    protected ShapeZipPPIO(
            WPSResourceManager resources,
            GeoServer gs,
            Catalog catalog,
            GeoServerResourceLoader resourceLoader) {
        super(FeatureCollection.class, FeatureCollection.class, "application/zip");
        this.resources = resources;
        this.gs = gs;
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) value;
        ShapeZipOutputFormat of = new ShapeZipOutputFormat(gs, catalog, resourceLoader);
        of.write(Collections.singletonList(fc), getCharset(), os, null);
    }

    private Charset getCharset() {
        final String charsetName =
                GeoServerExtensions.getProperty(
                        ShapeZipOutputFormat.GS_SHAPEFILE_CHARSET, (ServletContext) null);
        if (charsetName != null) {
            return Charset.forName(charsetName);
        } else {
            // if not specified let's use the shapefile default one
            return Charset.forName("ISO-8859-1");
        }
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // create the temp directory and register it as a temporary resource
        File tempDir = IOUtils.createTempDirectory("shpziptemp");

        // unzip to the temporary directory
        ZipInputStream zis = null;
        File shapeFile = null;
        try {
            zis = new ZipInputStream(input);
            ZipEntry entry = null;

            while ((entry = zis.getNextEntry()) != null) {
                File file = IOUtils.getZipOutputFile(tempDir, entry);
                if (entry.isDirectory()) {
                    file.mkdir();
                } else {
                    if (file.getName().toLowerCase().endsWith(".shp")) {
                        shapeFile = file;
                    }

                    int count;
                    byte data[] = new byte[4096];
                    // write the files to the disk
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        while ((count = zis.read(data)) != -1) {
                            fos.write(data, 0, count);
                        }
                        fos.flush();
                    }
                }
                zis.closeEntry();
            }
        } finally {
            if (zis != null) {
                zis.close();
            }
        }

        if (shapeFile == null) {
            FileUtils.deleteDirectory(tempDir);
            throw new IOException("Could not find any file with .shp extension in the zip file");
        } else {
            ShapefileDataStore store = new ShapefileDataStore(URLs.fileToUrl(shapeFile));
            resources.addResource(new ShapefileResource(store, tempDir));
            return store.getFeatureSource().getFeatures();
        }
    }

    @Override
    public String getFileExtension() {
        return "zip";
    }
}
