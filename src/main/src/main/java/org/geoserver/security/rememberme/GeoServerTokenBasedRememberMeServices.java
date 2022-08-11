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

package org.geoserver.security.rememberme;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.filter.GeoServerWebAuthenticationDetails;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.rememberme.RememberMeUserDetailsService.RememberMeUserDetails;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * Token based remember me services that appends a user group service name to generated tokens.
 *
 * <p>The user group service name is used by {@link RememberMeUserDetailsService} in order to
 * delegate to the proper user group service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    public GeoServerTokenBasedRememberMeServices(
            String key, UserDetailsService userDetailsService) {
        super(key, userDetailsService);
    }

    /** Create the signature by removing the user group service name suffix from the user name */
    @Override
    protected String makeTokenSignature(long tokenExpiryTime, String username, String password) {

        Matcher m = RememberMeUserDetailsService.TOKEN_PATTERN.matcher(username);
        String uName;
        if (!m.matches()) {
            uName = username;
        } else {
            uName = m.group(1).replace("\\@", "@");
            // String service = m.group(2);
        }
        return super.makeTokenSignature(tokenExpiryTime, uName, password);
    }

    /** A proper {@link GeoServerWebAuthenticationDetails} object must be present */
    protected String retrieveUserName(Authentication authentication) {
        if (authentication.getDetails() instanceof GeoServerWebAuthenticationDetails) {
            String userGroupServiceName =
                    ((GeoServerWebAuthenticationDetails) authentication.getDetails())
                            .getUserGroupServiceName();
            if (userGroupServiceName == null || userGroupServiceName.trim().length() == 0)
                return ""; // no service specified --> no remember me
            return encode(super.retrieveUserName(authentication), userGroupServiceName);
        } else return ""; // no remember me feature without a user group service name
    };

    String encode(String username, String userGroupServiceName) {
        if (userGroupServiceName == null) {
            return username;
        }
        if (username.endsWith("@" + userGroupServiceName)) {
            return username;
        }

        // escape any @ symboles present in the username, and append '@userGroupServiceName')
        return username.replace("@", "\\@") + "@" + userGroupServiceName;
    }

    @Override
    protected Authentication createSuccessfulAuthentication(
            HttpServletRequest request, UserDetails user) {
        if (user instanceof RememberMeUserDetails)
            user = ((RememberMeUserDetails) user).getWrappedObject();

        Collection<GrantedAuthority> roles = new HashSet<>();
        if (user.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            roles.addAll(user.getAuthorities());
        } else {
            roles = new HashSet<>();
            roles.addAll(user.getAuthorities());
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        }
        RememberMeAuthenticationToken auth =
                new RememberMeAuthenticationToken(getKey(), user, roles);
        auth.setDetails(getAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }
}
