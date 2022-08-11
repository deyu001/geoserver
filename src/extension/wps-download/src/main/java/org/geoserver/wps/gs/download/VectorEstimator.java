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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Checks whether or not the provided request exceeds the provided download limits for a vectorial
 * resource.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
class VectorEstimator {

    private static final Logger LOGGER = Logging.getLogger(VectorEstimator.class);

    /** The downloadServiceConfiguration object containing the limits to check */
    private DownloadServiceConfiguration downloadServiceConfiguration;

    /**
     * Constructor.
     *
     * @param limits an instance of the {@link DownloadEstimatorProcess} that contains the limits to
     *     enforce
     */
    public VectorEstimator(DownloadServiceConfiguration limits) {
        this.downloadServiceConfiguration = limits;
    }

    /**
     * Checks whether or not the requests exceed download limits for vector data.
     *
     * @param resourceInfo the {@link FeatureTypeInfo} to download from
     * @param roi the {@link Geometry} for the clip/intersection
     * @param clip whether or not to clip the resulting data (useless for the moment)
     * @param filter the {@link Filter} to load the data
     * @param targetCRS the reproject {@link CoordinateReferenceSystem} (useless for the moment)
     * @return <code>true</code> if we do not exceeds the limits, <code>false</code> otherwise.
     * @throws Exception in case something bad happens.
     */
    public boolean execute(
            FeatureTypeInfo resourceInfo,
            Geometry roi,
            boolean clip,
            Filter filter,
            CoordinateReferenceSystem targetCRS,
            final ProgressListener progressListener)
            throws Exception {

        //
        // Do we need to do anything?
        //
        if (downloadServiceConfiguration.getMaxFeatures() <= 0) {
            return true;
        }

        // prepare native CRS
        CoordinateReferenceSystem nativeCRS = DownloadUtilities.getNativeCRS(resourceInfo);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Native CRS is " + nativeCRS.toWKT());
        }

        //
        // STEP 0 - Push ROI back to native CRS (if ROI is provided)
        //
        ROIManager roiManager = null;
        if (roi != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pushing ROI to native CRS");
            }
            CoordinateReferenceSystem roiCRS = (CoordinateReferenceSystem) roi.getUserData();
            roiManager = new ROIManager(roi, roiCRS);
            // set use nativeCRS
            roiManager.useNativeCRS(nativeCRS);
        }

        //
        // STEP 1 - Create the Filter
        //

        // access feature source and collection of features
        final SimpleFeatureSource featureSource =
                (SimpleFeatureSource)
                        resourceInfo.getFeatureSource(null, GeoTools.getDefaultHints());

        // basic filter preparation
        Filter ra = Filter.INCLUDE;
        if (filter != null) {
            ra = filter;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Using filter " + ra);
            }
        }
        // and with the ROI if we have one
        if (roi != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Adding Geometry filter with ROI");
            }
            final String dataGeomName =
                    featureSource.getSchema().getGeometryDescriptor().getLocalName();
            final Intersects intersectionFilter =
                    FeatureUtilities.DEFAULT_FILTER_FACTORY.intersects(
                            FeatureUtilities.DEFAULT_FILTER_FACTORY.property(dataGeomName),
                            FeatureUtilities.DEFAULT_FILTER_FACTORY.literal(
                                    roiManager.getSafeRoiInNativeCRS()));
            ra = FeatureUtilities.DEFAULT_FILTER_FACTORY.and(ra, intersectionFilter);
        }

        // simplify filter
        ra = (Filter) ra.accept(new SimplifyingFilterVisitor(), null);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Counting features");
        }
        // read
        int count = featureSource.getCount(new Query("counter", ra));
        if (count < 0) {
            // a value minor than "0" means that the store does not provide any counting feature ...
            // lets proceed using the iterator
            SimpleFeatureCollection features = featureSource.getFeatures(ra);
            count = features.size();
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Feature size is " + count);
        }
        // finally checking the number of features accordingly to the "maxfeatures" limit
        final long maxFeatures = downloadServiceConfiguration.getMaxFeatures();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Max features limit is " + maxFeatures);
        }
        if (maxFeatures > 0 && count > maxFeatures) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(
                        Level.SEVERE, "MaxFeatures limit exceeded. " + count + " > " + maxFeatures);
            }
            return false;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MaxFeatures limit not exceeded.");
        }
        // limits were not exceeded
        return true;
    }
}
