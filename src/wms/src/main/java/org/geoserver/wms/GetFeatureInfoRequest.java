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

package org.geoserver.wms;

import java.util.List;

/**
 * Represents a WMS 1.1.1 GetFeatureInfo request.
 *
 * <p>The "GetMap" part of the request is represented by a <code>GetMapRequest</code> object by
 * itself. It is intended to provide enough map context information about the map over the
 * GetFeatureInfo request is performed.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetFeatureInfoRequest extends WMSRequest {
    private static final String DEFAULT_EXCEPTION_FORMAT = "application/vnd.ogc.se_xml";

    private static final int DEFAULT_MAX_FEATURES = 1;

    /**
     * Holds the GetMap part of the GetFeatureInfo request, wich is meant to provide enough context
     * information about the map over the GetFeatureInfo request is being made.
     */
    private GetMapRequest getMapRequest;

    /** List of FeatureTypeInfo's parsed from the <code>QUERY_LAYERS</code> mandatory parameter. */
    private List<MapLayerInfo> queryLayers;

    /** Holder for the <code>INFO_FORMAT</code> optional parameter */
    private String infoFormat;

    /** Holder for the <code>FEATURE_COUNT</code> optional parameter. Deafults to 1. */
    private int featureCount = DEFAULT_MAX_FEATURES;

    /** Holds the value of the required <code>X</code> parameter */
    private int XPixel;

    /** Holds the value of the requiered <code>Y</code> parameter */
    private int YPixel;

    /** Property selection, if any (one list per layer) */
    private List<List<String>> propertyNames;

    /**
     * Holder for the optional <code>EXCEPTIONS</code> parameter, defaults to <code>
     * "application/vnd.ogc.se_xml"</code>
     */
    private String exceptionFormat = DEFAULT_EXCEPTION_FORMAT;

    /** Optional parameter to exclude nodata values from the results */
    private boolean excludeNodataResults = false;

    public GetFeatureInfoRequest() {
        super("GetFeatureInfo");
    }

    /** @return Returns the exceptionFormat. */
    public String getExceptions() {
        return exceptionFormat;
    }

    /** @param exceptionFormat The exceptionFormat to set. */
    public void setExceptions(String exceptionFormat) {
        this.exceptionFormat = exceptionFormat;
    }

    /** @return Returns the featureCount. */
    public int getFeatureCount() {
        return featureCount;
    }

    /** @param featureCount The featureCount to set. */
    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
    }

    /** @return Returns the excludeNodataResults field */
    public boolean isExcludeNodataResults() {
        return excludeNodataResults;
    }

    /** @param excludeNodataResults Whether to exclude nodata results or not */
    public void setExcludeNodataResults(boolean excludeNodataResults) {
        this.excludeNodataResults = excludeNodataResults;
    }

    /** @return Returns the getMapRequest. */
    public GetMapRequest getGetMapRequest() {
        return getMapRequest;
    }

    /** @param getMapRequest The getMapRequest to set. */
    public void setGetMapRequest(GetMapRequest getMapRequest) {
        this.getMapRequest = getMapRequest;
    }

    /** @return Returns the infoFormat. */
    public String getInfoFormat() {
        return infoFormat;
    }

    /** @param infoFormat The infoFormat to set. */
    public void setInfoFormat(String infoFormat) {
        this.infoFormat = infoFormat;
    }

    /** @return Returns the queryLayers. */
    public List<MapLayerInfo> getQueryLayers() {
        return queryLayers;
    }

    /** @param queryLayers The queryLayers to set. */
    public void setQueryLayers(List<MapLayerInfo> queryLayers) {
        this.queryLayers = queryLayers;
    }

    /** @return Returns the xPixel. */
    public int getXPixel() {
        return XPixel;
    }

    /** @param pixel The xPixel to set. */
    public void setXPixel(int pixel) {
        XPixel = pixel;
    }

    /** @return Returns the yPixel. */
    public int getYPixel() {
        return YPixel;
    }

    /** @param pixel The yPixel to set. */
    public void setYPixel(int pixel) {
        YPixel = pixel;
    }

    /** The property selection, if any */
    public List<List<String>> getPropertyNames() {
        return propertyNames;
    }

    /** Sets the property selection */
    public void setPropertyNames(List<List<String>> propertyNames) {
        this.propertyNames = propertyNames;
    }
}
