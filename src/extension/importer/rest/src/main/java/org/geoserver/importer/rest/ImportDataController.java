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

package org.geoserver.importer.rest;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import java.util.NoSuchElementException;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportJSONWriter;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/imports/{importId}",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypeExtensions.TEXT_JSON_VALUE}
)
public class ImportDataController extends ImportBaseController {

    public ImportDataController(Importer importer) {
        super(importer);
    }

    protected ImportJSONWriter converterWriter;

    @GetMapping(
        value = {
            "/data",
            "/tasks/{taskId}/data",
        }
    )
    public ImportData getData(
            @PathVariable Long importId, @PathVariable(required = false) Integer taskId)
            throws Exception {

        ImportData data = null;

        ImportTask task = task(importId, taskId, true);
        if (task != null) {
            data = task.getData();
            data.setParent(task);

        } else {
            final ImportContext context = context(importId);
            data = context.getData();
            data.setParent(context);
        }

        return data;
    }

    // We need to force spring to ignore the .shp here (we don't want a .shp encoded response!
    @GetMapping(
        value = {
            "/data/files", "/tasks/{taskId}/data/files",
            "/data/files/{fileName:.+}", "/tasks/{taskId}/data/files/{fileName:\\.+}"
        }
    )
    public ImportData getDirectory(
            @PathVariable Long importId,
            @PathVariable(required = false) Integer taskId,
            @PathVariable(required = false) String fileName)
            throws Exception {

        return getDataImport(importId, fileName);
    }

    // We need to force spring to ignore the .shp here (we don't want a .shp encoded response!
    @DeleteMapping(
        value = {"/data/files/{fileName:.+}", "/tasks/{taskId}/data/files/{fileName:\\.+}"}
    )
    public ResponseEntity deleteDirectory(
            @PathVariable Long importId,
            @PathVariable(required = false) Integer taskId,
            @PathVariable(required = false) String fileName)
            throws Exception {

        Directory dir = lookupDirectory(importId);
        ImportData file = lookupFile(fileName, dir);

        if (dir.getFiles().remove(file)) {
            return new ResponseEntity<>("", new HttpHeaders(), HttpStatus.NO_CONTENT);
        } else {
            throw new RestException(
                    "Unable to remove file: " + file.getName(), HttpStatus.BAD_REQUEST);
        }
    }

    private ImportData getDataImport(Long importId, String fileName) {
        Directory dir = lookupDirectory(importId);
        ImportData response = dir;

        if (fileName != null) {
            response = lookupFile(fileName, dir);
            response.setParent((ImportContext) dir.getParent());
        }
        return response;
    }

    Directory lookupDirectory(Long importId) {
        ImportContext context = context(importId);
        if (!(context.getData() instanceof Directory)) {
            throw new RestException("Data is not a directory", HttpStatus.BAD_REQUEST);
        }

        Directory data = (Directory) context.getData();
        data.setParent(context);
        return data;
    }

    public ImportData lookupFile(String file, Directory dir) {
        try {
            if (file != null) {
                return Iterators.find(
                        dir.getFiles().iterator(),
                        new Predicate<FileData>() {
                            @Override
                            public boolean apply(FileData input) {
                                return input.getFile().getName().equals(file);
                            }
                        });
            }
        } catch (NoSuchElementException e) {

        }
        throw new RestException("No such file: " + file, HttpStatus.NOT_FOUND);
    }
}
