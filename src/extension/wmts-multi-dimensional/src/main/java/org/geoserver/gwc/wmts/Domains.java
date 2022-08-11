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

package org.geoserver.gwc.wmts;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

/**
 * Utility class for aggregating several dimensions. All the dimensions will share the same spatial
 * domain (bounding box), restrictions (filter) and resource.
 */
public class Domains {

    private final List<Dimension> dimensions;
    private final ReferencedEnvelope spatialDomain;
    private final Filter filter;

    private final LayerInfo layerInfo;
    private int expandLimit;
    private int maxReturnedValues;
    private SortOrder sortOrder;

    private String histogram;
    private String resolution;
    private String fromValue;

    public Domains(
            List<Dimension> dimensions,
            LayerInfo layerInfo,
            ReferencedEnvelope boundingBox,
            Filter filter) {
        this.dimensions = dimensions;
        this.layerInfo = layerInfo;
        this.spatialDomain = boundingBox;
        this.filter = filter;
    }

    public Domains withExpandLimit(int expandLimit) {
        this.expandLimit = expandLimit;
        return this;
    }

    public Domains withMaxReturnedValues(int maxReturnedValues) {
        this.maxReturnedValues = maxReturnedValues;
        return this;
    }

    public Domains withSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public Domains withFromValue(String fromValue) {
        this.fromValue = fromValue;
        return this;
    }

    /** The maximum number of returned values in a GetDomainValues request */
    public int getMaxReturnedValues() {
        return maxReturnedValues;
    }

    /** Returns the "fromValue" parameter in a GetDomainValues request */
    public String getFromValue() {
        return fromValue;
    }

    /** The sort direction in a GetDomainValues request */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public List<Dimension> getDimensions() {
        return dimensions;
    }

    ReferencedEnvelope getSpatialDomain() {
        return spatialDomain;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setHistogram(String histogram) {
        this.histogram = histogram;
    }

    void setResolution(String resolution) {
        this.resolution = resolution;
    }

    String getHistogramName() {
        return histogram;
    }

    public int getExpandLimit() {
        return expandLimit;
    }

    Tuple<String, List<Integer>> getHistogramValues() {
        for (Dimension dimension : dimensions) {
            if (dimension.getDimensionName().equalsIgnoreCase(histogram)) {
                return dimension.getHistogram(filter, resolution);
            }
        }
        throw new RuntimeException(String.format("Dimension '%s' could not be found.", histogram));
    }

    /** Returns the feature collection associated with these domains. */
    FeatureCollection getFeatureCollection() {
        ResourceInfo resourceInfo = layerInfo.getResource();
        try {
            if (resourceInfo instanceof FeatureTypeInfo) {
                // accessing the features of a vector
                return new FilteredFeatureType((FeatureTypeInfo) resourceInfo, filter)
                        .getFeatureSource(null, null)
                        .getFeatures();
            }
            // accessing the features of a raster
            return getFeatureCollection((CoverageInfo) resourceInfo);
        } catch (IOException exception) {
            throw new RuntimeException(
                    String.format("Error getting features of layer '%s'.", layerInfo.getName()),
                    exception);
        }
    }

    /** Helper method that just gets a feature collection from a raster. */
    private FeatureCollection getFeatureCollection(CoverageInfo typeInfo) throws IOException {
        GridCoverage2DReader reader =
                (GridCoverage2DReader) typeInfo.getGridCoverageReader(null, null);
        if (!(reader instanceof StructuredGridCoverage2DReader)) {
            throw new RuntimeException(
                    "Is not possible to obtain a feature collection from a non structured reader.");
        }
        StructuredGridCoverage2DReader structuredReader = (StructuredGridCoverage2DReader) reader;
        String coverageName = structuredReader.getGridCoverageNames()[0];
        GranuleSource source = structuredReader.getGranules(coverageName, true);
        Query query = new Query(source.getSchema().getName().getLocalPart());
        if (filter != null) {
            query.setFilter(filter);
        }
        return source.getGranules(query);
    }
}
