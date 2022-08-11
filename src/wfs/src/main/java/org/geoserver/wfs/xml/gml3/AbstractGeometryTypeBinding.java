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

package org.geoserver.wfs.xml.gml3;

import org.geoserver.wfs.WFSException;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml2.bindings.GML2ParsingUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.PointOutsideEnvelopeException;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;

/**
 * Subclass of {@link org.geotools.gml3.bindings.AbstractGeometryTypeBinding} which performs some
 * addtional validation checks.
 *
 * <p>Checks include:
 *
 * <ul>
 *   <li>All geometries have a crs, when not specified, the server default is used.
 *   <li>If a crs is specified it has a valid authority
 *   <li>Points defined on geometries fall into the valid coordinate space defined by crs.
 * </ul>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class AbstractGeometryTypeBinding
        extends org.geotools.gml3.bindings.AbstractGeometryTypeBinding {

    CoordinateReferenceSystem crs;

    public AbstractGeometryTypeBinding() {
        super(null, null);
    }

    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public void initializeChildContext(
            ElementInstance childInstance, Node node, MutablePicoContainer context) {
        // if an srsName is set for this geometry, put it in the context for
        // children, so they can use it as well
        if (node.hasAttribute("srsName")) {
            try {
                CoordinateReferenceSystem crs = GML2ParsingUtils.crs(node);
                if (crs != null) {
                    context.registerComponentInstance(CoordinateReferenceSystem.class, crs);
                }
            } catch (Exception e) {
                throw new WFSException(e, "InvalidParameterValue");
            }
        }
    }

    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        try {
            if (node.hasAttribute("srsName")) {
                CRS.decode(node.getAttributeValue("srsName").toString());
            }
        } catch (NoSuchAuthorityCodeException e) {
            throw new WFSException(
                    "Invalid Authority Code: " + e.getAuthorityCode(), "InvalidParameterValue");
        }

        Geometry geometry = (Geometry) super.parse(instance, node, value);

        if (geometry != null) {
            // 1. ensure a crs is set
            if (geometry.getUserData() == null) {
                // no crs set for the geometry, did we inherit one from a parent?
                if (crs != null) {
                    geometry.setUserData(crs);
                }
                // else {
                // for the moment we don't do anything since we miss the information
                // to infer the CRS from the feature type
                // }
            }

            // 2. ensure the coordinates of the geometry fall into valid space defined by crs
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) geometry.getUserData();
            if (crs != null)
                try {
                    JTS.checkCoordinatesRange(geometry, crs);
                } catch (PointOutsideEnvelopeException e) {
                    throw new WFSException(e, "InvalidParameterValue");
                }
        }

        return geometry;
    }
}
