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

package org.geoserver.ows;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

/**
 * Dispatcher callback that sets and clears the {@link LocalWorkspace} and {@link LocalPublished}
 * thread locals.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LocalWorkspaceCallback implements DispatcherCallback, ExtensionPriority {

    static final Logger LOGGER = Logging.getLogger(LocalWorkspaceCallback.class);

    GeoServer gs;
    Catalog catalog;

    public LocalWorkspaceCallback(GeoServer gs) {
        this.gs = gs;
        catalog = gs.getCatalog();
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public Request init(Request request) {
        WorkspaceInfo ws = null;
        LayerGroupInfo lg = null;
        if (request.context != null) {
            String first = request.context;
            String last = null;

            int slash = first.indexOf('/');
            if (slash > -1) {
                last = first.substring(slash + 1);
                first = first.substring(0, slash);
            }

            // check if the context matches a workspace
            ws = catalog.getWorkspaceByName(first);
            if (ws != null) {
                LocalWorkspace.set(ws);

                // set the local layer if it exists
                if (last != null) {
                    // hack up a qualified name
                    NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
                    if (ns != null) {
                        // can have extra bits, like ws/layer/gwc/service
                        int slashInLayer = last.indexOf('/');
                        if (slashInLayer != -1) {
                            last = last.substring(0, slashInLayer);
                        }

                        LayerInfo l = catalog.getLayerByName(new NameImpl(ns.getURI(), last));
                        if (l != null) {
                            LocalPublished.set(l);
                        } else {
                            LOGGER.log(
                                    Level.FINE,
                                    "Could not lookup context {0} as a layer, trying as group",
                                    first);
                            lg = catalog.getLayerGroupByName(ws, last);
                            if (lg != null) {
                                LocalPublished.set(lg);
                            } else {
                                // TODO: perhaps throw an exception?
                                LOGGER.log(
                                        Level.FINE,
                                        "Could not lookup context {0} as a group either",
                                        first);
                            }
                        }
                    }
                }
            } else {
                LOGGER.log(
                        Level.FINE,
                        "Could not lookup context {0} as a workspace, trying as group",
                        first);
                lg = catalog.getLayerGroupByName((WorkspaceInfo) null, first);
                if (lg != null) {
                    LocalPublished.set(lg);
                } else {
                    LOGGER.log(
                            Level.FINE,
                            "Could not lookup context {0} as a layer group either",
                            first);
                }
            }
            if (ws == null && lg == null) {
                // if no workspace context specified and server configuration not allowing global
                // services throw an error
                if (!gs.getGlobal().isGlobalServices()) {
                    throw new ServiceException("No such workspace '" + request.context + "'");
                }
            }
        } else if (!gs.getGlobal().isGlobalServices()) {
            throw new ServiceException("No workspace specified");
        }

        return request;
    }

    public Operation operationDispatched(Request request, Operation operation) {
        return null;
    }

    public Object operationExecuted(Request request, Operation operation, Object result) {
        return null;
    }

    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return null;
    }

    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return null;
    }

    public void finished(Request request) {
        LocalWorkspace.remove();
        LocalPublished.remove();
    }

    public int getPriority() {
        return HIGHEST;
    }
}
