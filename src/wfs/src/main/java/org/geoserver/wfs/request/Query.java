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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.XlinkPropertyNameType;
import org.eclipse.emf.ecore.EObject;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Query of a GetFeature/LockFeature request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Query extends RequestObject {

    public static Query adapt(Object query) {
        if (query instanceof QueryType) {
            return new WFS11((EObject) query);
        } else if (query instanceof net.opengis.wfs20.QueryType) {
            return new WFS20((EObject) query);
        }
        return null;
    }

    protected Query(EObject adaptee) {
        super(adaptee);
    }

    public URI getSrsName() {
        return eGet(adaptee, "srsName", URI.class);
    }

    public void setSrsName(URI srs) {
        eSet(adaptee, "srsName", srs);
    }

    public String getFeatureVersion() {
        return eGet(adaptee, "featureVersion", String.class);
    }

    // public abstract boolean isTypeNamesUnset(List queries);

    public abstract List<QName> getTypeNames();

    public abstract List<String> getAliases();

    public abstract List<String> getPropertyNames();

    public abstract void setPropertyNames(List<String> names);

    public abstract Filter getFilter();

    public abstract List<SortBy> getSortBy();

    public abstract List<XlinkPropertyNameType> getXlinkPropertyNames();

    public static class WFS11 extends Query {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<QName> getTypeNames() {
            return eGet(adaptee, "typeName", List.class);
        }

        @Override
        public List<String> getAliases() {
            return new ArrayList<>();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<String> getPropertyNames() {
            return eGet(adaptee, "propertyName", List.class);
        }

        @Override
        public void setPropertyNames(List<String> names) {
            eSet(adaptee, "propertyNames", names);
        }

        @Override
        public Filter getFilter() {
            return eGet(adaptee, "filter", Filter.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<SortBy> getSortBy() {
            return eGet(adaptee, "sortBy", List.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<XlinkPropertyNameType> getXlinkPropertyNames() {
            return eGet(adaptee, "xlinkPropertyName", List.class);
        }
    }

    public static class WFS20 extends Query {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<QName> getTypeNames() {
            return eGet(adaptee, "typeNames", List.class);
        }

        public void setTypeNames(List<QName> typeNames) {
            @SuppressWarnings("unchecked")
            List<QName> l = eGet(adaptee, "typeNames", List.class);
            l.clear();
            l.addAll(typeNames);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<String> getAliases() {
            return eGet(adaptee, "aliases", List.class);
        }

        @Override
        public List<String> getPropertyNames() {
            // WFS 2.0 has this as a list of QNAme, drop the qualified part
            @SuppressWarnings("unchecked")
            List<QName> propertyNames = eGet(adaptee, "abstractProjectionClause", List.class);
            List<String> l = new ArrayList<>();
            for (QName name : propertyNames) {
                l.add(name.getLocalPart());
            }
            return l;
        }

        @Override
        public void setPropertyNames(List<String> names) {
            List<QName> qnames = names.stream().map(n -> new QName(n)).collect(Collectors.toList());
            eSet(adaptee, "abstractProjectionClause", qnames);
        }

        @Override
        public Filter getFilter() {
            return eGet(adaptee, "abstractSelectionClause", Filter.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<SortBy> getSortBy() {
            return eGet(adaptee, "abstractSortingClause", List.class);
        }

        @Override
        public List<XlinkPropertyNameType> getXlinkPropertyNames() {
            // no equivalent in wfs 2.0
            return Collections.emptyList();
        }
    }
}
