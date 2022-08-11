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
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.NamedMap;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Master password controller */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/masterpw")
public class MasterPasswordController extends RestBaseController {

    static final String MP_CURRENT_KEY = "oldMasterPassword";

    static final String MP_NEW_KEY = "newMasterPassword";

    static final String XML_ROOT_ELEM = "masterPassword";

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public NamedMap<String, String> masterPasswordGet() throws IOException {

        if (!getManager().checkAuthenticationForAdminRole()) {
            throw new RestException("Administrative privileges required", HttpStatus.FORBIDDEN);
        }

        char[] masterpw = getManager().getMasterPasswordForREST();

        NamedMap<String, String> m = new NamedMap<>(XML_ROOT_ELEM);
        m.put(MP_CURRENT_KEY, new String(masterpw));

        getManager().disposePassword(masterpw);
        return m;
    }

    @PutMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void masterPasswordPut(@RequestBody Map<String, String> putMap) throws IOException {
        if (!getManager().checkAuthenticationForAdminRole()) {
            // yes, for backwards compat, it's really METHOD_NOT_ALLOWED
            throw new RestException(
                    "Administrative privileges required", HttpStatus.METHOD_NOT_ALLOWED);
        }

        String providerName;
        try {
            providerName = getManager().loadMasterPasswordConfig().getProviderName();
            if (getManager().loadMasterPassswordProviderConfig(providerName).isReadOnly()) {
                throw new RestException(
                        "Master password provider does not allow writes",
                        HttpStatus.METHOD_NOT_ALLOWED);
            }
        } catch (IOException e) {
            throw new RestException(
                    "Master password provider does not allow writes",
                    HttpStatus.METHOD_NOT_ALLOWED);
        }

        String current = putMap.get(MP_CURRENT_KEY);
        String newpass = putMap.get(MP_NEW_KEY);

        if (!StringUtils.isNotBlank(current))
            throw new RestException("no master password", HttpStatus.BAD_REQUEST);

        if (!StringUtils.isNotBlank(newpass))
            throw new RestException("no master password", HttpStatus.BAD_REQUEST);

        char[] currentArray = current.trim().toCharArray();
        char[] newpassArray = newpass.trim().toCharArray();

        GeoServerSecurityManager m = getManager();
        try {
            m.saveMasterPasswordConfig(
                    m.loadMasterPasswordConfig(), currentArray, newpassArray, newpassArray);
        } catch (Exception e) {
            throw new RestException(
                    "Cannot change master password", HttpStatus.UNPROCESSABLE_ENTITY, e);
        } finally {
            m.disposePassword(currentArray);
            m.disposePassword(newpassArray);
        }
    }
}
