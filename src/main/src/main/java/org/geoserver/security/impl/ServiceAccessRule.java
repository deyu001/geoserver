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

package org.geoserver.security.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a service access rule: identifies a service, a method, and the set of roles that are
 * allowed to access it
 */
@SuppressWarnings("serial")
public class ServiceAccessRule implements Comparable<ServiceAccessRule>, Serializable {

    /** Any service or method */
    public static final String ANY = "*";

    public static ServiceAccessRule READ_ALL = new ServiceAccessRule(ANY, ANY);

    public static ServiceAccessRule WRITE_ALL = new ServiceAccessRule(ANY, ANY);

    String service;

    String method;

    Set<String> roles;

    /** Builds a new rule */
    public ServiceAccessRule(String service, String method, Set<String> roles) {
        super();
        this.service = service;
        this.method = method;
        if (roles == null) this.roles = new HashSet<>();
        else this.roles = new HashSet<>(roles);
    }

    /** Builds a new rule */
    public ServiceAccessRule(String service, String method, String... roles) {
        this(service, method, roles == null ? null : new HashSet<>(Arrays.asList(roles)));
    }

    /** Copy constructor */
    public ServiceAccessRule(ServiceAccessRule other) {
        this.service = other.service;
        this.method = other.method;
        this.roles = new HashSet<>(other.roles);
    }

    /** Builds the default rule: *.*.r=* */
    public ServiceAccessRule() {
        this(ANY, ANY);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String layer) {
        this.method = layer;
    }

    public Set<String> getRoles() {
        return roles;
    }

    /** Returns the key for the current rule. No other rule should have the same */
    public String getKey() {
        return service + "." + method;
    }

    /** Returns the list of roles as a comma separated string for this rule */
    public String getValue() {
        if (roles.isEmpty()) {
            return ServiceAccessRule.ANY;
        } else {
            StringBuffer sb = new StringBuffer();
            for (String role : roles) {
                sb.append(role);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

    /**
     * Comparison implemented so that generic rules get first, specific one are compared by service
     * and method
     */
    public int compareTo(ServiceAccessRule other) {
        int compareService = compareServiceItems(service, other.service);
        if (compareService != 0) return compareService;

        return compareServiceItems(method, other.method);
    }

    /** Equality based on service/method only */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceAccessRule)) return false;

        return 0 == compareTo((ServiceAccessRule) obj);
    }

    /** Hashcode based on service/method only */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(service).append(method).toHashCode();
    }

    /** Generic string comparison that considers the use of {@link #ANY} */
    public int compareServiceItems(String item, String otherItem) {
        if (item.equals(otherItem)) return 0;
        else if (ANY.equals(item)) return -1;
        else if (ANY.equals(otherItem)) return 1;
        else return item.compareTo(otherItem);
    }

    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }
}
