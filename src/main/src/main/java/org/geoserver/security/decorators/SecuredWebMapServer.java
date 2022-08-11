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

package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.logging.Level;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.data.ows.GetCapabilitiesRequest;
import org.geotools.data.ows.GetCapabilitiesResponse;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.request.DescribeLayerRequest;
import org.geotools.ows.wms.request.GetFeatureInfoRequest;
import org.geotools.ows.wms.request.GetLegendGraphicRequest;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.ows.wms.request.GetStylesRequest;
import org.geotools.ows.wms.request.PutStylesRequest;
import org.geotools.ows.wms.response.DescribeLayerResponse;
import org.geotools.ows.wms.response.GetFeatureInfoResponse;
import org.geotools.ows.wms.response.GetLegendGraphicResponse;
import org.geotools.ows.wms.response.GetMapResponse;
import org.geotools.ows.wms.response.GetStylesResponse;
import org.geotools.ows.wms.response.PutStylesResponse;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Applies security around the web map server
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredWebMapServer extends WebMapServer {

    WebMapServer delegate;

    public SecuredWebMapServer(WebMapServer delegate) throws IOException, ServiceException {
        super(delegate.getCapabilities());
        this.delegate = delegate;
    }

    public GetFeatureInfoRequest createGetFeatureInfoRequest(GetMapRequest getMapRequest) {
        return new SecuredGetFeatureInfoRequest(
                delegate.createGetFeatureInfoRequest(getMapRequest), getMapRequest);
    }

    public GetMapRequest createGetMapRequest() {
        return new SecuredGetMapRequest(delegate.createGetMapRequest());
    }

    // -------------------------------------------------------------------------------------------
    //
    // Purely delegated methods
    //
    // -------------------------------------------------------------------------------------------

    public GetStylesResponse issueRequest(GetStylesRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    public PutStylesResponse issueRequest(PutStylesRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    public GetLegendGraphicResponse issueRequest(GetLegendGraphicRequest request)
            throws IOException, ServiceException {

        return delegate.issueRequest(request);
    }

    public DescribeLayerResponse issueRequest(DescribeLayerRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    public GetCapabilitiesResponse issueRequest(GetCapabilitiesRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    public GetFeatureInfoResponse issueRequest(GetFeatureInfoRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    public GetMapResponse issueRequest(GetMapRequest request) throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    public DescribeLayerRequest createDescribeLayerRequest() throws UnsupportedOperationException {
        return delegate.createDescribeLayerRequest();
    }

    public GetLegendGraphicRequest createGetLegendGraphicRequest()
            throws UnsupportedOperationException {
        return delegate.createGetLegendGraphicRequest();
    }

    public GetStylesRequest createGetStylesRequest() throws UnsupportedOperationException {
        return delegate.createGetStylesRequest();
    }

    public PutStylesRequest createPutStylesRequest() throws UnsupportedOperationException {
        return delegate.createPutStylesRequest();
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public WMSCapabilities getCapabilities() {
        return delegate.getCapabilities();
    }

    public GeneralEnvelope getEnvelope(Layer layer, CoordinateReferenceSystem crs) {
        return delegate.getEnvelope(layer, crs);
    }

    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    public ResourceInfo getInfo(Layer resource) {
        return delegate.getInfo(resource);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public void setLoggingLevel(Level newLevel) {
        delegate.setLoggingLevel(newLevel);
    }

    public String toString() {
        return "SecuredWebMapServer " + delegate.toString();
    }
}
