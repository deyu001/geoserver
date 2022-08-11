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

package org.geoserver.monitor.ows.wms;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class GetFeatureInfoHandler extends RequestObjectHandler {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    public GetFeatureInfoHandler(MonitorConfig config) {
        super("org.geoserver.wms.GetFeatureInfoRequest", config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLayers(Object request) {
        List<Object> queryLayers = (List<Object>) OwsUtils.get(request, "queryLayers");
        if (queryLayers == null) {
            return null;
        }

        List<String> layers = new ArrayList<>();
        for (Object queryLayer : queryLayers) {
            layers.add((String) OwsUtils.get(queryLayer, "name"));
        }

        return layers;
    }

    @Override
    protected BoundingBox getBBox(Object request) {
        Object gmr = OwsUtils.get(request, "getMapRequest");
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) OwsUtils.get(gmr, "crs");
        Envelope mapEnv = (Envelope) OwsUtils.get(gmr, "bbox");
        ReferencedEnvelope mapBbox = new ReferencedEnvelope(mapEnv, crs);
        int x = (Integer) OwsUtils.get(request, "xPixel");
        int y = (Integer) OwsUtils.get(request, "yPixel");
        int width = (Integer) OwsUtils.get(gmr, "width");
        int height = (Integer) OwsUtils.get(gmr, "height");

        Coordinate coord = org.geoserver.wms.WMS.pixelToWorld(x, y, mapBbox, width, height);

        try {
            return new ReferencedEnvelope(new Envelope(coord), crs)
                    .toBounds(monitorConfig.getBboxCrs());
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Could not transform bounding box to logging CRS", e);
            return null;
        }
    }
}
