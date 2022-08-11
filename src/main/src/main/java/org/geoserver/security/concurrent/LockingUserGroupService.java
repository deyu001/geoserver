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

package org.geoserver.security.concurrent;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * This is a wrapper class for a {@link GeoServerUserGroupService} This wrapper protects internal
 * data structures using read/write locks
 *
 * @author christian
 */
public class LockingUserGroupService extends AbstractLockingService
        implements GeoServerUserGroupService, UserGroupLoadedListener {

    protected Set<UserGroupLoadedListener> listeners = Collections.synchronizedSet(new HashSet<>());

    /** Constructor for the locking wrapper */
    public LockingUserGroupService(GeoServerUserGroupService service) {
        super(service);
        service.registerUserGroupLoadedListener(this);
    }

    /** @return the wrapped service */
    public GeoServerUserGroupService getService() {
        return (GeoServerUserGroupService) super.getService();
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        GeoServerUserGroupStore store = getService().createStore();
        return store != null ? new LockingUserGroupStore(store) : null;
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getGroupByGroupname(java.lang.String)
     */
    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        readLock();
        try {
            return getService().getGroupByGroupname(groupname);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#createUserObject(java.lang.String,
     *     java.lang.String, boolean)
     */
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        readLock();
        try {
            return getService().createUserObject(username, password, isEnabled);
        } finally {
            readUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#load()
     */
    public void load() throws IOException {
        writeLock();
        try {
            getService().load();
        } finally {
            writeUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUserByUsername(java.lang.String)
     */
    public GeoServerUser getUserByUsername(String username) throws IOException {
        readLock();
        try {
            return getService().getUserByUsername(username);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#createGroupObject(java.lang.String,
     *     boolean)
     */
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        readLock();
        try {
            return getService().createGroupObject(groupname, isEnabled);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUsers()
     */
    public SortedSet<GeoServerUser> getUsers() throws IOException {
        readLock();
        try {
            return getService().getUsers();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUserGroups()
     */
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {
        readLock();
        try {
            return getService().getUserGroups();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#getUsersForGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        readLock();
        try {
            return getService().getUsersForGroup(group);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#getGroupsForUser(org.geoserver.security.impl.GeoServerUser)
     */
    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        readLock();
        try {
            return getService().getGroupsForUser(user);
        } finally {
            readUnLock();
        }
    }

    /** Fire {@link UserGroupLoadedEvent} for all listeners */
    protected void fireUserGroupLoadedEvent() {
        UserGroupLoadedEvent event = new UserGroupLoadedEvent(this);
        for (UserGroupLoadedListener listener : listeners) {
            listener.usersAndGroupsChanged(event);
        }
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#registerUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void registerUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.add(listener);
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#unregisterUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.remove(listener);
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.event.UserGroupChangedListener#usersAndGroupsChanged(org.geoserver.security.event.UserGroupChangedEvent)
     */
    public void usersAndGroupsChanged(UserGroupLoadedEvent event) {
        //        if (rwl.isWriteLockedByCurrentThread())
        //            writeUnLock();
        //        else
        //            readUnLock();
        fireUserGroupLoadedEvent();
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#initializeFromConfig(org.geoserver.security.config.SecurityNamedServiceConfig)
     */
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        writeLock();
        try {
            getService().initializeFromConfig(config);
        } finally {
            writeUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        readLock();
        try {
            return getService().loadUserByUsername(username);
        } finally {
            readUnLock();
        }
    }

    /** NO_LOCK */
    @Override
    public String getPasswordEncoderName() {
        return getService().getPasswordEncoderName();
    }

    /** NO_LOCK */
    @Override
    public String getPasswordValidatorName() {
        return getService().getPasswordValidatorName();
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUserCount()
     */
    public int getUserCount() throws IOException {
        readLock();
        try {
            return getService().getUserCount();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getGroupCount()
     */
    public int getGroupCount() throws IOException {
        readLock();
        try {
            return getService().getGroupCount();
        } finally {
            readUnLock();
        }
    }
    /** READ_LOCK */
    @Override
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUsersHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public int getUserCountHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUserCountHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUsersNotHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public int getUserCountNotHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUserCountNotHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        readLock();
        try {
            return getService().getUsersHavingPropertyValue(propname, propvalue);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        readLock();
        try {
            return getService().getUserCountHavingPropertyValue(propname, propvalue);
        } finally {
            readUnLock();
        }
    }
}
