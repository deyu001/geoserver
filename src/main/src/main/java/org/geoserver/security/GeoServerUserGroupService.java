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
import java.util.SortedSet;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.PasswordValidator;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * This interface is an extenstion to {@link UserDetailsService}
 *
 * <p>A class implementing this interface implements a read only backend for user and group
 * management
 *
 * @author christian
 */
public interface GeoServerUserGroupService extends GeoServerSecurityService, UserDetailsService {

    /**
     * Creates the user group store that corresponds to this service, or null if creating a store is
     * not supported.
     *
     * <p>Implementations that do not support a store should ensure that {@link #canCreateStore()}
     * returns <code>false</code>.
     */
    GeoServerUserGroupStore createStore() throws IOException;

    /** Register for notifications on load */
    void registerUserGroupLoadedListener(UserGroupLoadedListener listener);

    /** Unregister for notifications on store/load */
    void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener);

    /**
     * Returns the the group object, null if not found
     *
     * @return null if group not found
     */
    GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException;

    /**
     * Returns the the user object, null if not found
     *
     * @return null if user not found
     */
    GeoServerUser getUserByUsername(String username) throws IOException;

    /** Create a user object. Implementations can use subclasses of {@link GeoServerUser} */
    GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException;

    /**
     * Create a user object. Implementations can use classes implementing {@link GeoServerUserGroup}
     */
    GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled) throws IOException;

    /**
     * Returns the list of users.
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUser> getUsers() throws IOException;

    /**
     * Returns the list of GeoserverUserGroups.
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUserGroup> getUserGroups() throws IOException;

    /**
     * get users for a group
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException;

    /**
     * get the groups for a user, an implementation not supporting user groups returns an empty
     * collection
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException;

    /** load from backendstore. On success, a {@link UserGroupLoadedEvent} should be triggered */
    void load() throws IOException;

    /**
     * @return the Spring name of the {@link GeoServerPasswordEncoder} object. mandatory, default is
     *     {@link GeoServerDigestPasswordEncoder#BeanName}.
     */
    String getPasswordEncoderName();

    /**
     * @return the name of the {@link PasswordValidator} object. mandatory, default is {@link
     *     PasswordValidator#DEFAULT_NAME} Validators can be loaded using {@link
     *     GeoServerSecurityManager#loadPasswordValidator(String)}
     */
    String getPasswordValidatorName();

    /** @return the number of users */
    int getUserCount() throws IOException;

    /** @return the number of groups */
    int getGroupCount() throws IOException;

    /** Returns a set of {@link GeoServerUser} objects having the specified property */
    SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException;

    /** Returns the number of {@link GeoServerUser} objects having the specified property */
    int getUserCountHavingProperty(String propname) throws IOException;

    /** Returns a set of {@link GeoServerUser} objects NOT having the specified property */
    SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException;

    /** Returns the number of {@link GeoServerUser} objects NOT having the specified property */
    int getUserCountNotHavingProperty(String propname) throws IOException;

    /**
     * Returns a set of {@link GeoServerUser} objects having the property with the specified value
     */
    SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException;

    /**
     * Returns the number of {@link GeoServerUser} objects having the property with the specified
     * value
     */
    int getUserCountHavingPropertyValue(String propname, String propvalue) throws IOException;
}
