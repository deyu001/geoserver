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

package org.geoserver.wfs.xml;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.ProfileImpl;
import org.geotools.gml3.GML;
import org.geotools.gml3.GMLSchema;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.Schema;

public class GML3Profile extends TypeMappingProfile {
    static Set<Schema> profiles = new HashSet<>();

    static {
        // set with guaranteed iteration order, so that we can put deprecated elements only
        // after the ones that replaced them
        Set<Name> profile = new LinkedHashSet<>();

        // basic
        profile.add(new NameImpl(GML.NAMESPACE, GML.MeasureType.getLocalPart()));

        // geomtetries
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.PointType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.PointPropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiPointType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiPointPropertyType.getLocalPart()));

        // profile.add( new NameImpl(  GML.NAMESPACE, GML.LineStringType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.LineStringPropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiLineStringType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiLineStringPropertyType.getLocalPart()));

        // profile.add( new NameImpl(  GML.NAMESPACE, GML.CurveType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.CurvePropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiCurveType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiCurvePropertyType.getLocalPart()));

        profile.add(new NameImpl(GML.NAMESPACE, GML.SurfacePropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiSurfaceType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiSurfacePropertyType.getLocalPart()));

        // register polygon and multipolygon only after surface, the iteration order
        // will make sure surface is found before in any encoding attempt, this way we
        // are still able to handle polygons, but we don't use them by default
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.PolygonType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.PolygonPropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiPolygonType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiPolygonPropertyType.getLocalPart()));

        // profile.add( new NameImpl(  GML.NAMESPACE, GML.AbstractGeometryType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.GeometryPropertyType.getLocalPart()));
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiGeometryPropertyType.getLocalPart()));

        profiles.add(new ProfileImpl(new GMLSchema(), profile));
    }

    public GML3Profile() {
        super(profiles);
    }
}
