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

package org.geoserver.wcs;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geotools.coverage.grid.io.OverviewPolicy;

/**
 * Default implementation for the {@link WCSInfo} bean.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
@SuppressWarnings("unchecked")
public class WCSInfoImpl extends ServiceInfoImpl implements WCSInfo {

    private static final long serialVersionUID = 3721044439071286273L;

    List<String> srs = new ArrayList<>();

    boolean gmlPrefixing;

    private boolean latLon = false;

    long maxInputMemory = -1;

    long maxOutputMemory = -1;

    Boolean subsamplingEnabled = Boolean.TRUE;

    OverviewPolicy overviewPolicy;

    Integer maxRequestedDimensionValues;

    public WCSInfoImpl() {}

    public boolean isGMLPrefixing() {
        return gmlPrefixing;
    }

    public void setGMLPrefixing(boolean gmlPrefixing) {
        this.gmlPrefixing = gmlPrefixing;
    }

    public long getMaxInputMemory() {
        return maxInputMemory;
    }

    public void setMaxInputMemory(long maxInputSize) {
        this.maxInputMemory = maxInputSize;
    }

    public long getMaxOutputMemory() {
        return maxOutputMemory;
    }

    public void setMaxOutputMemory(long maxOutputSize) {
        this.maxOutputMemory = maxOutputSize;
    }

    public boolean isSubsamplingEnabled() {
        return subsamplingEnabled == null ? true : subsamplingEnabled;
    }

    public void setSubsamplingEnabled(boolean subsamplingEnabled) {
        this.subsamplingEnabled = subsamplingEnabled;
    }

    public OverviewPolicy getOverviewPolicy() {
        return overviewPolicy == null ? OverviewPolicy.IGNORE : overviewPolicy;
    }

    public void setOverviewPolicy(OverviewPolicy overviewPolicy) {
        this.overviewPolicy = overviewPolicy;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public void setLatLon(boolean latLon) {
        this.latLon = latLon;
    }

    @Override
    public boolean isLatLon() {
        return latLon;
    }

    public List<String> getSRS() {
        return srs;
    }

    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (gmlPrefixing ? 1231 : 1237);
        result = prime * result + (latLon ? 1231 : 1237);
        result = prime * result + (int) (maxInputMemory ^ (maxInputMemory >>> 32));
        result = prime * result + (int) (maxOutputMemory ^ (maxOutputMemory >>> 32));
        result = prime * result + ((overviewPolicy == null) ? 0 : overviewPolicy.hashCode());
        result = prime * result + ((srs == null) ? 0 : srs.hashCode());
        result =
                prime * result + ((subsamplingEnabled == null) ? 0 : subsamplingEnabled.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        WCSInfoImpl other = (WCSInfoImpl) obj;
        if (gmlPrefixing != other.gmlPrefixing) return false;
        if (latLon != other.latLon) return false;
        if (maxInputMemory != other.maxInputMemory) return false;
        if (maxOutputMemory != other.maxOutputMemory) return false;
        if (overviewPolicy != other.overviewPolicy) return false;
        if (srs == null) {
            if (other.srs != null) return false;
        } else if (!srs.equals(other.srs)) return false;
        if (subsamplingEnabled == null) {
            if (other.subsamplingEnabled != null) return false;
        } else if (!subsamplingEnabled.equals(other.subsamplingEnabled)) return false;
        return true;
    }

    public int getMaxRequestedDimensionValues() {
        return maxRequestedDimensionValues == null
                ? DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES
                : maxRequestedDimensionValues;
    }

    public void setMaxRequestedDimensionValues(int maxRequestedDimensionValues) {
        this.maxRequestedDimensionValues = maxRequestedDimensionValues;
    }
}
