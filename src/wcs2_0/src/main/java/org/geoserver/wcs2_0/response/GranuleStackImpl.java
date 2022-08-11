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

package org.geoserver.wcs2_0.response;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * A GridCoverage instance composed of several GridCoverage2D Granules which may be obtained through
 * the getGranules() method.
 *
 * <p>TODO: note that we extends GridCoverage2D since all coverageResponseDelegate.encode has a
 * GridCoverage2D as input parameter. we should propose an API change where we encode a GridCoverage
 * instead and where GridCoverage has a dispose method to be implemented.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class GranuleStackImpl extends GridCoverage2D /*AbstractGridCoverage*/
        implements GranuleStack {

    /**
     * Right now, all CoverageResponseDelegate work with GridCoverage2D. Therefore, in order to
     * encode a granuleStack we need to implement it as a GridCoverage2D. So we pass a Dummy
     * GridCoverage2D with dummy information to avoid constructor failures. The provided information
     * will be ignored anyway.
     *
     * <p>Once we move to extending AbstractGridCoverage instead of GridCoverage2D we may remove
     * this dummy class.
     */
    static class DummyGridCoverage2D extends GridCoverage2D {

        static GridEnvelope SAMPLE_GRID_ENVELOPE = new GridEnvelope2D(new Rectangle(0, 0, 1, 1));

        static MathTransform SAMPLE_TRANSFORM =
                ProjectiveTransform.create(AffineTransform.getScaleInstance(1, 1));

        static PlanarImage SAMPLE_IMAGE =
                new TiledImage(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY), false);

        protected DummyGridCoverage2D(CharSequence name, CoordinateReferenceSystem crs)
                throws IllegalArgumentException {
            super(
                    name,
                    SAMPLE_IMAGE,
                    new GridGeometry2D(
                            SAMPLE_GRID_ENVELOPE,
                            new GeneralEnvelope(
                                    SAMPLE_GRID_ENVELOPE,
                                    PixelInCell.CELL_CENTER,
                                    SAMPLE_TRANSFORM,
                                    crs)),
                    null,
                    null,
                    null,
                    null);
        }
    }

    /** The list of all dimensions available for this stak */
    private List<DimensionBean> dimensions;

    @Override
    public String toString() {
        return "GranuleStackImpl [dimensions=" + dimensions + ", coverages=" + coverages + "]";
    }

    /** The coverages stored by this Granule stack */
    private List<GridCoverage2D> coverages;

    /** Granule stack constructor. */
    public GranuleStackImpl(
            CharSequence name, CoordinateReferenceSystem crs, List<DimensionBean> dimensions) {
        super(name, new DummyGridCoverage2D(name, crs));
        this.dimensions = dimensions;
        this.coverages = new ArrayList<>();
    }

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public RenderedImage getRenderedImage() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Unable to return a RenderedImage for a GranuleStack which is made of different coverages: returning null");
        }
        return null;
    }

    @Override
    public Object evaluate(DirectPosition point)
            throws PointOutsideCoverageException, CannotEvaluateException {
        throw new UnsupportedOperationException(
                "This is a multidimensional coverage, you should access its contents calling getGranules");
    }

    @Override
    public int getNumSampleDimensions() {
        throw new UnsupportedOperationException(
                "This is a multidimensional coverage, you should access its contents calling getGranules");
    }

    @Override
    public List<DimensionBean> getDimensions() {
        return dimensions;
    }

    @Override
    public List<GridCoverage2D> getGranules() {
        return coverages;
    }

    public void addCoverage(GridCoverage2D coverage) {
        coverages.add(coverage);
    }

    @Override
    public boolean dispose(boolean force) {
        boolean disposed = true;
        for (GridCoverage2D coverage : coverages) {
            RenderedImage ri = coverage.getRenderedImage();
            disposed &= coverage.dispose(force);
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
        return disposed;
    }
}
