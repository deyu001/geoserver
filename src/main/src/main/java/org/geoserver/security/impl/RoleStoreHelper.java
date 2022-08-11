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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class is common helper for {@link AbstractRoleService} and {@link AbstractRoleStore} to
 * avoid code duplication
 *
 * @author christian
 */
public class RoleStoreHelper {
    public TreeMap<String, GeoServerRole> roleMap = new TreeMap<>();
    public TreeMap<String, SortedSet<GeoServerRole>> group_roleMap = new TreeMap<>();
    public TreeMap<String, SortedSet<GeoServerRole>> user_roleMap = new TreeMap<>();
    public HashMap<GeoServerRole, GeoServerRole> role_parentMap = new HashMap<>();

    public void clearMaps() {
        roleMap.clear();
        role_parentMap.clear();
        group_roleMap.clear();
        user_roleMap.clear();
    }

    public Map<String, String> getParentMappings() throws IOException {
        Map<String, String> parentMap = new HashMap<>();
        for (GeoServerRole role : roleMap.values()) {
            GeoServerRole parentRole = role_parentMap.get(role);
            parentMap.put(
                    role.getAuthority(), parentRole == null ? null : parentRole.getAuthority());
        }
        return Collections.unmodifiableMap(parentMap);
    }

    public SortedSet<GeoServerRole> getRoles() throws IOException {
        SortedSet<GeoServerRole> result = new TreeSet<>();
        result.addAll(roleMap.values());
        return Collections.unmodifiableSortedSet(result);
    }

    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        SortedSet<GeoServerRole> roles = user_roleMap.get(username);
        if (roles == null) roles = new TreeSet<>();
        return Collections.unmodifiableSortedSet(roles);
    }

    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        SortedSet<GeoServerRole> roles = group_roleMap.get(groupname);
        if (roles == null) roles = new TreeSet<>();
        return Collections.unmodifiableSortedSet(roles);
    }

    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return role_parentMap.get(role);
    }

    public GeoServerRole getRoleByName(String role) throws IOException {
        return roleMap.get(role);
    }

    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        SortedSet<String> result = new TreeSet<>();
        for (Entry<String, SortedSet<GeoServerRole>> entry : group_roleMap.entrySet()) {
            if (entry.getValue().contains(role)) result.add(entry.getKey());
        }
        return Collections.unmodifiableSortedSet(result);
    }

    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        SortedSet<String> result = new TreeSet<>();
        for (Entry<String, SortedSet<GeoServerRole>> entry : user_roleMap.entrySet()) {
            if (entry.getValue().contains(role)) result.add(entry.getKey());
        }
        return Collections.unmodifiableSortedSet(result);
    }

    public int getRoleCount() throws IOException {
        return roleMap.size();
    }
}
