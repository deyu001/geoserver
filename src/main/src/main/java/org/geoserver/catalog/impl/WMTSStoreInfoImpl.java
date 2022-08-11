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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geotools.ows.wmts.WebMapTileServer;
import org.opengis.util.ProgressListener;

@SuppressWarnings("serial")
public class WMTSStoreInfoImpl extends StoreInfoImpl implements WMTSStoreInfo {

    public static final int DEFAULT_MAX_CONNECTIONS = 6;

    public static final int DEFAULT_CONNECT_TIMEOUT = 30;

    public static final int DEFAULT_READ_TIMEOUT = 60;

    String capabilitiesURL;
    private String user;
    private String password;
    private int maxConnections;
    private int readTimeout;
    private int connectTimeout;

    // Map<String, String> headers;
    private String headerName; // todo: replace with Map<String, String>
    private String headerValue; // todo: replace with Map<String, String>

    protected WMTSStoreInfoImpl() {}

    public WMTSStoreInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public String getCapabilitiesURL() {
        return capabilitiesURL;
    }

    public void setCapabilitiesURL(String capabilitiesURL) {
        this.capabilitiesURL = capabilitiesURL;
    }

    @Override
    public String getUsername() {
        return user;
    }

    @Override
    public void setUsername(String user) {
        this.user = user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int getMaxConnections() {
        return maxConnections;
    }

    @Override
    public void setMaxConnections(int maxConcurrentConnections) {
        this.maxConnections = maxConcurrentConnections;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public void setReadTimeout(int timeoutSeconds) {
        this.readTimeout = timeoutSeconds;
    }

    @Override
    public int getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public void setConnectTimeout(int timeoutSeconds) {
        this.connectTimeout = timeoutSeconds;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public WebMapTileServer getWebMapTileServer(ProgressListener listener) throws IOException {
        Catalog catalog2 = getCatalog();
        ResourcePool resourcePool = catalog2.getResourcePool();
        WebMapTileServer webMapTileServer = resourcePool.getWebMapTileServer(this);
        return webMapTileServer;
    }

    @Override
    public boolean isUseConnectionPooling() {
        Boolean useConnectionPooling = getMetadata().get("useConnectionPooling", Boolean.class);
        return useConnectionPooling == null ? Boolean.TRUE : useConnectionPooling;
    }

    @Override
    public void setUseConnectionPooling(boolean useHttpConnectionPooling) {
        getMetadata().put("useConnectionPooling", Boolean.valueOf(useHttpConnectionPooling));
    }
}
