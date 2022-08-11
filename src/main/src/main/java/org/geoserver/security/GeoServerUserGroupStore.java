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

package org.geoserver.security;

import java.io.IOException;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;

/**
 * A class implementing this interface implements a backend for user and group management. The store
 * always operates on a {@link GeoServerUserGroupService} object.
 *
 * @author christian
 */
public interface GeoServerUserGroupStore extends GeoServerUserGroupService {

    /** Initializes itself from a service for future store modifications concerning this service */
    void initializeFromService(GeoServerUserGroupService service) throws IOException;

    /** discards all entries */
    void clear() throws IOException;

    /**
     * Adds a user, the {@link GeoServerUser#getPassword()} returns the raw password
     *
     * <p>The method must use #getPasswordValidatorName() to validate the raw password and
     * #getPasswordEncoderName() to encode the password.
     */
    void addUser(GeoServerUser user) throws IOException, PasswordPolicyException;

    /**
     * Updates a user
     *
     * <p>The method must be able to determine if {@link GeoServerUser#getPassword()} has changed
     * (reread from backend, check for a prefix, ...)
     *
     * <p>if the password has changed, it is a raw password and the method must use
     * #getPasswordValidatorName() to validate the raw password and #getPasswordEncoderName() to
     *
     * <p>encode the password.
     */
    void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException;

    /** Removes the specified user */
    boolean removeUser(GeoServerUser user) throws IOException;

    /** Adds a group */
    void addGroup(GeoServerUserGroup group) throws IOException;

    /** Updates a group */
    void updateGroup(GeoServerUserGroup group) throws IOException;

    /** Removes the specified group. */
    boolean removeGroup(GeoServerUserGroup group) throws IOException;

    /**
     * Synchronizes all changes with the backend store.On success, the associated {@link
     * GeoServerUserGroupService} object should be loaded
     */
    void store() throws IOException;

    /** Associates a user with a group, on success */
    void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group) throws IOException;

    /** Disassociates a user from a group, on success */
    void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group) throws IOException;

    /**
     * returns true if there are pending modifications not written to the backend store
     *
     * @return true/false
     */
    boolean isModified();
}
