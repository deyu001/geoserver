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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;

public class ImportBaseController extends RestBaseController {

    private static final Logger LOGGER = Logging.getLogger(ImportBaseController.class);

    protected Importer importer;

    protected ImportBaseController(Importer importer) {
        this.importer = importer;
    }

    protected ImportContext context(Long imp) {
        return context(imp, false);
    }

    protected ImportContext context(Long imp, boolean optional) {
        return (ImportContext) context(imp, optional, false);
    }

    Object context(Long imp, boolean optional, boolean allowAll) {
        if (imp == null) {
            if (allowAll) {
                return importer.getAllContexts();
            }
            if (optional) {
                return null;
            }
            throw new RestException("No import specified", HttpStatus.BAD_REQUEST);
        } else {
            ImportContext context = null;
            context = importer.getContext(imp);
            if (context == null && !optional) {
                throw new RestException("No such import: " + imp.toString(), HttpStatus.NOT_FOUND);
            }
            return context;
        }
    }

    protected ImportTask task(Long imp, Integer taskNumber) {
        return task(imp, taskNumber, false);
    }

    protected ImportTask task(Long imp, Integer taskNumber, boolean optional) {
        return (ImportTask) task(imp, taskNumber, optional, false);
    }

    protected Object task(Long imp, Integer taskNumber, boolean optional, boolean allowAll) {
        ImportContext context = context(imp);
        ImportTask task = null;

        // handle null taskNumber
        if (taskNumber == null) {
            if (!optional && !allowAll) {
                throw new RestException("No task specified", HttpStatus.NOT_FOUND);
            }
        } else {
            task = context.task(taskNumber);
        }

        // handle no task found
        if (task == null) {
            if (allowAll) {
                return context.getTasks();
            }
            if (!optional) {
                throw new RestException(
                        "No such task: " + taskNumber + " for import: " + context.getId(),
                        HttpStatus.NOT_FOUND);
            }
        }
        return task;
    }

    ImportTransform transform(Long importId, Integer taskId, Integer transformId) {
        return transform(importId, taskId, transformId, false);
    }

    ImportTransform transform(
            Long importId, Integer taskId, Integer transformId, boolean optional) {
        ImportTask task = task(importId, taskId);

        return transform(task, transformId, optional);
    }

    ImportTransform transform(ImportTask task, Integer transformId, boolean optional) {
        ImportTransform tx = null;
        if (transformId != null) {
            try {
                tx = task.getTransform().getTransforms().get(transformId);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                LOGGER.log(
                        Level.FINER,
                        "No transform with  id "
                                + transformId
                                + ". Exception message is "
                                + e.getMessage(),
                        e);
            }
        }

        if (tx == null && !optional) {
            throw new RestException("No such transform", HttpStatus.NOT_FOUND);
        }
        return tx;
    }

    protected int expand(int def, String ex) {
        if (ex == null) {
            return def;
        }
        try {
            return "self".equalsIgnoreCase(ex)
                    ? 1
                    : "all".equalsIgnoreCase(ex)
                            ? Integer.MAX_VALUE
                            : "none".equalsIgnoreCase(ex) ? 0 : Integer.parseInt(ex);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
