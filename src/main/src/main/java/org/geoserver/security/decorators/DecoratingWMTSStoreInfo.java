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
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.util.ProgressListener;

/**
 * Delegates every method to the delegate wmts store info.
 *
 * <p>Subclasses will override selected methods to perform their "decoration" job.
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class DecoratingWMTSStoreInfo extends AbstractDecorator<WMTSStoreInfo>
        implements WMTSStoreInfo {

    public DecoratingWMTSStoreInfo(WMTSStoreInfo delegate) {
        super(delegate);
    }

    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return delegate.getAdapter(adapterClass, hints);
    }

    public String getCapabilitiesURL() {
        return delegate.getCapabilitiesURL();
    }

    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    public Map<String, Serializable> getConnectionParameters() {
        return delegate.getConnectionParameters();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public Throwable getError() {
        return delegate.getError();
    }

    public String getId() {
        return delegate.getId();
    }

    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    public String getName() {
        return delegate.getName();
    }

    public String getType() {
        return delegate.getType();
    }

    public WorkspaceInfo getWorkspace() {
        return delegate.getWorkspace();
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public void setCapabilitiesURL(String url) {
        delegate.setCapabilitiesURL(url);
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void setError(Throwable t) {
        delegate.setError(t);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setType(String type) {
        delegate.setType(type);
    }

    public void setWorkspace(WorkspaceInfo workspace) {
        delegate.setWorkspace(workspace);
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public void setUsername(String user) {
        delegate.setUsername(user);
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public void setPassword(String password) {
        delegate.setPassword(password);
    }

    @Override
    public int getMaxConnections() {
        return delegate.getMaxConnections();
    }

    @Override
    public void setMaxConnections(int maxConcurrentConnections) {
        delegate.setMaxConnections(maxConcurrentConnections);
    }

    public int getReadTimeout() {
        return delegate.getReadTimeout();
    }

    public void setReadTimeout(int timeoutSeconds) {
        delegate.setReadTimeout(timeoutSeconds);
    }

    public int getConnectTimeout() {
        return delegate.getConnectTimeout();
    }

    public void setConnectTimeout(int timeoutSeconds) {
        delegate.setConnectTimeout(timeoutSeconds);
    }

    public boolean isUseConnectionPooling() {
        return delegate.isUseConnectionPooling();
    }

    public void setUseConnectionPooling(boolean useHttpConnectionPooling) {
        delegate.setUseConnectionPooling(useHttpConnectionPooling);
    }

    @Override
    public WebMapTileServer getWebMapTileServer(ProgressListener listener) throws IOException {

        return delegate.getWebMapTileServer(listener);
    }

    @Override
    public String getHeaderName() {
        return delegate.getHeaderName();
    }

    @Override
    public void setHeaderName(String headerName) {
        delegate.setHeaderName(headerName);
    }

    @Override
    public String getHeaderValue() {
        return delegate.getHeaderValue();
    }

    @Override
    public void setHeaderValue(String headerValue) {
        delegate.setHeaderValue(headerValue);
    }

    @Override
    public Date getDateModified() {
        return delegate.getDateModified();
    }

    @Override
    public Date getDateCreated() {
        return delegate.getDateCreated();
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        delegate.setDateCreated(dateCreated);
    }

    @Override
    public void setDateModified(Date dateModified) {
        delegate.setDateModified(dateModified);
    }
}
