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
import org.geoserver.security.AccessMode;

/**
 * Represents a data access rule: identifies a workspace, a layer, an access mode, and the set of
 * roles that are allowed to access it
 *
 * <p>Mind, two rules are considered equal if the address the same data, if you need full
 * comparison, use {@link #equalsExact(DataAccessRule)}
 */
@SuppressWarnings("serial")
public class DataAccessRule implements Comparable<DataAccessRule>, Serializable {

    /** Any layer, or any workspace, or any role */
    public static final String ANY = "*";

    public static DataAccessRule READ_ALL = new DataAccessRule(ANY, ANY, AccessMode.READ);
    public static DataAccessRule WRITE_ALL = new DataAccessRule(ANY, ANY, AccessMode.WRITE);

    String root;

    String layer;

    AccessMode accessMode;

    Set<String> roles;

    boolean globalGroupRule;

    /** Builds a new rule */
    public DataAccessRule(String root, String layer, AccessMode accessMode, Set<String> roles) {
        super();
        this.root = root;
        this.layer = layer;
        this.globalGroupRule = (layer == null);
        this.accessMode = accessMode;
        if (roles == null) this.roles = new HashSet<>();
        else this.roles = new HashSet<>(roles);
    }

    /** Builds a new rule */
    public DataAccessRule(String root, String layer, AccessMode accessMode, String... roles) {
        this(root, layer, accessMode, roles == null ? null : new HashSet<>(Arrays.asList(roles)));
    }

    /** Copy constructor */
    public DataAccessRule(DataAccessRule other) {
        this.root = other.root;
        this.layer = other.layer;
        this.accessMode = other.accessMode;
        this.globalGroupRule = other.globalGroupRule;
        this.roles = new HashSet<>(other.roles);
    }

    /** Builds the default rule: *.*.r=* */
    public DataAccessRule() {
        this(ANY, ANY, AccessMode.READ);
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean isGlobalGroupRule() {
        return globalGroupRule;
    }

    public void setGlobalGroupRule(boolean globalGroupRule) {
        this.globalGroupRule = globalGroupRule;
    }

    /** Returns the key for the current rule. No other rule should have the same */
    public String getKey() {
        if (globalGroupRule) {
            return root + "." + accessMode.getAlias();
        } else {
            return root + "." + layer + "." + accessMode.getAlias();
        }
    }

    /** Returns the list of roles as a comma separated string for this rule */
    public String getValue() {
        if (roles.isEmpty()) {
            return DataAccessRule.ANY;
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
     * Comparison implemented so that generic rules get first, specific one are compared by name,
     * and if anything else is equal, read comes before write
     */
    public int compareTo(DataAccessRule other) {
        int compareRoot = compareCatalogItems(root, other.root);
        if (compareRoot != 0) return compareRoot;

        int compareLayer = compareCatalogItems(layer, other.layer);
        if (compareLayer != 0) return compareLayer;

        if (accessMode.equals(other.accessMode)) return 0;
        else return accessMode.equals(AccessMode.READ) ? -1 : 1;
    }

    /** Equality based on ws/layer/mode only */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataAccessRule)) return false;

        return 0 == compareTo((DataAccessRule) obj);
    }

    /** Full equality, roles included */
    public boolean equalsExact(DataAccessRule obj) {
        if (0 != compareTo(obj)) return false;
        else return roles.equals(obj.roles);
    }

    /** Hashcode based on wfs/layer/mode only */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(root)
                .append(layer)
                .append(accessMode.getAlias())
                .toHashCode();
    }

    /** Generic string comparison that considers the use of {@link #ANY} */
    public int compareCatalogItems(String item, String otherItem) {
        if (item == null) {
            return otherItem != null ? -1 : 0;
        }
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
