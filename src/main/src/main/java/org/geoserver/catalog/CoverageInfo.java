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

package org.geoserver.catalog;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.util.ProgressListener;

/**
 * A raster-based or coverage based resource.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @uml.dependency supplier="org.geoserver.catalog.CoverageResource"
 */
public interface CoverageInfo extends ResourceInfo {

    /**
     * The data store the feature type is a part of.
     *
     * <p>
     */
    CoverageStoreInfo getStore();

    /**
     * The native format of the coverage.
     *
     * @uml.property name="nativeFormat"
     */
    String getNativeFormat();

    /**
     * Sets the native format of the coverage.
     *
     * @uml.property name="nativeFormat"
     */
    void setNativeFormat(String nativeFormat);

    /**
     * The supported formats for the coverage.
     *
     * @uml.property name="supportedFormats"
     */
    List<String> getSupportedFormats();

    /**
     * The collection of identifiers of the crs's the coverage supports in a request.
     *
     * @uml.property name="requestSRS"
     */
    List<String> getRequestSRS();

    /**
     * The collection of identifiers of the crs's the coverage supports in a response.
     *
     * @uml.property name="responseSRS"
     */
    List<String> getResponseSRS();

    /**
     * The default interpolation method for hte coverage.
     *
     * @uml.property name="defaultInterpolationMethod"
     */
    String getDefaultInterpolationMethod();

    /**
     * Sets the default interpolation method for the coverage.
     *
     * @uml.property name="defaultInterpolationMethod"
     */
    void setDefaultInterpolationMethod(String defaultInterpolationMethod);

    /**
     * The collection of interpolation methods available for the coverage.
     *
     * @uml.property name="interpolationMethods"
     */
    List<String> getInterpolationMethods();

    /**
     * A map of coverage specific parameters.
     *
     * @uml.property name="parameters"
     */
    Map<String, Serializable> getParameters();

    /**
     * The dimensions of the coverage.
     *
     * @uml.property name="dimensions"
     * @uml.associationEnd multiplicity="(0 -1)" container="java.util.List"
     *     inverse="coverageInfo:org.geoserver.catalog.CoverageDimension"
     */
    List<CoverageDimensionInfo> getDimensions();

    /** The grid geometry. */
    GridGeometry getGrid();

    /** Sets the grid geometry. */
    void setGrid(GridGeometry grid);

    /**
     * Returns the underlying grid coverage instance.
     *
     * <p>This method does I/O and is potentially blocking. The <tt>listener</tt> may be used to
     * report the progress of loading the coverage and also to report any errors or warnings that
     * occur.
     *
     * @param listener A progress listener, may be <code>null</code>.
     * @param hints Hints to be used when loading the coverage.
     * @return The grid coverage.
     * @throws IOException Any I/O problems.
     */
    GridCoverage getGridCoverage(ProgressListener listener, Hints hints) throws IOException;

    GridCoverage getGridCoverage(
            ProgressListener listener, ReferencedEnvelope envelope, Hints hints) throws IOException;

    GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException;

    /** Returns the native coverage name (might be null for single coverage formats) */
    String getNativeCoverageName();

    /** Sets the native coverage name (used to pick up a specific coverage from withing a reader) */
    void setNativeCoverageName(String nativeCoverageName);
}
