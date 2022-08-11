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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wms.xml.Dimension;
import org.geotools.ows.wms.xml.Extent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A {@link Layer} wrapper carrying around the wrapper policy so that {@link SecuredWebMapServer}
 * can apply it while performing the requests
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredWMSLayer extends Layer {
    Layer delegate;

    WrapperPolicy policy;

    public SecuredWMSLayer(Layer delegate, WrapperPolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    public WrapperPolicy getPolicy() {
        return policy;
    }

    public boolean isQueryable() {
        if (policy.getLimits() instanceof WMSAccessLimits) {
            WMSAccessLimits wl = (WMSAccessLimits) policy.getLimits();
            if (!wl.isAllowFeatureInfo()) {
                return false;
            }
        }
        return delegate.isQueryable();
    }

    public String toString() {
        return "SecuredLayer - " + delegate.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
        result = prime * result + ((policy == null) ? 0 : policy.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SecuredWMSLayer other = (SecuredWMSLayer) obj;
        if (delegate == null) {
            if (other.delegate != null) return false;
        } else if (!delegate.equals(other.delegate)) return false;
        if (policy == null) {
            if (other.policy != null) return false;
        } else if (!policy.equals(other.policy)) return false;
        return true;
    }

    // --------------------------------------------------------------------------------------
    // Purely delegated methods
    // --------------------------------------------------------------------------------------

    public void addChildren(Layer child) {
        delegate.addChildren(child);
    }

    public void clearCache() {
        delegate.clearCache();
    }

    public int compareTo(Layer layer) {
        return delegate.compareTo(layer);
    }

    public String get_abstract() {
        return delegate.get_abstract();
    }

    public Map<String, CRSEnvelope> getBoundingBoxes() {
        return delegate.getBoundingBoxes();
    }

    public Layer[] getChildren() {
        return delegate.getChildren();
    }

    public Dimension getDimension(String name) {
        return delegate.getDimension(name);
    }

    public Map<String, Dimension> getDimensions() {
        return delegate.getDimensions();
    }

    public GeneralEnvelope getEnvelope(CoordinateReferenceSystem crs) {
        return delegate.getEnvelope(crs);
    }

    public Extent getExtent(String name) {
        return delegate.getExtent(name);
    }

    public Map<String, Extent> getExtents() {
        return delegate.getExtents();
    }

    public String[] getKeywords() {
        return delegate.getKeywords();
    }

    public CRSEnvelope getLatLonBoundingBox() {
        return delegate.getLatLonBoundingBox();
    }

    public List<CRSEnvelope> getLayerBoundingBoxes() {
        return delegate.getLayerBoundingBoxes();
    }

    public List<Layer> getLayerChildren() {
        return delegate.getLayerChildren();
    }

    public List<Dimension> getLayerDimensions() {
        return delegate.getLayerDimensions();
    }

    public List<Extent> getLayerExtents() {
        return delegate.getLayerExtents();
    }

    public String getName() {
        return delegate.getName();
    }

    public Layer getParent() {
        return delegate.getParent();
    }

    public double getScaleDenominatorMax() {
        return delegate.getScaleDenominatorMax();
    }

    public double getScaleDenominatorMin() {
        return delegate.getScaleDenominatorMin();
    }

    public double getScaleHintMax() {
        return delegate.getScaleDenominatorMax();
    }

    public double getScaleHintMin() {
        return delegate.getScaleDenominatorMin();
    }

    public Set<String> getSrs() {
        return delegate.getSrs();
    }

    public List<StyleImpl> getStyles() {
        return delegate.getStyles();
    }

    public String getTitle() {
        return delegate.getTitle();
    }

    public void set_abstract(String abstract1) {
        delegate.set_abstract(abstract1);
    }

    public void setBoundingBoxes(CRSEnvelope boundingBox) {
        delegate.setBoundingBoxes(boundingBox);
    }

    public void setBoundingBoxes(Map<String, CRSEnvelope> boundingBoxes) {
        delegate.setBoundingBoxes(boundingBoxes);
    }

    public void setChildren(Layer[] childrenArray) {
        delegate.setChildren(childrenArray);
    }

    public void setDimensions(Collection<Dimension> dimensionList) {
        delegate.setDimensions(dimensionList);
    }

    public void setDimensions(Dimension dimension) {
        delegate.setDimensions(dimension);
    }

    public void setDimensions(Map<String, Dimension> dimensionMap) {
        delegate.setDimensions(dimensionMap);
    }

    public void setExtents(Collection<Extent> extentList) {
        delegate.setExtents(extentList);
    }

    public void setExtents(Extent extent) {
        delegate.setExtents(extent);
    }

    public void setExtents(Map<String, Extent> extentMap) {
        delegate.setExtents(extentMap);
    }

    public void setKeywords(String[] keywords) {
        delegate.setKeywords(keywords);
    }

    public void setLatLonBoundingBox(CRSEnvelope latLonBoundingBox) {
        delegate.setLatLonBoundingBox(latLonBoundingBox);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setParent(Layer parentLayer) {
        delegate.setParent(parentLayer);
    }

    public void setQueryable(boolean queryable) {
        delegate.setQueryable(queryable);
    }

    public void setScaleDenominatorMax(double scaleDenominatorMax) {
        delegate.setScaleDenominatorMax(scaleDenominatorMax);
    }

    public void setScaleDenominatorMin(double scaleDenominatorMin) {
        delegate.setScaleDenominatorMin(scaleDenominatorMin);
    }

    public void setScaleHintMax(double scaleHintMax) {
        delegate.setScaleDenominatorMax(scaleHintMax);
    }

    public void setScaleHintMin(double scaleHintMin) {
        delegate.setScaleDenominatorMin(scaleHintMin);
    }

    public void setSrs(Set<String> srs) {
        delegate.setSrs(srs);
    }

    public void setStyles(List<StyleImpl> styles) {
        delegate.setStyles(styles);
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
    }
}
