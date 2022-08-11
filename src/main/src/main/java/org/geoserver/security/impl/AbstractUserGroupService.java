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
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Standard implementation of {@link GeoServerUserGroupService}
 *
 * @author christian
 */
public abstract class AbstractUserGroupService extends AbstractGeoServerSecurityService
        implements GeoServerUserGroupService {

    protected Set<UserGroupLoadedListener> listeners = Collections.synchronizedSet(new HashSet<>());
    protected String passwordEncoderName, passwordValidatorName;
    protected UserGroupStoreHelper helper;

    protected AbstractUserGroupService() {
        helper = new UserGroupStoreHelper();
    }

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public String getPasswordValidatorName() {
        return passwordValidatorName;
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        // return null, subclasses can override if they support a store along with a service
        return null;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#registerUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void registerUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#unregisterUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#getUserByUsername(java.lang.String)
     */
    public GeoServerUser getUserByUsername(String username) throws IOException {
        return helper.getUserByUsername(username);
    }

    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        return helper.getGroupByGroupname(groupname);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#getUsers()
     */
    public SortedSet<GeoServerUser> getUsers() throws IOException {
        return helper.getUsers();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#getUserGroups()
     */
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {
        return helper.getUserGroups();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#createUserObject(java.lang.String, java.lang.String, boolean)
     */
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        GeoServerUser user = new GeoServerUser(username);
        user.setEnabled(isEnabled);
        user.setPassword(password);
        return user;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#createGroupObject(java.lang.String, boolean)
     */
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        GeoServerUserGroup group = new GeoServerUserGroup(groupname);
        group.setEnabled(isEnabled);
        return group;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#getGroupsForUser(org.geoserver.security.impl.GeoserverUser)
     */
    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        return helper.getGroupsForUser(user);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#getUsersForGroup(org.geoserver.security.impl.GeoserverUserGroup)
     */
    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        return helper.getUsersForGroup(group);
    }

    /** Subclasses must implement this method Load user groups from backend */
    protected abstract void deserialize() throws IOException;

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupService#load()
     */
    public void load() throws IOException {
        LOGGER.info("Start reloading user/groups for service named " + getName());
        // prevent concurrent write from store and
        // read from service
        synchronized (this) {
            deserialize();
        }
        LOGGER.info("Reloading user/groups successful for service named " + getName());
        fireUserGroupLoadedEvent();
    }

    /** Fire {@link UserGroupLoadedEvent} for all listeners */
    protected void fireUserGroupLoadedEvent() {
        UserGroupLoadedEvent event = new UserGroupLoadedEvent(this);
        for (UserGroupLoadedListener listener : listeners) {
            listener.usersAndGroupsChanged(event);
        }
    }

    /** internal use, clear the maps */
    protected void clearMaps() {
        helper.clearMaps();
    }

    /** The root configuration for the user group service. */
    public Resource getConfigRoot() throws IOException {
        return getSecurityManager().userGroup().get(getName());
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        GeoServerUser user = null;
        try {
            user = getUserByUsername(username);
            if (user == null) throw new UsernameNotFoundException(userNotFoundMessage(username));
            RoleCalculator calculator =
                    new RoleCalculator(this, getSecurityManager().getActiveRoleService());
            user.setAuthorities(calculator.calculateRoles(user));
        } catch (IOException e) {
            throw new UsernameNotFoundException(userNotFoundMessage(username), e);
        }

        return user;
    }

    protected String userNotFoundMessage(String username) {
        return "User  " + username + " not found in usergroupservice: " + getName();
    }

    public int getUserCount() throws IOException {
        return helper.getUserCount();
    }

    public int getGroupCount() throws IOException {
        return helper.getGroupCount();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        return helper.getUsersHavingProperty(propname);
    }

    @Override
    public int getUserCountHavingProperty(String propname) throws IOException {
        return helper.getUserCountHavingProperty(propname);
    }

    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        return helper.getUsersNotHavingProperty(propname);
    }

    @Override
    public int getUserCountNotHavingProperty(String propname) throws IOException {
        return helper.getUserCountNotHavingProperty(propname);
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return helper.getUsersHavingPropertyValue(propname, propvalue);
    }

    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return helper.getUserCountHavingPropertyValue(propname, propvalue);
    }
}
