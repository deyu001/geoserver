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

package org.geoserver.wfs.request;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.geotools.xsd.EMFUtils;
import org.opengis.filter.Filter;

/**
 * Base class for WFS request object adpaters.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class RequestObject {

    /** underlying request object */
    protected EObject adaptee;

    protected RequestObject(EObject adaptee) {
        this.adaptee = adaptee;
    }

    /** The underlying object being adapted. */
    public EObject getAdaptee() {
        return adaptee;
    }

    /** Factory that creates the underlying request model objects. */
    public EFactory getFactory() {
        return adaptee.eClass().getEPackage().getEFactoryInstance();
    }

    //
    // Some common properties that many request objects share
    //

    public String getBaseURL() {
        return getBaseUrl();
    }

    public String getBaseUrl() {
        return eGet(adaptee, "baseUrl", String.class);
    }

    public void setBaseUrl(String baseUrl) {
        eSet(adaptee, "baseUrl", baseUrl);
    }

    public String getVersion() {
        return eGet(adaptee, "version", String.class);
    }

    public boolean isSetService() {
        return eIsSet(adaptee, "service");
    }

    public Map getMetadata() {
        return eGet(adaptee, "metadata", Map.class);
    }

    public void setMetadata(Map metadata) {
        eSet(adaptee, "metadata", metadata);
    }

    public Map getExtendedProperties() {
        return eGet(adaptee, "extendedProperties", Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFormatOptions() {
        return eGet(adaptee, "formatOptions", Map.class);
    }

    public String getHandle() {
        return eGet(adaptee, "handle", String.class);
    }

    public void setHandle(String handle) {
        eSet(adaptee, "handle", handle);
    }

    public QName getTypeName() {
        return eGet(adaptee, "typeName", QName.class);
    }

    public void setTypeName(QName typeName) {
        eSet(adaptee, "typeName", typeName);
    }

    @SuppressWarnings("unchecked")
    public List<QName> getTypeNames() {
        return eGet(adaptee, "typeName", List.class);
    }

    public void setTypeNames(List<QName> typeNames) {
        @SuppressWarnings("unchecked")
        List<QName> l = eGet(adaptee, "typeName", List.class);
        l.clear();
        l.addAll(typeNames);
    }

    public Filter getFilter() {
        return eGet(adaptee, "filter", Filter.class);
    }

    public void setFilter(Filter filter) {
        eSet(adaptee, "filter", filter);
    }

    public boolean isSetOutputFormat() {
        return eIsSet(adaptee, "outputFormat");
    }

    public String getOutputFormat() {
        return eGet(adaptee, "outputFormat", String.class);
    }

    public void setOutputFormat(String outputFormat) {
        eSet(adaptee, "outputFormat", outputFormat);
    }

    //
    // helpers
    //
    protected <T> T eGet(Object obj, String property, Class<T> type) {
        String[] props = property.split("\\.");
        for (String prop : props) {
            if (obj == null) {
                return null;
            }
            if (!EMFUtils.has((EObject) obj, prop)) {
                return null;
            }
            obj = EMFUtils.get((EObject) obj, prop);
        }
        return type.cast(obj);
    }

    protected void eSet(Object obj, String property, Object value) {
        String[] props = property.split("\\.");
        for (int i = 0; i < props.length - 1; i++) {
            obj = eGet(obj, props[i], Object.class);
        }

        EMFUtils.set((EObject) obj, props[props.length - 1], value);
    }

    protected void eAdd(Object obj, String property, Object value) {
        EMFUtils.add((EObject) obj, property, value);
    }

    protected boolean eIsSet(Object obj, String property) {
        return EMFUtils.isSet((EObject) obj, property);
    }
}
