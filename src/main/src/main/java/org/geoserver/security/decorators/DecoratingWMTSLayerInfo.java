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
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Delegates every method to the delegate wmts layer info.
 *
 * <p>Subclasses will override selected methods to perform their "decoration" job
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class DecoratingWMTSLayerInfo extends AbstractDecorator<WMTSLayerInfo>
        implements WMTSLayerInfo {

    public DecoratingWMTSLayerInfo(WMTSLayerInfo delegate) {
        super(delegate);
    }

    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    public ReferencedEnvelope boundingBox() throws Exception {
        return delegate.boundingBox();
    }

    public boolean enabled() {
        return delegate.enabled();
    }

    public String getAbstract() {
        return delegate.getAbstract();
    }

    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return delegate.getAdapter(adapterClass, hints);
    }

    public List<String> getAlias() {
        return delegate.getAlias();
    }

    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    public CoordinateReferenceSystem getCRS() {
        return delegate.getCRS();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public String getId() {
        return delegate.getId();
    }

    @Override
    public List<KeywordInfo> getKeywords() {
        return delegate.getKeywords();
    }

    public List<String> keywordValues() {
        return delegate.keywordValues();
    }

    public ReferencedEnvelope getLatLonBoundingBox() {
        return delegate.getLatLonBoundingBox();
    }

    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    public List<MetadataLinkInfo> getMetadataLinks() {
        return delegate.getMetadataLinks();
    }

    @Override
    public List<DataLinkInfo> getDataLinks() {
        return delegate.getDataLinks();
    }

    public String getName() {
        return delegate.getName();
    }

    public NamespaceInfo getNamespace() {
        return delegate.getNamespace();
    }

    public ReferencedEnvelope getNativeBoundingBox() {
        return delegate.getNativeBoundingBox();
    }

    public CoordinateReferenceSystem getNativeCRS() {
        return delegate.getNativeCRS();
    }

    public String getNativeName() {
        return delegate.getNativeName();
    }

    public String prefixedName() {
        return delegate.prefixedName();
    }

    public ProjectionPolicy getProjectionPolicy() {
        return delegate.getProjectionPolicy();
    }

    public Name getQualifiedName() {
        return delegate.getQualifiedName();
    }

    public Name getQualifiedNativeName() {
        return delegate.getQualifiedNativeName();
    }

    public String getSRS() {
        return delegate.getSRS();
    }

    public WMTSStoreInfo getStore() {
        return delegate.getStore();
    }

    public String getTitle() {
        return delegate.getTitle();
    }

    public WMTSLayer getWMTSLayer(ProgressListener listener) throws IOException {
        return delegate.getWMTSLayer(listener);
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public void setAbstract(String abstract1) {
        delegate.setAbstract(abstract1);
    }

    public void setCatalog(Catalog catalog) {
        delegate.setCatalog(catalog);
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void setLatLonBoundingBox(ReferencedEnvelope box) {
        delegate.setLatLonBoundingBox(box);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setNamespace(NamespaceInfo namespace) {
        delegate.setNamespace(namespace);
    }

    public void setNativeBoundingBox(ReferencedEnvelope box) {
        delegate.setNativeBoundingBox(box);
    }

    public void setNativeCRS(CoordinateReferenceSystem nativeCRS) {
        delegate.setNativeCRS(nativeCRS);
    }

    public void setNativeName(String nativeName) {
        delegate.setNativeName(nativeName);
    }

    public void setProjectionPolicy(ProjectionPolicy policy) {
        delegate.setProjectionPolicy(policy);
    }

    public void setSRS(String srs) {
        delegate.setSRS(srs);
    }

    public void setStore(StoreInfo store) {
        delegate.setStore(store);
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
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
    public boolean isServiceConfiguration() {
        return delegate.isServiceConfiguration();
    }

    @Override
    public void setServiceConfiguration(boolean serviceConfiguration) {
        delegate.setServiceConfiguration(serviceConfiguration);
    }

    @Override
    public List<String> getDisabledServices() {
        return delegate.getDisabledServices();
    }

    @Override
    public void setDisabledServices(List<String> disabledServices) {
        delegate.setDisabledServices(disabledServices);
    }

    @Override
    public boolean isSimpleConversionEnabled() {
        return delegate.isSimpleConversionEnabled();
    }

    @Override
    public void setSimpleConversionEnabled(boolean activateComplexToSimpleOutput) {
        delegate.setSimpleConversionEnabled(activateComplexToSimpleOutput);
    }
}
