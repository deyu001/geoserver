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
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;

/**
 * This is a wrapper class for a {@link GeoServerUserGroupStore} protected internal data structures
 * using read/write locks
 *
 * @author christian
 */
public class LockingUserGroupStore extends LockingUserGroupService
        implements GeoServerUserGroupStore {

    /** Constructor for the locking wrapper */
    public LockingUserGroupStore(GeoServerUserGroupStore store) {
        super(store);
    }

    /** @return the wrapped store */
    public GeoServerUserGroupStore getStore() {
        return (GeoServerUserGroupStore) super.getService();
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#addUser(org.geoserver.security.impl.GeoServerUser)
     */
    public void addUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        writeLock();
        try {
            getStore().addUser(user);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#updateUser(org.geoserver.security.impl.GeoServerUser)
     */
    public void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        writeLock();
        try {
            getStore().updateUser(user);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#removeUser(org.geoserver.security.impl.GeoServerUser)
     */
    public boolean removeUser(GeoServerUser user) throws IOException {
        writeLock();
        try {
            return getStore().removeUser(user);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#addGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void addGroup(GeoServerUserGroup group) throws IOException {
        writeLock();
        try {
            getStore().addGroup(group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#updateGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void updateGroup(GeoServerUserGroup group) throws IOException {
        writeLock();
        try {
            getStore().updateGroup(group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#removeGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public boolean removeGroup(GeoServerUserGroup group) throws IOException {
        writeLock();
        try {
            return getStore().removeGroup(group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupStore#store()
     */
    public void store() throws IOException {
        writeLock();
        try {
            getStore().store();
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#associateUserToGroup(org.geoserver.security.impl.GeoServerUser,
     *     org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        writeLock();
        try {
            getStore().associateUserToGroup(user, group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#disAssociateUserFromGroup(org.geoserver.security.impl.GeoServerUser,
     *     org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        writeLock();
        try {
            getStore().disAssociateUserFromGroup(user, group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupStore#isModified()
     */
    public boolean isModified() {
        readLock();
        try {
            return getStore().isModified();
        } finally {
            readUnLock();
        }
    }
    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupStore#clear()
     */
    public void clear() throws IOException {
        writeLock();
        try {
            getStore().clear();
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#initializeFromService(org.geoserver.security.GeoServerUserGroupService)
     */
    public void initializeFromService(GeoServerUserGroupService service) throws IOException {
        writeLock();
        try {
            getStore().initializeFromService(service);
        } finally {
            writeUnLock();
        }
    }
}
