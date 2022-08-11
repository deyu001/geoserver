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

package org.geoserver.rest.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

public abstract class AbstractStoreUploadController extends AbstractCatalogController {

    static final Logger LOGGER = Logging.getLogger(AbstractStoreUploadController.class);

    /** The ways a file upload can be achieved */
    protected enum UploadMethod {
        file(true),
        external(false),
        url(true),
        remote(false); // Remote upload being only supported by structuredGridCoverage2DReader

        boolean inline;

        UploadMethod(boolean inline) {
            this.inline = inline;
        }

        public boolean isInline() {
            return inline;
        }
    }

    public AbstractStoreUploadController(Catalog catalog) {
        super(catalog);
    }

    /** */
    protected List<Resource> handleFileUpload(
            String store,
            String workspace,
            String filename,
            UploadMethod method,
            String format,
            Resource directory,
            HttpServletRequest request) {

        List<Resource> files = new ArrayList<>();

        Resource uploadedFile;
        boolean external = false;
        try {
            if (method == UploadMethod.file) {
                // we want to delete the previous dir contents only in case of PUT, not
                // in case of POST (harvest, available only for raster data)
                boolean cleanPreviousContents = HttpMethod.PUT.name().equals(request.getMethod());
                if (filename == null) {
                    filename = buildUploadedFilename(store, format);
                }
                uploadedFile =
                        RESTUtils.handleBinUpload(
                                filename, directory, cleanPreviousContents, request, workspace);
            } else if (method == UploadMethod.url) {
                uploadedFile =
                        RESTUtils.handleURLUpload(
                                buildUploadedFilename(store, format),
                                workspace,
                                directory,
                                request);
            } else if (method == UploadMethod.external) {
                uploadedFile = RESTUtils.handleEXTERNALUpload(request);
                external = true;
            } else {
                throw new RestException(
                        "Unrecognized file upload method: " + method, HttpStatus.BAD_REQUEST);
            }
        } catch (Throwable t) {
            if (t instanceof RestException) {
                throw (RestException) t;
            } else {
                throw new RestException(
                        "Error while storing uploaded file:", HttpStatus.INTERNAL_SERVER_ERROR, t);
            }
        }

        // handle the case that the uploaded file was a zip file, if so unzip it
        if (RESTUtils.isZipMediaType(request)) {
            // rename to .zip if need be
            if (!uploadedFile.name().endsWith(".zip")) {
                Resource newUploadedFile =
                        uploadedFile
                                .parent()
                                .get(FilenameUtils.getBaseName(uploadedFile.path()) + ".zip");
                String oldFileName = uploadedFile.name();
                if (!uploadedFile.renameTo(newUploadedFile)) {
                    String errorMessage =
                            "Error renaming zip file from "
                                    + oldFileName
                                    + " -> "
                                    + newUploadedFile.name();
                    throw new RestException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
                }
                uploadedFile = newUploadedFile;
            }
            // unzip the file
            try {
                // Unzipping of the file and, if it is a POST request, filling of the File List
                RESTUtils.unzipFile(uploadedFile, directory, workspace, store, files, external);

                // look for the "primary" file
                // TODO: do a better check
                Resource primaryFile = findPrimaryFile(directory, format);
                if (primaryFile != null) {
                    uploadedFile = primaryFile;
                } else {
                    throw new RestException(
                            "Could not find appropriate " + format + " file in archive",
                            HttpStatus.BAD_REQUEST);
                }
            } catch (RestException e) {
                throw e;
            } catch (Exception e) {
                throw new RestException(
                        "Error occured unzipping file", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }
        // If the File List is empty then the uploaded file must be added
        if (files.isEmpty() && uploadedFile != null) {
            files.clear();
            files.add(uploadedFile);
        } else {
            files.add(0, uploadedFile);
        }

        return files;
    }

    /** Build name for an uploaded file. */
    private String buildUploadedFilename(String store, String format) {
        if ("h2".equalsIgnoreCase(format)) {
            return store + ".data.db";
        } else {
            return store + "." + format;
        }
    }

    /** */
    protected Resource findPrimaryFile(Resource directory, String format) {
        for (Resource f :
                Resources.list(
                        directory, new Resources.ExtensionFilter(format.toUpperCase()), true)) {
            // assume the first
            return f;
        }
        return null;
    }
}
