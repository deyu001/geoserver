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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Helper class for querying the role hierarchy
 *
 * @author mcr
 */
public class RoleHierarchyHelper {

    Map<String, String> parentMappings;

    public RoleHierarchyHelper(Map<String, String> parentMappings) {
        this.parentMappings = parentMappings;
    }

    /** Test if roleName is known */
    public boolean containsRole(String roleName) {
        return parentMappings.containsKey(roleName);
    }

    /** return the parent role name, null if role has no parent */
    public String getParent(String roleName) {
        checkRole(roleName);
        String parentRole = parentMappings.get(roleName);
        if (roleName.equals(parentRole)) cycleDetected(roleName, null);
        return parentRole;
    }

    /** Calculate an ordered list of ancestors, starting with the parent */
    public List<String> getAncestors(String roleName) {
        checkRole(roleName);
        List<String> ancestors = new ArrayList<>();
        fillAncestors(parentMappings.get(roleName), ancestors);
        return ancestors;
    }

    /** recursive method to fill the ancestor list */
    protected void fillAncestors(String roleName, List<String> ancestors) {
        if (roleName == null || roleName.length() == 0) return; // end recursion
        ancestors.add(roleName);
        String parentName = parentMappings.get(roleName);
        if (ancestors.contains(parentName)) {
            cycleDetected(roleName, parentName);
        }
        fillAncestors(parentMappings.get(roleName), ancestors);
    }

    /** Return child roles */
    public List<String> getChildren(String roleName) {
        checkRole(roleName);
        List<String> children = new ArrayList<>();
        for (Entry<String, String> entry : parentMappings.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(roleName)) {
                if (roleName.equals(entry.getKey())) cycleDetected(roleName, null);
                children.add(entry.getKey());
            }
        }
        return children;
    }

    /** Get all descendant roles, the order is randomly */
    public List<String> getDescendants(String roleName) {
        checkRole(roleName);
        Set<String> descendants = new HashSet<>();
        fillDescendents(getChildren(roleName), descendants);

        List<String> result = new ArrayList<>();
        result.addAll(descendants);
        return result;
    }

    /** recursive method to fill the descendant list */
    protected void fillDescendents(List<String> children, Set<String> descendants) {
        if (children == null || children.isEmpty()) return; // end recursion
        for (String childName : children) {
            if (descendants.contains(childName)) // cycle
            cycleDetected(childName, null);
            descendants.add(childName);
        }

        for (String childName : children) {
            List<String> grandchildren = getChildren(childName);
            fillDescendents(grandchildren, descendants);
        }
    }

    /** throws a {@link RuntimeException} for a non existing role. */
    protected void checkRole(String roleName) {
        if (parentMappings.containsKey(roleName) == false)
            throw new RuntimeException("Not extistend role: " + roleName);
    }

    /**
     * Throws a {@link RuntimeException} due to a cyclic parent relationship between the two roles
     */
    protected void cycleDetected(String roleName1, String roleName2) {
        if (roleName2 == null) throw new RuntimeException("Cycle detected for " + roleName1);
        else
            throw new RuntimeException("Cycle detected between " + roleName1 + " and " + roleName2);
    }

    /** Check if the role is a root role */
    public boolean isRoot(String roleName) {
        checkRole(roleName);
        return parentMappings.get(roleName) == null;
    }

    /** Get a list of root roles */
    public List<String> getRootRoles() {
        List<String> result = new ArrayList<>();

        for (String roleName : parentMappings.keySet()) {
            if (isRoot(roleName)) result.add(roleName);
        }
        return result;
    }

    /** get a list of leaf roles */
    public List<String> getLeafRoles() {
        List<String> result = new ArrayList<>();

        Set<String> leafRoles = new HashSet<>();
        leafRoles.addAll(parentMappings.keySet());
        for (String parentRoleName : parentMappings.values()) {
            if (parentRoleName != null) leafRoles.remove(parentRoleName);
        }
        result.addAll(leafRoles);
        return result;
    }

    /** returns true if parentName is a valid parent for roleName (avoiding cycles) */
    public boolean isValidParent(String roleName, String parentName) {
        if (parentName == null || parentName.length() == 0) return true;
        if (roleName.equals(parentName)) return false;
        if (getDescendants(roleName).contains(parentName)) return false;
        return true;
    }
}
