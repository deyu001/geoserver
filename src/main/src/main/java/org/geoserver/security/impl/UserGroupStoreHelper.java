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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.util.StringUtils;

/**
 * This class is common helper for {@link AbstractUserGroupService} and {@link
 * AbstractUserGroupStore} to avoid code duplication
 *
 * @author christian
 */
public class UserGroupStoreHelper {

    public TreeMap<String, GeoServerUser> userMap = new TreeMap<>();
    public TreeMap<String, GeoServerUserGroup> groupMap = new TreeMap<>();
    public TreeMap<GeoServerUserGroup, SortedSet<GeoServerUser>> group_userMap = new TreeMap<>();
    public TreeMap<GeoServerUser, SortedSet<GeoServerUserGroup>> user_groupMap = new TreeMap<>();
    public TreeMap<String, SortedSet<GeoServerUser>> propertyMap = new TreeMap<>();

    protected SortedSet<GeoServerUser> emptyUsers;
    protected SortedSet<GeoServerUserGroup> emptyGroups;

    public UserGroupStoreHelper() {
        emptyUsers = Collections.unmodifiableSortedSet(new TreeSet<>());
        emptyGroups = Collections.unmodifiableSortedSet(new TreeSet<>());
    }

    public GeoServerUser getUserByUsername(String username) throws IOException {
        return userMap.get(username);
    }

    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        return groupMap.get(groupname);
    }

    public SortedSet<GeoServerUser> getUsers() throws IOException {

        SortedSet<GeoServerUser> users = new TreeSet<>();
        users.addAll(userMap.values());
        return Collections.unmodifiableSortedSet(users);
    }

    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {

        SortedSet<GeoServerUserGroup> groups = new TreeSet<>();
        groups.addAll(groupMap.values());
        return Collections.unmodifiableSortedSet(groups);
    }

    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        SortedSet<GeoServerUserGroup> groups = user_groupMap.get(user);
        if (groups == null) return emptyGroups;
        return Collections.unmodifiableSortedSet(groups);
    }

    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        SortedSet<GeoServerUser> users = group_userMap.get(group);
        if (users == null) return emptyUsers;
        return Collections.unmodifiableSortedSet(users);
    }

    public void clearMaps() {
        userMap.clear();
        groupMap.clear();
        user_groupMap.clear();
        group_userMap.clear();
        propertyMap.clear();
    }

    public int getUserCount() throws IOException {
        return userMap.size();
    }

    public int getGroupCount() throws IOException {
        return groupMap.size();
    }

    SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        SortedSet<GeoServerUser> users = propertyMap.get(propname);
        if (users == null) return emptyUsers;

        return Collections.unmodifiableSortedSet(users);
    }

    int getUserCountHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return 0;

        SortedSet<GeoServerUser> users = propertyMap.get(propname);
        if (users == null) return 0;
        else return users.size();
    }

    SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        SortedSet<GeoServerUser> users = getUsersHavingProperty(propname);
        SortedSet<GeoServerUser> result = new TreeSet<>();
        result.addAll(userMap.values());
        result.removeAll(users);
        return Collections.unmodifiableSortedSet(result);
    }

    int getUserCountNotHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return userMap.size();

        return userMap.size() - getUserCountHavingProperty(propname);
    }

    SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        if (StringUtils.hasLength(propvalue) == false) return emptyUsers;

        SortedSet<GeoServerUser> result = new TreeSet<>();
        for (GeoServerUser user : getUsersHavingProperty(propname)) {
            if (propvalue.equals(user.getProperties().getProperty(propname))) result.add(user);
        }
        return Collections.unmodifiableSortedSet(result);
    }

    int getUserCountHavingPropertyValue(String propname, String propvalue) throws IOException {
        int count = 0;
        if (StringUtils.hasLength(propname) == false) return count;

        if (StringUtils.hasLength(propvalue) == false) return count;

        for (GeoServerUser user : getUsersHavingProperty(propname)) {
            if (propvalue.equals(user.getProperties().getProperty(propname))) count++;
        }
        return count;
    }
}
