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

package org.geoserver.rest.security;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/self/password")
public class UserPasswordController extends RestBaseController {
    static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.rest");

    static final String UP_NEW_PW = "newPassword";

    static final String XML_ROOT_ELEM = "userPassword";

    @GetMapping()
    public void passwordGet() {
        throw new RestException("You can not request the password!", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @PutMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        }
    )
    public void passwordPut(@RequestBody Map<String, String> putMap) {
        if (!getManager()
                .checkAuthenticationForRole(
                        SecurityContextHolder.getContext().getAuthentication(),
                        GeoServerRole.AUTHENTICATED_ROLE))
            // yes, for backwards compat, it's really METHOD_NOT_ALLOWED
            throw new RestException(
                    "Administrative privileges required", HttpStatus.METHOD_NOT_ALLOWED);

        try {
            // Look for the service that handles the current user
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();

            GeoServerUserGroupService ugService = null;

            for (GeoServerUserGroupService service : getManager().loadUserGroupServices()) {
                if (service.getUserByUsername(userName) != null) {
                    ugService = service;
                    break;
                }
            }

            if (ugService == null) {
                throw new RestException(
                        "Cannot calculate if PUT is allowed (service not found)",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

        } catch (IOException e) {
            throw new RestException(
                    "Cannot calculate if PUT is allowed (" + e.getMessage() + ")",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    e);
        }
        String newpass = putMap.get(UP_NEW_PW);

        if (StringUtils.isBlank(newpass))
            throw new RestException("Missing '" + UP_NEW_PW + "'", HttpStatus.BAD_REQUEST);

        GeoServerUser user = null;
        GeoServerUserGroupService ugService = null;

        try {
            // Look for the authentication service
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();

            for (GeoServerUserGroupService service : getManager().loadUserGroupServices()) {
                user = service.getUserByUsername(userName);
                if (user != null) {
                    ugService = service;
                    break;
                }
            }
        } catch (IOException e) {
            throw new RestException(
                    "Cannot retrieve user service", HttpStatus.FAILED_DEPENDENCY, e);
        }

        if (ugService == null) {
            throw new RestException("User service not found", HttpStatus.FAILED_DEPENDENCY);
        }

        // Check again if the provider allows updates
        if (!ugService.canCreateStore()) {
            throw new RestException(
                    "User service does not support changing pw", HttpStatus.FAILED_DEPENDENCY);
        }

        try {
            UserGroupStoreValidationWrapper ugStore =
                    new UserGroupStoreValidationWrapper(ugService.createStore());

            user.setPassword(newpass);
            ugStore.updateUser(user);

            ugStore.store();
            ugService.load();

            LOGGER.log(Level.INFO, "Changed password for user {0}", user.getUsername());

        } catch (IOException e) {
            throw new RestException("Internal IO error", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } catch (PasswordPolicyException e) {
            throw new RestException("Bad password", HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }
}
