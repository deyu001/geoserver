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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geoserver.wms.describelayer.XMLDescribeLayerResponse;

/**
 * Holds the pre-validated parameters of a <code>DescribeLayer</code> request.
 *
 * <p>This pre-validation must to be done by the request reader, so the content of this object is
 * assumed to be valid.
 *
 * @author Gabriel Roldan
 * @author Carlo Cancellieri
 * @version $Id$
 */
public class DescribeLayerRequest extends WMSRequest {

    /** Holds the FeatureTypes parsed from the request's <code>LAYERS</code> parameter. */
    private List<MapLayerInfo> layers = new ArrayList<>(2);

    /**
     * Holds the GetMap part of the GetFeatureInfo request, which is meant to provide enough context
     * information about the map over the DescribeLayer request is being made.
     */
    private GetMapRequest getMapRequest;

    /**
     * Holder for the optional <code>EXCEPTIONS</code> parameter, defaults to <code>
     * "application/vnd.ogc.se_xml"</code>
     */
    private static final String DEFAULT_EXCEPTION_FORMAT = "application/vnd.ogc.se_xml";

    private String exeptionFormat = DEFAULT_EXCEPTION_FORMAT;

    /** Holder for the <code>outputFormat</code> optional parameter */
    private String outputFormat = XMLDescribeLayerResponse.DESCLAYER_MIME_TYPE;

    public GetMapRequest getGetMapRequest() {
        return getMapRequest;
    }

    public void setGetMapRequest(GetMapRequest getMapRequest) {
        this.getMapRequest = getMapRequest;
    }

    public String getExeptionFormat() {
        return exeptionFormat;
    }

    public void setExeptionFormat(String exeptionFormat) {
        this.exeptionFormat = exeptionFormat;
    }

    public DescribeLayerRequest() {
        super("DescribeLayer");
    }

    public void addLayer(MapLayerInfo layer) {
        if (layer == null) {
            throw new NullPointerException();
        }

        layers.add(layer);
    }

    public List<MapLayerInfo> getLayers() {
        return new ArrayList<>(layers);
    }

    public void setLayers(List<MapLayerInfo> layers) {
        this.layers = layers;
    }

    /** @return Returns the describeFormat. */
    public String getOutputFormat() {
        return outputFormat;
    }

    /** @param outputFormat The describeFormat to set. */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("DescribeLayerRequest[layers=");

        for (Iterator<MapLayerInfo> it = layers.iterator(); it.hasNext(); ) {
            sb.append(it.next().getName());

            if (it.hasNext()) {
                sb.append(',');
            }
        }

        sb.append(']');

        return sb.toString();
    }
}
