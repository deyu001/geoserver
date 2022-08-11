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
import java.util.Set;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.util.decorate.AbstractDecorator;

/**
 * Delegates every method to the wrapped {@link LayerInfo}. Subclasses will override selected
 * methods to perform their "decoration" job
 *
 * @author Andrea Aime
 */
public class DecoratingLayerInfo extends AbstractDecorator<LayerInfo> implements LayerInfo {

    public DecoratingLayerInfo(LayerInfo delegate) {
        super(delegate);
    }

    public StyleInfo getDefaultStyle() {
        return delegate.getDefaultStyle();
    }

    public String getId() {
        return delegate.getId();
    }

    public LegendInfo getLegend() {
        return delegate.getLegend();
    }

    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    public String getName() {
        return delegate.getName();
    }

    public String prefixedName() {
        return delegate.prefixedName();
    }

    public String getPath() {
        return delegate.getPath();
    }

    public ResourceInfo getResource() {
        return delegate.getResource();
    }

    public Set<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    public PublishedType getType() {
        return delegate.getType();
    }

    public AttributionInfo getAttribution() {
        return delegate.getAttribution();
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public boolean enabled() {
        return delegate.enabled();
    }

    public void setDefaultStyle(StyleInfo defaultStyle) {
        delegate.setDefaultStyle(defaultStyle);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void setLegend(LegendInfo legend) {
        delegate.setLegend(legend);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setPath(String path) {
        delegate.setPath(path);
    }

    public void setResource(ResourceInfo resource) {
        delegate.setResource(resource);
    }

    public void setType(PublishedType type) {
        delegate.setType(type);
    }

    public void setAttribution(AttributionInfo attr) {
        delegate.setAttribution(attr);
    }

    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(delegate)
                .append(']')
                .toString();
    }

    public void setQueryable(boolean _queryableEnabled) {
        delegate.setQueryable(_queryableEnabled);
    }

    public boolean isQueryable() {
        return delegate.isQueryable();
    }

    public void setOpaque(boolean _opaqueEnabled) {
        delegate.setOpaque(_opaqueEnabled);
    }

    public boolean isOpaque() {
        return delegate.isOpaque();
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
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return delegate.getAuthorityURLs();
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return delegate.getIdentifiers();
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
    public WMSInterpolation getDefaultWMSInterpolationMethod() {
        return delegate.getDefaultWMSInterpolationMethod();
    }

    @Override
    public void setDefaultWMSInterpolationMethod(WMSInterpolation interpolationMethod) {
        delegate.setDefaultWMSInterpolationMethod(interpolationMethod);
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
