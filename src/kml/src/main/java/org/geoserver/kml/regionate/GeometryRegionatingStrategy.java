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

package org.geoserver.kml.regionate;

import java.util.Map;
import java.util.logging.Level;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;

/**
 * Strategy using geometry size to determine feature allocation in tiles. Bigger geometries get into
 * the bigger tiles. Does not work with simple points, use attribute sorting or random strategy in
 * that case
 *
 * @author Andrea Aime
 */
public class GeometryRegionatingStrategy extends ExternalSortRegionatingStrategy {

    public GeometryRegionatingStrategy(GeoServer gs) {
        super(gs);
    }

    @Override
    protected void checkAttribute(WMSMapContent con, SimpleFeatureType ft) {
        // find out which attribute we're going to use
        Map options = con.getRequest().getFormatOptions();
        attribute = (String) options.get("regionateAttr");
        if (attribute == null) {
            attribute = MapLayerInfo.getRegionateAttribute(featureType);
        }
        if (attribute == null || ft.getDescriptor(attribute) == null) {
            LOGGER.log(
                    Level.FINER, "No attribute specified, falling " + "back on geometry attribute");
            attribute = ft.getGeometryDescriptor().getLocalName();
        } else {
            // Make sure the attribute is actually there
            AttributeType attributeType = ft.getType(attribute);
            if (attributeType == null) {
                throw new ServiceException(
                        "Could not find regionating attribute "
                                + attribute
                                + " in layer "
                                + featureType.getName());
            }
        }

        // geometry size is a double
        h2Type = "DOUBLE";
    }

    @Override
    protected String checkAttribute(FeatureTypeInfo cfg) {
        String attribute = MapLayerInfo.getRegionateAttribute(cfg);
        try {
            FeatureType ft = cfg.getFeatureType();
            if ((attribute != null) && (ft.getDescriptor(attribute) != null)) return attribute;

            return ft.getGeometryDescriptor().getLocalName();
        } catch (Exception e) {
            LOGGER.severe("Couldn't get attribute name due to " + e);
            return null;
        }
    }

    @Override
    protected Double getSortAttributeValue(SimpleFeature f) {
        Geometry g = (Geometry) f.getAttribute(attribute);

        if (g instanceof MultiPoint) return (double) g.getNumGeometries();
        if (g instanceof Polygon || g instanceof MultiPolygon) return g.getArea();
        else return g.getLength();
    }
}
