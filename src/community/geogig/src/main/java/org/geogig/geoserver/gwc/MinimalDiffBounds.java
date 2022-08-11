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

import static com.google.common.base.Optional.fromNullable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.plumbing.ResolveTreeish;
import org.locationtech.geogig.plumbing.diff.PathFilteringDiffConsumer;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk.Consumer;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.jts.geom.Geometry;

/**
 * An operation that computes the "approximate minimal bounds" difference between two {@link RevTree
 * trees}.
 *
 * <p>The "approximate minimal bounds" is defined as the geometry union of the bounds of each
 * individual difference, with the exception that when a tree node or bucket tree does not exist at
 * either side of the comparison, the traversal of the existing tree is skipped and its whole bounds
 * are used instead of adding up the bounds of each individual feature.
 *
 * <p>One depth level filtering by tree name is supported through {@link #setTreeNameFilter(String)}
 * in order to skip root node's children sibling of the tree of interest.
 *
 * <p>The tree-ish at the left side of the comparison is set through {@link #setOldVersion(String)},
 * and defaults to {@link Ref#HEAD} if not set.
 *
 * <p>The tree-ish at the right side of the comparison is set through {@link
 * #setNewVersion(String)}, and defaults to {@link Ref#WORK_HEAD} if not set.
 */
public class MinimalDiffBounds extends AbstractGeoGigOp<Geometry> {

    private String oldVersion;

    private String newVersion;

    private String treeName;

    public MinimalDiffBounds setOldVersion(String oldTreeish) {
        this.oldVersion = oldTreeish;
        return this;
    }

    public MinimalDiffBounds setNewVersion(String newTreeish) {
        this.newVersion = newTreeish;
        return this;
    }

    public MinimalDiffBounds setTreeNameFilter(String treeName) {
        this.treeName = treeName;
        return this;
    }

    @Override
    protected Geometry _call() {
        final String leftRefSpec = fromNullable(oldVersion).or(Ref.HEAD);
        final String rightRefSpec = fromNullable(newVersion).or(Ref.WORK_HEAD);

        RevTree left = resolveTree(leftRefSpec);
        RevTree right = resolveTree(rightRefSpec);

        ObjectDatabase leftSource = objectDatabase();
        ObjectDatabase rightSource = objectDatabase();

        PreOrderDiffWalk visitor = new PreOrderDiffWalk(left, right, leftSource, rightSource);
        MinimalDiffBoundsConsumer boundsBuilder = new MinimalDiffBoundsConsumer();
        Consumer consumer = boundsBuilder;
        if (treeName != null) {
            consumer = new PathFilteringDiffConsumer(ImmutableList.of(treeName), boundsBuilder);
        }
        visitor.walk(consumer);
        Geometry minimalBounds = boundsBuilder.buildGeometry();
        return minimalBounds;
    }

    private RevTree resolveTree(String refSpec) {

        Optional<ObjectId> id = command(ResolveTreeish.class).setTreeish(refSpec).call();
        Preconditions.checkState(id.isPresent(), "%s did not resolve to a tree", refSpec);

        return objectDatabase().getTree(id.get());
    }
}
