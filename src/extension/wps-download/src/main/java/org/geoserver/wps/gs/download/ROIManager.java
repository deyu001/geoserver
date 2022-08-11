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

package org.geoserver.wps.gs.download;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * This class is used for managing ROI and its CRS. ROIManager provides utility method like
 * reprojecting the ROI in the desired CRS.
 *
 * @author Simone Giannecchini, GeoSolutions
 */
final class ROIManager {

    private static final Logger LOGGER = Logging.getLogger(ROIManager.class);

    /** Input Geometry */
    final Geometry originalRoi;

    /** ROI reprojected in the input ROI CRS */
    Geometry roiInNativeCRS;

    /** ROI reprojected in the native ROI CRS (reduced to envelope if possible) */
    Geometry safeRoiInNativeCRS;

    /** ROI native CRS */
    CoordinateReferenceSystem nativeCRS;

    /** ROI reprojected in the target CRS */
    Geometry roiInTargetCRS;

    /** ROI reprojected in the target CRS (reduced to envelope if possible) */
    Geometry safeRoiInTargetCRS;

    /** Initial ROI CRS */
    final CoordinateReferenceSystem roiCRS;

    /** ROI target CRS */
    CoordinateReferenceSystem targetCRS;

    /** Boolean indicating if the ROI is a BBOX */
    final boolean isROIBBOX;

    /** Boolean indicating if the roiCRS equals the targetCRS */
    boolean roiCrsEqualsTargetCrs = true;

    /**
     * Constructor.
     *
     * @param roi original ROI as a JTS geometry
     * @param roiCRS {@link CoordinateReferenceSystem} for the provided geometry. If this is null
     *     the CRS must be provided with the USerData of the roi
     */
    public ROIManager(Geometry roi, CoordinateReferenceSystem roiCRS) {
        this.originalRoi = roi;
        DownloadUtilities.checkPolygonROI(roi);
        // Check ROI CRS
        if (roiCRS == null) {
            if (!(roi.getUserData() instanceof CoordinateReferenceSystem)) {
                throw new IllegalArgumentException("ROI without a CRS is not usable!");
            }
            this.roiCRS = (CoordinateReferenceSystem) roi.getUserData();
        } else {
            this.roiCRS = roiCRS;
        }
        roi.setUserData(this.roiCRS);
        // is this a bbox
        isROIBBOX = roi.isRectangle();
    }

    /**
     * Reproject the initial roi to the provided CRS which is supposedly the native CRS of the data
     * to clip.
     *
     * @param nativeCRS a valid instance of {@link CoordinateReferenceSystem}
     * @throws IOException in case something bad happens.
     */
    public void useNativeCRS(final CoordinateReferenceSystem nativeCRS) throws IOException {
        if (nativeCRS == null) {
            throw new IllegalArgumentException("The provided nativeCRS is null");
        }
        roiInNativeCRS = DownloadUtilities.transformGeometry(originalRoi, nativeCRS);
        DownloadUtilities.checkPolygonROI(roiInNativeCRS);
        if (isROIBBOX) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "ROI is a Bounding Box");
            }
            // if the ROI is a BBOX we tend to preserve the fact that it is a BBOX
            safeRoiInNativeCRS = roiInNativeCRS.getEnvelope();
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "ROI is not a Bounding Box");
            }
            safeRoiInNativeCRS = roiInNativeCRS;
        }
        safeRoiInNativeCRS.setUserData(nativeCRS);
        this.nativeCRS = nativeCRS;
    }

    /**
     * Reproject the initial roi to the provided CRS which is supposedly the target CRS as per the
     * request.
     *
     * <p>This method should be called once the native CRS has been set, that is the {@link
     * #useNativeCRS(CoordinateReferenceSystem)} has been called.
     *
     * @param targetCRS a valid instance of {@link CoordinateReferenceSystem}
     * @throws IOException in case something bad happens.
     */
    public void useTargetCRS(final CoordinateReferenceSystem targetCRS)
            throws IOException, FactoryException {
        if (targetCRS == null) {
            throw new IllegalArgumentException("The provided targetCRS is null");
        }
        if (roiInNativeCRS == null) {
            throw new IllegalStateException("It looks like useNativeCRS has not been called yet");
        }
        this.targetCRS = targetCRS;
        if (!CRS.equalsIgnoreMetadata(roiCRS, targetCRS)) {

            MathTransform reprojectionTrasform = CRS.findMathTransform(roiCRS, targetCRS, true);
            if (!reprojectionTrasform.isIdentity()) {
                // avoid doing the transform if this is the identity
                roiCrsEqualsTargetCrs = false;
            }
        }

        if (isROIBBOX) {
            // we need to use a larger bbox in native CRS
            roiInTargetCRS = DownloadUtilities.transformGeometry(safeRoiInNativeCRS, targetCRS);
            DownloadUtilities.checkPolygonROI(roiInTargetCRS);
            safeRoiInTargetCRS = roiInTargetCRS.getEnvelope();
            safeRoiInTargetCRS.setUserData(targetCRS);

            // Back to the minimal roiInTargetCrs for future clipping if needed.
            roiInTargetCRS =
                    roiCrsEqualsTargetCrs
                            ? originalRoi
                            : DownloadUtilities.transformGeometry(originalRoi, targetCRS);

            // touch safeRoiInNativeCRS
            safeRoiInNativeCRS = DownloadUtilities.transformGeometry(safeRoiInTargetCRS, nativeCRS);
            DownloadUtilities.checkPolygonROI(safeRoiInNativeCRS);
            safeRoiInNativeCRS = safeRoiInNativeCRS.getEnvelope();
            safeRoiInNativeCRS.setUserData(nativeCRS);
        } else {
            roiInTargetCRS = DownloadUtilities.transformGeometry(roiInNativeCRS, targetCRS);
            safeRoiInTargetCRS = roiInTargetCRS;
        }
    }

    /** @return the isBBOX */
    public boolean isROIBBOX() {
        return isROIBBOX;
    }

    /** @return the roi */
    public Geometry getOriginalRoi() {
        return originalRoi;
    }

    /** @return the roiInNativeCRS */
    public Geometry getRoiInNativeCRS() {
        return roiInNativeCRS;
    }

    /** @return the safeRoiInNativeCRS */
    public Geometry getSafeRoiInNativeCRS() {
        return safeRoiInNativeCRS;
    }

    /** @return the roiInTargetCRS */
    public Geometry getRoiInTargetCRS() {
        return roiInTargetCRS;
    }

    /** @return the safeRoiInTargetCRS */
    public Geometry getSafeRoiInTargetCRS() {
        return safeRoiInTargetCRS;
    }

    /** @return the roiCRS */
    public CoordinateReferenceSystem getRoiCRS() {
        return roiCRS;
    }

    /** @return the targetCRS */
    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    public CoordinateReferenceSystem getNativeCRS() {
        return nativeCRS;
    }

    public boolean isRoiCrsEqualsTargetCrs() {
        return roiCrsEqualsTargetCrs;
    }

    public Geometry getTargetRoi(boolean clip) {
        if (clip) {
            // clipping means carefully following the ROI shape
            return isRoiCrsEqualsTargetCrs() ? originalRoi : getRoiInTargetCRS();
        } else {
            // use envelope of the ROI to simply crop and not clip the raster. This is important
            // since when reprojecting we might read a bit more than needed!
            return isRoiCrsEqualsTargetCrs()
                    ? originalRoi.getEnvelope()
                    : getSafeRoiInTargetCRS().getEnvelope();
        }
    }
}
