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

import java.util.Date;
import java.util.List;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.AbstractDecorator;

/**
 * Delegates every method to the wrapped {@link LayerGroupInfo}. Subclasses will override selected
 * methods to perform their "decoration" job
 *
 * @author Andrea Aime
 */
public class DecoratingLayerGroupInfo extends AbstractDecorator<LayerGroupInfo>
        implements LayerGroupInfo {

    public DecoratingLayerGroupInfo(LayerGroupInfo delegate) {
        super(delegate);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return delegate.getBounds();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public LayerInfo getRootLayer() {
        return delegate.getRootLayer();
    }

    @Override
    public StyleInfo getRootLayerStyle() {
        return delegate.getRootLayerStyle();
    }

    @Override
    public List<PublishedInfo> getLayers() {
        return delegate.getLayers();
    }

    @Override
    public List<LayerInfo> layers() {
        LayerGroupHelper helper = new LayerGroupHelper(this);
        return helper.allLayersForRendering();
    }

    @Override
    public List<StyleInfo> styles() {
        LayerGroupHelper helper = new LayerGroupHelper(this);
        return helper.allStylesForRendering();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Mode getMode() {
        return delegate.getMode();
    }

    @Override
    public boolean isQueryDisabled() {
        return delegate.isQueryDisabled();
    }

    @Override
    public void setQueryDisabled(boolean queryDisabled) {
        delegate.setQueryDisabled(queryDisabled);
    }

    @Override
    public WorkspaceInfo getWorkspace() {
        return delegate.getWorkspace();
    }

    @Override
    public String prefixedName() {
        return delegate.prefixedName();
    }

    @Override
    public List<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    @Override
    public void setRootLayer(LayerInfo rootLayer) {
        delegate.setRootLayer(rootLayer);
    }

    @Override
    public void setRootLayerStyle(StyleInfo style) {
        delegate.setRootLayerStyle(style);
    }

    @Override
    public void setBounds(ReferencedEnvelope bounds) {
        delegate.setBounds(bounds);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setMode(Mode mode) {
        delegate.setMode(mode);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public boolean isAdvertised() {
        return delegate.isAdvertised();
    }

    @Override
    public void setAdvertised(boolean advertised) {
        delegate.setAdvertised(advertised);
    }

    @Override
    public void setWorkspace(WorkspaceInfo workspace) {
        delegate.setWorkspace(workspace);
    }

    @Override
    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public void setTitle(String title) {
        delegate.setTitle(title);
    }

    @Override
    public String getAbstract() {
        return delegate.getAbstract();
    }

    @Override
    public void setAbstract(String abstractTxt) {
        delegate.setAbstract(abstractTxt);
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return delegate.getAuthorityURLs();
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return delegate.getIdentifiers();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(delegate)
                .append(']')
                .toString();
    }

    @Override
    public PublishedType getType() {
        return delegate.getType();
    }

    @Override
    public AttributionInfo getAttribution() {
        return delegate.getAttribution();
    }

    @Override
    public void setAttribution(AttributionInfo attribution) {
        delegate.setAttribution(attribution);
    }

    @Override
    public List<MetadataLinkInfo> getMetadataLinks() {
        return delegate.getMetadataLinks();
    }

    @Override
    public boolean equals(Object obj) {
        return LayerGroupInfo.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return LayerGroupInfo.hashCode(this);
    }

    @Override
    public List<KeywordInfo> getKeywords() {
        return delegate.getKeywords();
    }

    @Override
    public Date getDateModified() {
        return delegate.getDateModified();
    }

    @Override
    public Date getDateCreated() {
        return delegate.getDateCreated();
    }
}
