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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.GeoServerRole;

/**
 * This is a wrapper class for a {@link GeoServerRoleService}. This wrapper protects internal data
 * structures using read/write locks
 *
 * @author christian
 */
public class LockingRoleService extends AbstractLockingService
        implements GeoServerRoleService, RoleLoadedListener {

    protected Set<RoleLoadedListener> listeners = Collections.synchronizedSet(new HashSet<>());

    /** Constructor for the locking wrapper */
    public LockingRoleService(GeoServerRoleService service) {
        super(service);
        service.registerRoleLoadedListener(this);
    }

    /** @return the wrapped service */
    public GeoServerRoleService getService() {
        return (GeoServerRoleService) super.getService();
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        GeoServerRoleStore store = getService().createStore();
        return store != null ? new LockingRoleStore(store) : null;
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#load()
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
     * @see org.geoserver.security.GeoServerRoleService#getRolesForUser(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        readLock();
        try {
            return getService().getRolesForUser(username);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRolesForGroup(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        readLock();
        try {
            return getService().getRolesForGroup(groupname);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRoles()
     */
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        readLock();
        try {
            return getService().getRoles();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#createRoleObject(java.lang.String)
     */
    public GeoServerRole createRoleObject(String role) throws IOException {
        readLock();
        try {
            return getService().createRoleObject(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#getParentRole(org.geoserver.security.impl.GeoServerRole)
     */
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        readLock();
        try {
            return getService().getParentRole(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRoleByName(java.lang.String)
     */
    public GeoServerRole getRoleByName(String role) throws IOException {
        readLock();
        try {
            return getService().getRoleByName(role);
        } finally {
            readUnLock();
        }
    }

    /** Fire {@link RoleLoadedEvent} for all listeners */
    protected void fireRoleChangedEvent() {
        RoleLoadedEvent event = new RoleLoadedEvent(this);
        for (RoleLoadedListener listener : listeners) {
            listener.rolesChanged(event);
        }
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#registerRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#unregisterRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    /** NO_LOCK */
    public void rolesChanged(RoleLoadedEvent event) {
        // release the locks to avoid deadlock situations
        //        if (rwl.isWriteLockedByCurrentThread())
        //            writeUnLock();
        //        else
        //            readUnLock();
        fireRoleChangedEvent();
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#getGroupNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        readLock();
        try {
            return getService().getGroupNamesForRole(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        readLock();
        try {
            return getService().getUserNamesForRole(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getParentMappings()
     */
    public Map<String, String> getParentMappings() throws IOException {
        readLock();
        try {
            return getService().getParentMappings();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#personalizeRoleParams(java.lang.String,
     *     java.util.Properties, java.lang.String, java.util.Properties)
     */
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {

        readLock();
        try {
            return getService().personalizeRoleParams(roleName, roleParams, userName, userProps);
        } finally {
            readUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#initializeFromConfig(org.geoserver.security.config.SecurityNamedServiceConfig)
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

    /** NO_LOCK */
    @Override
    public GeoServerRole getAdminRole() {
        return getService().getAdminRole();
    }

    /** NO_LOCK */
    @Override
    public GeoServerRole getGroupAdminRole() {
        return getService().getGroupAdminRole();
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRoleCount()
     */
    public int getRoleCount() throws IOException {
        readLock();
        try {
            return getService().getRoleCount();
        } finally {
            readUnLock();
        }
    }
}
