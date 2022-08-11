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

import java.util.List;
import org.geoserver.wps.ppio.gpx.GpxType;
import org.geoserver.wps.ppio.gpx.RteType;
import org.geoserver.wps.ppio.gpx.WptType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;

/** Small helper class to convert from JTS Geometry to GPX types */
public class GPXHelper {
    private GpxType gpxType;

    public GPXHelper(GpxType gpx) {
        this.gpxType = gpx;
    }

    public void addFeature(SimpleFeature f) {

        Object defaultGeometry = f.getDefaultGeometryProperty();
        if (defaultGeometry == null) {
            return;
        }
        String nameStr = null;
        String commentStr = null;
        String descriptionStr = null;

        for (Property p : f.getProperties()) {
            Object object = p.getValue();
            if (object instanceof Geometry) {
                continue;
            } else {
                Name name = p.getName();
                if (name.getLocalPart().equalsIgnoreCase("name")
                        || name.getLocalPart().equalsIgnoreCase("geographicalName")) {
                    nameStr = p.getValue().toString();
                } else if (name.getLocalPart().equalsIgnoreCase("description")) {
                    descriptionStr = p.getValue().toString();
                } else if (name.getLocalPart().equalsIgnoreCase("comment")) {
                    commentStr = p.getValue().toString();
                }
            }
        }

        Object go = ((Property) defaultGeometry).getValue();
        if (go instanceof MultiLineString) {
            int nrls = ((MultiLineString) go).getNumGeometries();
            for (int li = 0; li < nrls; li++) {
                Geometry ls = ((MultiLineString) go).getGeometryN(li);
                RteType rte = toRte((LineString) ls);
                if (nameStr != null) rte.setName(nameStr);
                if (commentStr != null) rte.setCmt(commentStr);
                if (descriptionStr != null) rte.setDesc(descriptionStr);
                gpxType.getRte().add(rte);
            }
        } else if (go instanceof LineString) {
            RteType rte = toRte((LineString) go);
            if (nameStr != null) rte.setName(nameStr);
            if (commentStr != null) rte.setCmt(commentStr);
            if (descriptionStr != null) rte.setDesc(descriptionStr);
            gpxType.getRte().add(rte);
        } else if (go instanceof MultiPoint) {
            int nrpt = ((MultiPoint) go).getNumGeometries();
            for (int pi = 0; pi < nrpt; pi++) {
                Geometry pt = ((MultiPoint) go).getGeometryN(pi);
                WptType wpt = toWpt((Point) pt);
                if (nameStr != null) wpt.setName(nameStr);
                if (commentStr != null) wpt.setCmt(commentStr);
                if (descriptionStr != null) wpt.setDesc(descriptionStr);
                gpxType.getWpt().add(wpt);
            }
        } else if (go instanceof Point) {
            WptType wpt = toWpt((Point) go);
            if (nameStr != null) wpt.setName(nameStr);
            if (commentStr != null) wpt.setCmt(commentStr);
            if (descriptionStr != null) wpt.setDesc(descriptionStr);
            gpxType.getWpt().add(wpt);
        } else {
            // no useful geometry, no feature!
            return;
        }
    }

    public WptType toWpt(Point p) {
        return coordToWpt(p.getX(), p.getY());
    }

    private WptType coordToWpt(double x, double y) {
        WptType wpt = new WptType();
        wpt.setLon(x);
        wpt.setLat(y);
        return wpt;
    }

    public RteType toRte(LineString ls) {
        RteType rte = new RteType();
        List<WptType> rtePts = rte.getRtept();

        Coordinate[] coordinates = ((Geometry) ls).getCoordinates();
        for (int pi = 0; pi < coordinates.length; pi++) {
            rtePts.add(coordToWpt(coordinates[pi].x, coordinates[pi].y));
        }
        return rte;
    }
}
