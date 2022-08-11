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

package org.geoserver.catalog.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.util.ProgressListener;

/**
 * Default Implementation of the {@link CoverageInfo} bean to capture information about a coverage.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
@SuppressWarnings("deprecation")
public class CoverageInfoImpl extends ResourceInfoImpl implements CoverageInfo {

    /** */
    private static final long serialVersionUID = 659498790758954330L;

    protected String nativeFormat;

    protected GridGeometry grid;

    protected List<String> supportedFormats = new ArrayList<>();

    protected List<String> interpolationMethods = new ArrayList<>();

    protected String defaultInterpolationMethod;

    protected List<CoverageDimensionInfo> dimensions = new ArrayList<>();

    protected List<String> requestSRS = new ArrayList<>();

    protected List<String> responseSRS = new ArrayList<>();

    protected Map<String, Serializable> parameters = new HashMap<>();

    protected String nativeCoverageName;

    protected CoverageInfoImpl() {}

    public CoverageInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public CoverageInfoImpl(Catalog catalog, String id) {
        super(catalog, id);
    }

    public CoverageStoreInfo getStore() {
        return (CoverageStoreInfo) super.getStore();
    }

    public GridGeometry getGrid() {
        return grid;
    }

    public void setGrid(GridGeometry grid) {
        this.grid = grid;
    }

    public String getNativeFormat() {
        return nativeFormat;
    }

    public void setNativeFormat(String nativeFormat) {
        this.nativeFormat = nativeFormat;
    }

    public List<String> getSupportedFormats() {
        return supportedFormats;
    }

    public List<String> getInterpolationMethods() {
        return interpolationMethods;
    }

    public String getDefaultInterpolationMethod() {
        return defaultInterpolationMethod;
    }

    public void setDefaultInterpolationMethod(String defaultInterpolationMethod) {
        this.defaultInterpolationMethod = defaultInterpolationMethod;
    }

    public List<CoverageDimensionInfo> getDimensions() {
        return dimensions;
    }

    public List<String> getRequestSRS() {
        return requestSRS;
    }

    public List<String> getResponseSRS() {
        return responseSRS;
    }

    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Serializable> parameters) {
        this.parameters = parameters;
    }

    public GridCoverage getGridCoverage(ProgressListener listener, Hints hints) throws IOException {

        // manage projection policy
        if (this.projectionPolicy == ProjectionPolicy.FORCE_DECLARED) {
            final Hints crsHints =
                    new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, this.getCRS());
            if (hints != null) hints.putAll(crsHints);
            else hints = crsHints;
        }
        return catalog.getResourcePool().getGridCoverage(this, null, hints);
    }

    public GridCoverage getGridCoverage(
            ProgressListener listener, ReferencedEnvelope envelope, Hints hints)
            throws IOException {
        // manage projection policy
        if (this.projectionPolicy == ProjectionPolicy.FORCE_DECLARED) {
            final Hints crsHints =
                    new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, this.getCRS());
            if (hints != null) hints.putAll(crsHints);
            else hints = crsHints;
        }
        return catalog.getResourcePool().getGridCoverage(this, envelope, hints);
    }

    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException {
        // manage projection policy
        if (this.projectionPolicy == ProjectionPolicy.FORCE_DECLARED) {
            final Hints crsHints =
                    new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, this.getCRS());
            if (hints != null) hints.putAll(crsHints);
            else hints = crsHints;
        }
        return catalog.getResourcePool().getGridCoverageReader(this, nativeCoverageName, hints);
    }

    public void setSupportedFormats(List<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    public void setInterpolationMethods(List<String> interpolationMethods) {
        this.interpolationMethods = interpolationMethods;
    }

    public void setDimensions(List<CoverageDimensionInfo> dimensions) {
        this.dimensions = dimensions;
    }

    public void setRequestSRS(List<String> requestSRS) {
        this.requestSRS = requestSRS;
    }

    public void setResponseSRS(List<String> responseSRS) {
        this.responseSRS = responseSRS;
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
                prime * result
                        + ((defaultInterpolationMethod == null)
                                ? 0
                                : defaultInterpolationMethod.hashCode());
        result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
        result = prime * result + ((grid == null) ? 0 : grid.hashCode());
        result =
                prime * result
                        + ((interpolationMethods == null) ? 0 : interpolationMethods.hashCode());
        result = prime * result + ((nativeFormat == null) ? 0 : nativeFormat.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((requestSRS == null) ? 0 : requestSRS.hashCode());
        result = prime * result + ((responseSRS == null) ? 0 : responseSRS.hashCode());
        result = prime * result + ((supportedFormats == null) ? 0 : supportedFormats.hashCode());
        result =
                prime * result + ((nativeCoverageName == null) ? 0 : nativeCoverageName.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CoverageInfo)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }

        final CoverageInfo other = (CoverageInfo) obj;
        if (defaultInterpolationMethod == null) {
            if (other.getDefaultInterpolationMethod() != null) return false;
        } else if (!defaultInterpolationMethod.equals(other.getDefaultInterpolationMethod()))
            return false;
        if (dimensions == null) {
            if (other.getDimensions() != null) return false;
        } else if (!dimensions.equals(other.getDimensions())) return false;
        if (grid == null) {
            if (other.getGrid() != null) return false;
        } else if (!grid.equals(other.getGrid())) return false;
        if (interpolationMethods == null) {
            if (other.getInterpolationMethods() != null) return false;
        } else if (!interpolationMethods.equals(other.getInterpolationMethods())) return false;
        if (nativeFormat == null) {
            if (other.getNativeFormat() != null) return false;
        } else if (!nativeFormat.equals(other.getNativeFormat())) return false;
        if (parameters == null) {
            if (other.getParameters() != null) return false;
        } else if (!parameters.equals(other.getParameters())) return false;
        if (requestSRS == null) {
            if (other.getRequestSRS() != null) return false;
        } else if (!requestSRS.equals(other.getRequestSRS())) return false;
        if (responseSRS == null) {
            if (other.getResponseSRS() != null) return false;
        } else if (!responseSRS.equals(other.getResponseSRS())) return false;
        if (supportedFormats == null) {
            if (other.getSupportedFormats() != null) return false;
        } else if (!supportedFormats.equals(other.getSupportedFormats())) return false;
        if (nativeCoverageName == null) {
            if (other.getNativeCoverageName() != null) return false;
        } else if (!nativeCoverageName.equals(other.getNativeCoverageName())) return false;
        return true;
    }

    public String getNativeCoverageName() {
        return nativeCoverageName;
    }

    public void setNativeCoverageName(String nativeCoverageName) {
        this.nativeCoverageName = nativeCoverageName;
    }
}
