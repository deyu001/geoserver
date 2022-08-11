/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

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

package org.geoserver.security.oauth2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;

/**
 * Base Access Token Converter with some GeoServer extras
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoServerAccessTokenConverter extends DefaultAccessTokenConverter {

    protected UserAuthenticationConverter userTokenConverter;

    /** Initializes the class with a default user auth converter */
    public GeoServerAccessTokenConverter(UserAuthenticationConverter defaultUserAuthConverter) {
        setUserTokenConverter(defaultUserAuthConverter);
    }

    /** Initializes the class with a default user auth converter */
    public GeoServerAccessTokenConverter() {
        setUserTokenConverter(new GeoServerUserAuthenticationConverter());
    }

    /**
     * Converter for the part of the data in the token representing a user.
     *
     * @param userTokenConverter the userTokenConverter to set
     */
    public final void setUserTokenConverter(UserAuthenticationConverter userTokenConverter) {
        this.userTokenConverter = userTokenConverter;
        super.setUserTokenConverter(userTokenConverter);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        Map<String, String> parameters = new HashMap<>();
        Set<String> scope = parseScopes(map);
        Authentication user = userTokenConverter.extractAuthentication(map);
        String clientId = (String) map.get(CLIENT_ID);
        parameters.put(CLIENT_ID, clientId);
        Set<String> resourceIds = new LinkedHashSet<>(getAud(map));
        OAuth2Request request =
                new OAuth2Request(
                        parameters, clientId, null, true, scope, resourceIds, null, null, null);
        return new OAuth2Authentication(request, user);
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getAud(Map<String, ?> map) {
        if (!map.containsKey(AUD)) {
            return Collections.emptySet();
        }

        Object aud = map.get(AUD);
        if (aud instanceof Collection) return (Collection) aud;
        else return Collections.singletonList(String.valueOf(aud));
    }

    private Set<String> parseScopes(Map<String, ?> map) {
        // Parsing of scopes coming back from GeoNode are slightly different from
        // the default implementation. Instead of it being a collection it is a
        // String where multiple scopes are separated by a space.
        Object scopeAsObject = map.containsKey(SCOPE) ? map.get(SCOPE) : "";
        Set<String> scope = new LinkedHashSet<>();
        if (String.class.isAssignableFrom(scopeAsObject.getClass())) {
            String scopeAsString = (String) scopeAsObject;
            Collections.addAll(scope, scopeAsString.split(" "));
        } else if (Collection.class.isAssignableFrom(scopeAsObject.getClass())) {
            Collection<String> scopes = (Collection<String>) scopeAsObject;
            scope.addAll(scopes);
        }
        return scope;
    }
}
