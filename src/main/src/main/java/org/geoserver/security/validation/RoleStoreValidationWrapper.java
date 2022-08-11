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

package org.geoserver.security.validation;

import java.io.IOException;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;

/**
 * This class is a validation wrapper for {@link GeoServerRoleStore}
 *
 * <p>Usage: <code>
 * GeoserverRoleStore valStore = new RoleStoreValidationWrapper(store);
 * valStore.addRole(..);
 * valStore.store()
 * </code> Since the {@link GeoServerRoleStore} interface does not allow to throw {@link
 * RoleServiceException} objects directly, these objects a wrapped into an IOException. Use {@link
 * IOException#getCause()} to get the proper exception.
 *
 * @author christian
 */
public class RoleStoreValidationWrapper extends RoleServiceValidationWrapper
        implements GeoServerRoleStore {

    /** @see RoleServiceValidationWrapper */
    public RoleStoreValidationWrapper(
            GeoServerRoleStore store,
            boolean checkAgainstRules,
            GeoServerUserGroupService... services) {
        super(store, checkAgainstRules, services);
    }

    /** @see RoleServiceValidationWrapper */
    public RoleStoreValidationWrapper(
            GeoServerRoleStore store, GeoServerUserGroupService... services) {
        super(store, services);
    }

    GeoServerRoleStore getStore() {
        return (GeoServerRoleStore) service;
    }

    public void initializeFromService(GeoServerRoleService aService) throws IOException {
        getStore().initializeFromService(aService);
    }

    public void clear() throws IOException {
        getStore().clear();
    }

    public void addRole(GeoServerRole role) throws IOException {
        checkReservedNames(role.getAuthority());
        checkNotExistingRoleName(role.getAuthority());
        checkNotExistingInOtherServices(role.getAuthority());
        getStore().addRole(role);
    }

    public void updateRole(GeoServerRole role) throws IOException {
        checkExistingRoleName(role.getAuthority());
        getStore().updateRole(role);
    }

    public boolean removeRole(GeoServerRole role) throws IOException {
        checkRoleIsMapped(role);
        checkRoleIsUsed(role);
        return getStore().removeRole(role);
    }

    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidGroupName(groupname);
        getStore().associateRoleToGroup(role, groupname);
    }

    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidGroupName(groupname);
        getStore().disAssociateRoleFromGroup(role, groupname);
    }

    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidUserName(username);
        getStore().associateRoleToUser(role, username);
    }

    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidUserName(username);
        getStore().disAssociateRoleFromUser(role, username);
    }

    public void store() throws IOException {
        getStore().store();
    }

    public boolean isModified() {
        return getStore().isModified();
    }

    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {
        checkExistingRoleName(role.getAuthority());
        if (parentRole != null) checkExistingRoleName(parentRole.getAuthority());
        getStore().setParentRole(role, parentRole);
    }
}
