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

package org.geoserver.wms;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.WorkspaceQualifyingCallback;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;

public class WMSWorkspaceQualifier extends WorkspaceQualifyingCallback {

    public WMSWorkspaceQualifier(Catalog catalog) {
        super(catalog);
    }

    @Override
    protected void qualifyRequest(
            WorkspaceInfo ws, PublishedInfo l, Service service, Request request) {
        if (WebMapService.class.isInstance(service.getService()) && request.getRawKvp() != null) {
            String layers = (String) request.getRawKvp().get("LAYERS");
            if (layers != null) {
                request.getRawKvp().put("LAYERS", qualifyLayerNamesKVP(layers, ws));
            }

            layers = (String) request.getRawKvp().get("QUERY_LAYERS");
            if (layers != null) {
                request.getRawKvp().put("QUERY_LAYERS", qualifyLayerNamesKVP(layers, ws));
            }

            String layer = (String) request.getRawKvp().get("LAYER");
            if (layer != null) {
                request.getRawKvp().put("LAYER", qualifyName(layer, ws));
            }

            String styles = (String) request.getRawKvp().get("STYLES");
            if (styles != null && !styles.trim().isEmpty()) {
                request.getRawKvp().put("STYLES", qualifyStyleNamesKVP(styles, ws));
            }

            String style = (String) request.getRawKvp().get("STYLE");
            if (style != null && !style.trim().isEmpty()) {
                request.getRawKvp().put("STYLE", qualifyStyleName(style, ws));
            }
        }
    }

    protected void qualifyRequest(
            WorkspaceInfo ws, PublishedInfo l, Operation operation, Request request) {
        GetCapabilitiesRequest gc = parameter(operation, GetCapabilitiesRequest.class);
        if (gc != null) {
            gc.setNamespace(ws.getName());
            return;
        }
    };

    String qualifyLayerNamesKVP(String layers, WorkspaceInfo ws) {
        List<String> list = KvpUtils.readFlat(layers);
        qualifyLayerNames(list, ws);

        return toCommaSeparatedList(list);
    }

    /**
     * Overriding the base class behavior as we want to avoid qualifying global layer group names
     */
    protected void qualifyLayerNames(List<String> names, WorkspaceInfo ws) {
        for (int i = 0; i < names.size(); i++) {
            String baseName = names.get(i);
            String qualified = qualifyName(baseName, ws);
            // only qualify if it's not a layer group (and prefer local layers to groups in case of
            // name clash), but also check for workspace specific layer groups
            if (catalog.getLayerByName(qualified) != null
                    || catalog.getLayerGroupByName(baseName) == null) {
                names.set(i, qualified);
            } else if (catalog.getLayerGroupByName(qualified) != null) {
                names.set(i, qualified);
            }
        }
    }

    String qualifyStyleNamesKVP(String styles, WorkspaceInfo ws) {
        List<String> list = KvpUtils.readFlat(styles);
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            name = qualifyStyleName(name, ws);
            list.set(i, name);
        }

        return toCommaSeparatedList(list);
    }

    private String qualifyStyleName(String name, WorkspaceInfo ws) {
        String qualified = qualifyName(name, ws);
        // does the qualified name exist?
        if (catalog.getStyleByName(qualified) != null) {
            return qualified;
        } else {
            // use the original name instead
            return name;
        }
    }

    private String toCommaSeparatedList(List<String> list) {
        return String.join(",", list);
    }
}
