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

package org.geogig.geoserver.gwc;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Dimension;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.GeometryFilter;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * A growable {@link MultiPoint} that saves all points in a single {@link
 * GrowableCoordinateSequence}
 */
class CompactMultiPoint extends MultiPoint {

    private static final long serialVersionUID = 1L;

    public static final GeometryFactory GEOM_FACTORY =
            new GeometryFactory(
                    new PrecisionModel(1E6),
                    0,
                    new PackedCoordinateSequenceFactory(PackedCoordinateSequenceFactory.FLOAT));

    private final GrowableCoordinateSequence coordSeq;

    public CompactMultiPoint() {
        this(new GrowableCoordinateSequence(), new Envelope());
    }

    private CompactMultiPoint(GrowableCoordinateSequence coordSeq, Envelope envelope) {
        super(new Point[0], GEOM_FACTORY);
        super.envelope = envelope;
        this.coordSeq = coordSeq;
    }

    public void add(double x, double y) {
        coordSeq.add(x, y);
        super.envelope.expandToInclude(x, y);
    }

    // /////////////// GeometryCollection overrides ///////////////////////
    @Override
    public void apply(CoordinateFilter filter) {
        final int size = coordSeq.size();
        for (int i = 0; i < size; i++) {
            getGeometryN(i).apply(filter);
        }
    }

    @Override
    public void apply(CoordinateSequenceFilter filter) {
        final int size = coordSeq.size();
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            getGeometryN(i).apply(filter);
            if (filter.isDone()) {
                break;
            }
        }
        if (filter.isGeometryChanged()) {
            geometryChanged();
        }
    }

    @Override
    public void apply(GeometryComponentFilter filter) {
        filter.filter(this);
        final int size = coordSeq.size();
        for (int i = 0; i < size; i++) {
            getGeometryN(i).apply(filter);
        }
    }

    @Override
    public void apply(GeometryFilter filter) {
        filter.filter(this);
        final int size = coordSeq.size();
        for (int i = 0; i < size; i++) {
            getGeometryN(i).apply(filter);
        }
    }

    @Override
    public boolean equalsExact(Geometry other, double tolerance) {
        if (!(other instanceof MultiPoint)) {
            return false;
        }
        if (getNumGeometries() != other.getNumGeometries()) {
            return false;
        }
        for (int i = 0; i < getNumGeometries(); i++) {
            if (!getGeometryN(i).equalsExact(other.getGeometryN(i), tolerance)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CompactMultiPoint copyInternal() {
        return new CompactMultiPoint(coordSeq.clone(), new Envelope(envelope));
    }

    @Override
    public double getArea() {
        return 0D;
    }

    @Override
    public boolean isEmpty() {
        return envelope.isNull();
    }

    @Override
    public int getDimension() {
        return 0;
    }

    @Override
    public int getBoundaryDimension() {
        return Dimension.FALSE;
    }

    @Override
    public Coordinate getCoordinate() {
        if (isEmpty()) {
            return null;
        }
        return coordSeq.getCoordinate(0);
    }

    @Override
    public Coordinate[] getCoordinates() {
        return coordSeq.toCoordinateArray();
    }

    @Override
    public Geometry getGeometryN(int n) {
        CoordinateSequence subSequence = coordSeq.subSequence(n, n);
        return GEOM_FACTORY.createPoint(subSequence);
    }

    @Override
    public double getLength() {
        return 0D;
    }

    @Override
    public int getNumGeometries() {
        return coordSeq.size();
    }

    @Override
    public int getNumPoints() {
        return getNumGeometries();
    }

    @Override
    public void normalize() {
        // nothing to do
    }

    @Override
    public Geometry reverse() {
        // nothing to do
        return (Geometry) clone();
    }

    // //////////////////// Geometry overrides ////////////////////////

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
