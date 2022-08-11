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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jdt.annotation.Nullable;
import org.geotools.geometry.jts.JTS;
import org.locationtech.geogig.model.Bounded;
import org.locationtech.geogig.model.Bucket;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk.BucketIndex;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

class MinimalDiffBoundsConsumer implements PreOrderDiffWalk.Consumer {

    private static final GeometryFactory GEOM_FACTORY = CompactMultiPoint.GEOM_FACTORY;

    /** Accumulates punctual differences to save heap */
    private CompactMultiPoint points = new CompactMultiPoint();

    /** Accumulates non punctual differences (i.e. bounding polygons) */
    private List<Geometry> nonPoints = new LinkedList<>();

    private final Lock lock = new ReentrantLock();

    /**
     * @return a single geometry product of unioning all the bounding boxes acquired while
     *     traversing the diff
     */
    public Geometry buildGeometry() {
        List<Geometry> geomList = nonPoints;
        nonPoints = null;
        if (!points.isEmpty()) {
            geomList.add(points);
        }
        points = null;

        Geometry buildGeometry = GEOM_FACTORY.buildGeometry(geomList);
        geomList.clear();
        Geometry union = buildGeometry.union();
        return union;
    }

    @Override
    public boolean feature(@Nullable final NodeRef left, @Nullable final NodeRef right) {
        addEnv(left);
        addEnv(right);
        return true;
    }

    @Override
    public boolean tree(@Nullable final NodeRef left, @Nullable final NodeRef right) {
        if (left == null) {
            addEnv(right);
            return false;
        } else if (right == null) {
            addEnv(left);
            return false;
        }
        return true;
    }

    @Override
    public boolean bucket(
            final NodeRef leftParent,
            final NodeRef rightParent,
            final BucketIndex bucketIndex,
            @Nullable final Bucket left,
            @Nullable final Bucket right) {
        if (left == null) {
            addEnv(right);
            return false;
        } else if (right == null) {
            addEnv(left);
            return false;
        }
        return true;
    }

    private void addEnv(@Nullable Bounded node) {
        if (node == null) {
            return;
        }
        final Envelope env = node.bounds().orNull();
        if (env == null || env.isNull()) {
            return;
        }
        if (isPoint(env)) {
            lock.lock();
            try {
                points.add(env.getMinX(), env.getMinY());
            } finally {
                lock.unlock();
            }
            return;
        }
        Geometry geom;
        if (isOrthoLine(env)) {
            // handle the case where the envelope is given by an orthogonal line so we don't add a
            // zero area polygon
            double width = env.getWidth();
            GrowableCoordinateSequence cs = new GrowableCoordinateSequence();
            if (width == 0D) {
                cs.add(env.getMinX(), env.getMinY());
                cs.add(env.getMinX(), env.getMaxY());
            } else {
                cs.add(env.getMinX(), env.getMinY());
                cs.add(env.getMaxX(), env.getMinY());
            }
            geom = GEOM_FACTORY.createLineString(cs);
        } else {
            geom = JTS.toGeometry(env, GEOM_FACTORY);
        }
        lock.lock();
        try {
            nonPoints.add(geom);
        } finally {
            lock.unlock();
        }
    }

    private boolean isOrthoLine(Envelope env) {
        return env.getArea() == 0D && env.getWidth() > 0D || env.getHeight() > 0D;
    }

    private boolean isPoint(Envelope env) {
        return env.getWidth() == 0D && env.getHeight() == 0D;
    }

    @Override
    public void endTree(@Nullable final NodeRef left, @Nullable final NodeRef right) {
        // nothing to do, intentionally blank
    }

    @Override
    public void endBucket(
            NodeRef leftParent,
            NodeRef rightParent,
            final BucketIndex bucketIndex,
            @Nullable final Bucket left,
            @Nullable final Bucket right) {
        // nothing to do, intentionally blank
    }
}
