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
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.ows.Response;
import org.geotools.http.HTTPResponse;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.request.GetFeatureInfoRequest;
import org.geotools.ows.wms.request.GetMapRequest;

/**
 * Wraps a GetFeatureInfo request enforcing GetFeatureInfo limits for each of the layers
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredGetFeatureInfoRequest implements GetFeatureInfoRequest {

    List<Layer> queryLayers = new ArrayList<>();
    GetFeatureInfoRequest delegate;
    int x;
    int y;
    GetMapRequest getMap;

    public SecuredGetFeatureInfoRequest(GetFeatureInfoRequest delegate, GetMapRequest getMap) {
        super();
        this.delegate = delegate;
        this.getMap = getMap;
    }

    public void addQueryLayer(Layer layer) {
        queryLayers.add(layer);
    }

    public void setQueryLayers(Set<Layer> layers) {
        queryLayers.clear();
        queryLayers.addAll(layers);
    }

    public void setQueryPoint(int x, int y) {
        this.x = x;
        this.y = y;
        delegate.setQueryPoint(x, y);
    }

    public URL getFinalURL() {
        // scan and check the layers
        for (Layer layer : queryLayers) {
            if (layer instanceof SecuredWMSLayer) {
                SecuredWMSLayer secured = (SecuredWMSLayer) layer;
                final WrapperPolicy policy = secured.getPolicy();
                // check if we can cascade GetFeatureInfo
                if (policy.getLimits() instanceof WMSAccessLimits) {
                    WMSAccessLimits limits = (WMSAccessLimits) policy.getLimits();
                    if (!limits.isAllowFeatureInfo()) {
                        if (policy.getResponse() == org.geoserver.security.Response.CHALLENGE) {
                            SecureCatalogImpl.unauthorizedAccess(layer.getName());
                        } else {
                            throw new IllegalArgumentException(
                                    "Layer " + layer.getName() + " is not queriable");
                        }
                    }
                }

                // add into the request
                delegate.addQueryLayer(layer);
            }
        }

        // add the cql filters
        if (getMap instanceof SecuredGetMapRequest) {
            SecuredGetMapRequest sgm = (SecuredGetMapRequest) getMap;
            String encodedFilter = sgm.buildCQLFilter();
            if (encodedFilter != null) {
                delegate.setProperty("CQL_FILTER", encodedFilter);
            }
        }

        return delegate.getFinalURL();
    }

    // ----------------------------------------------------------------------------------------
    // Pure delegate methods
    // ----------------------------------------------------------------------------------------

    public Response createResponse(HTTPResponse response) throws ServiceException, IOException {
        return delegate.createResponse(response);
    }

    public String getPostContentType() {
        return delegate.getPostContentType();
    }

    public Properties getProperties() {
        return delegate.getProperties();
    }

    public void performPostOutput(OutputStream outputStream) throws IOException {
        delegate.performPostOutput(outputStream);
    }

    public boolean requiresPost() {
        return delegate.requiresPost();
    }

    public void setFeatureCount(int featureCount) {
        delegate.setFeatureCount(featureCount);
    }

    public void setFeatureCount(String featureCount) {
        delegate.setFeatureCount(featureCount);
    }

    public void setInfoFormat(String infoFormat) {
        delegate.setInfoFormat(infoFormat);
    }

    public void setProperty(String name, String value) {
        delegate.setProperty(name, value);
    }
}
