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

package org.geoserver.csw.feature.sort;

import java.util.Comparator;
import org.opengis.feature.Attribute;
import org.opengis.filter.expression.PropertyName;

/**
 * Compares two feature based on an attribute value
 *
 * @author Andrea Aime - GeoSolutions
 */
class PropertyComparator<T> implements Comparator<T> {

    PropertyName propertyName;

    boolean ascending;

    /**
     * Builds a new comparator
     *
     * @param propertyName The property name to be used
     * @param ascending If true the comparator will force an ascending order (descending otherwise)
     */
    public PropertyComparator(PropertyName propertyName, boolean ascending) {
        this.propertyName = propertyName;
        this.ascending = ascending;
    }

    public int compare(T f1, T f2) {
        int result = compareAscending(f1, f2);
        if (ascending) {
            return result;
        } else {
            return result * -1;
        }
    }

    @SuppressWarnings("unchecked")
    private int compareAscending(T f1, T f2) {
        Object a1 = propertyName.evaluate(f1);
        Comparable o1, o2;
        if (a1 instanceof Attribute) {
            o1 = (Comparable) ((Attribute) a1).getValue();
        } else {
            o1 = a1 != null ? (Comparable) a1 : null;
        }

        Object a2 = propertyName.evaluate(f2);
        if (a2 instanceof Attribute) {
            o2 = (Comparable) ((Attribute) a2).getValue();
        } else {
            o2 = a2 != null ? (Comparable) a2 : null;
        }

        if (o1 == null) {
            if (o2 == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (o2 == null) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    }
}
