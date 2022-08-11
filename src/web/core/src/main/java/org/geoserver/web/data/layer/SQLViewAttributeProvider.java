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

package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

@SuppressWarnings("serial")
public class SQLViewAttributeProvider extends GeoServerDataProvider<SQLViewAttribute> {

    static final Logger LOGGER = Logging.getLogger(SQLViewAttribute.class);

    static final Property<SQLViewAttribute> NAME = new BeanProperty<>("name", "name");

    static final Property<SQLViewAttribute> TYPE =
            new AbstractProperty<SQLViewAttribute>("type") {

                public Object getPropertyValue(SQLViewAttribute item) {
                    if (item.getType() != null) {
                        return item.getType().getSimpleName();
                    }
                    return null;
                }
            };

    static final Property<SQLViewAttribute> SRID = new BeanProperty<>("srid", "srid");

    static final Property<SQLViewAttribute> PK = new BeanProperty<>("pk", "pk");

    List<SQLViewAttribute> attributes = new ArrayList<>();

    public SQLViewAttributeProvider() {
        setEditable(true);
    }

    void setFeatureType(SimpleFeatureType ft, VirtualTable vt) {
        attributes.clear();
        for (AttributeDescriptor ad : ft.getAttributeDescriptors()) {
            SQLViewAttribute at =
                    new SQLViewAttribute(ad.getLocalName(), ad.getType().getBinding());
            String attName = ad.getName().getLocalPart();
            attributes.add(at);
            if (ad instanceof GeometryDescriptor) {
                GeometryDescriptor gd = (GeometryDescriptor) ad;
                if (gd.getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID) != null) {
                    at.setSrid((Integer) gd.getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID));
                } else if (gd.getCoordinateReferenceSystem() != null) {
                    try {
                        at.setSrid(CRS.lookupEpsgCode(gd.getCoordinateReferenceSystem(), false));
                    } catch (Exception e) {
                        // it is ok, we're just trying to facilitate the user's life here
                    }
                }
                if (vt != null && vt.getGeometries().contains(attName)) {
                    at.setSrid(vt.getNativeSrid(attName));
                    at.setType(vt.getGeometryType(attName));
                }
            }
            if (vt != null
                    && vt.getPrimaryKeyColumns() != null
                    && vt.getPrimaryKeyColumns().contains(attName)) {
                at.setPk(true);
            }
        }
    }

    @Override
    protected List<SQLViewAttribute> getItems() {
        return attributes;
    }

    @Override
    protected List<Property<SQLViewAttribute>> getProperties() {
        return Arrays.asList(NAME, TYPE, SRID, PK);
    }

    /** Sets the geometries details and the primary key columns into the virtual table */
    @SuppressWarnings("unchecked")
    public void fillVirtualTable(VirtualTable vt) {
        List<String> pks = new ArrayList<>();
        for (SQLViewAttribute att : attributes) {
            if (Geometry.class.isAssignableFrom(att.getType())) {
                if (att.getSrid() == null) {
                    vt.addGeometryMetadatata(
                            att.getName(), (Class<? extends Geometry>) att.getType(), 4326);
                } else {
                    vt.addGeometryMetadatata(
                            att.getName(),
                            (Class<? extends Geometry>) att.getType(),
                            att.getSrid());
                }
            }
            if (att.pk) {
                pks.add(att.getName());
            }
        }
        if (!pks.isEmpty()) {
            vt.setPrimaryKeyColumns(pks);
        }
    }
}
