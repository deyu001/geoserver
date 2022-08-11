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

package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.geoserver.config.impl.ServiceInfoImpl;

public class WFSInfoImpl extends ServiceInfoImpl implements WFSInfo {

    protected Map<Version, GMLInfo> gml = new HashMap<>();
    protected ServiceLevel serviceLevel = ServiceLevel.COMPLETE;
    protected int maxFeatures = Integer.MAX_VALUE;
    protected boolean featureBounding = true;
    protected boolean canonicalSchemaLocation = false;
    protected boolean encodeFeatureMember = false;
    protected boolean hitsIgnoreMaxFeatures = false;
    protected boolean includeWFSRequestDumpFile = true;
    protected List<String> srs = new ArrayList<>();
    protected Boolean allowGlobalQueries = true;
    protected Boolean simpleConversionEnabled = false;

    public WFSInfoImpl() {}

    public Map<Version, GMLInfo> getGML() {
        return gml;
    }

    public void setGML(Map<Version, GMLInfo> gml) {
        this.gml = gml;
    }

    public ServiceLevel getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(ServiceLevel serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    public int getMaxFeatures() {
        return maxFeatures;
    }

    public void setFeatureBounding(boolean featureBounding) {
        this.featureBounding = featureBounding;
    }

    public boolean isFeatureBounding() {
        return featureBounding;
    }

    /** @see org.geoserver.wfs.WFSInfo#isCanonicalSchemaLocation() */
    public boolean isCanonicalSchemaLocation() {
        return canonicalSchemaLocation;
    }

    /** @see org.geoserver.wfs.WFSInfo#setCanonicalSchemaLocation(boolean) */
    public void setCanonicalSchemaLocation(boolean canonicalSchemaLocation) {
        this.canonicalSchemaLocation = canonicalSchemaLocation;
    }

    public void setIncludeWFSRequestDumpFile(boolean includeWFSRequestDumpFile) {
        this.includeWFSRequestDumpFile = includeWFSRequestDumpFile;
    }

    public boolean getIncludeWFSRequestDumpFile() {
        return includeWFSRequestDumpFile;
    }
    /*
     * @see org.geoserver.wfs.WFSInfo#isEncodingFeatureMember()
     */
    public boolean isEncodeFeatureMember() {
        return this.encodeFeatureMember;
    }

    /*
     * @see org.geoserver.wfs.WFSInfo#setEncodeFeatureMember(java.lang.Boolean)
     */
    public void setEncodeFeatureMember(boolean encodeFeatureMember) {
        this.encodeFeatureMember = encodeFeatureMember;
    }

    @Override
    public boolean isHitsIgnoreMaxFeatures() {
        return hitsIgnoreMaxFeatures;
    }

    @Override
    public void setHitsIgnoreMaxFeatures(boolean hitsIgnoreMaxFeatures) {
        this.hitsIgnoreMaxFeatures = hitsIgnoreMaxFeatures;
    }

    @Override
    public Integer getMaxNumberOfFeaturesForPreview() {
        Integer i = getMetadata().get("maxNumberOfFeaturesForPreview", Integer.class);
        return i != null ? i : 50;
    }

    @Override
    public void setMaxNumberOfFeaturesForPreview(Integer maxNumberOfFeaturesForPreview) {
        getMetadata().put("maxNumberOfFeaturesForPreview", maxNumberOfFeaturesForPreview);
    }

    public List<String> getSRS() {
        return srs;
    }

    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    @Override
    public Boolean getAllowGlobalQueries() {
        return allowGlobalQueries == null ? Boolean.TRUE : allowGlobalQueries;
    }

    @Override
    public void setAllowGlobalQueries(Boolean allowGlobalQueries) {
        this.allowGlobalQueries = allowGlobalQueries;
    }

    @Override
    public boolean isSimpleConversionEnabled() {
        return simpleConversionEnabled == null ? false : simpleConversionEnabled;
    }

    @Override
    public void setSimpleConversionEnabled(boolean simpleConversionEnabled) {
        this.simpleConversionEnabled = simpleConversionEnabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (canonicalSchemaLocation ? 1231 : 1237);
        result = prime * result + (encodeFeatureMember ? 1231 : 1237);
        result = prime * result + (featureBounding ? 1231 : 1237);
        result = prime * result + ((gml == null) ? 0 : gml.hashCode());
        result = prime * result + (hitsIgnoreMaxFeatures ? 1231 : 1237);
        result = prime * result + maxFeatures;
        result = prime * result + (includeWFSRequestDumpFile ? 1231 : 1237);
        result = prime * result + ((serviceLevel == null) ? 0 : serviceLevel.hashCode());
        result = prime * result + ((srs == null) ? 0 : srs.hashCode());
        result = prime * result + (allowGlobalQueries == null ? 0 : allowGlobalQueries.hashCode());
        result =
                prime * result
                        + (simpleConversionEnabled == null
                                ? 0
                                : simpleConversionEnabled.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof WFSInfo)) return false;
        final WFSInfo other = (WFSInfo) obj;
        if (gml == null) {
            if (other.getGML() != null) return false;
        } else if (!gml.equals(other.getGML())) return false;
        if (maxFeatures != other.getMaxFeatures()) return false;
        if (featureBounding != other.isFeatureBounding()) return false;
        if (canonicalSchemaLocation != other.isCanonicalSchemaLocation()) return false;
        if (serviceLevel == null) {
            if (other.getServiceLevel() != null) return false;
        } else if (!serviceLevel.equals(other.getServiceLevel())) return false;
        if (encodeFeatureMember != other.isEncodeFeatureMember()) return false;
        if (hitsIgnoreMaxFeatures != other.isHitsIgnoreMaxFeatures()) return false;
        if (includeWFSRequestDumpFile != other.getIncludeWFSRequestDumpFile()) return false;
        if (srs == null) {
            if (other.getSRS() != null) return false;
        } else if (!srs.equals(other.getSRS())) return false;
        if (allowGlobalQueries == null && other.getAllowGlobalQueries() != null
                || !Objects.equals(allowGlobalQueries, other.getAllowGlobalQueries())) {
            return false;
        }
        return true;
    }
}
