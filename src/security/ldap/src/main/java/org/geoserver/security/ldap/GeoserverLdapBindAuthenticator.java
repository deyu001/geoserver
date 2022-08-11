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

package org.geoserver.security.ldap;

import java.text.MessageFormat;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControlExtractor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Extended BindAuthenticator using a filter to find user data as an alternative to a direct dn
 * access.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class GeoserverLdapBindAuthenticator extends BindAuthenticator {

    private static final Log logger = LogFactory.getLog(GeoserverLdapBindAuthenticator.class);

    private String userFilter = "";

    private String userFormat = "";

    public GeoserverLdapBindAuthenticator(BaseLdapPathContextSource contextSource) {
        super(contextSource);
    }

    public void setUserFilter(String userFilter) {
        this.userFilter = userFilter;
    }

    @Override
    public DirContextOperations authenticate(Authentication authentication) {
        if (userFilter == null || userFilter.equals("")) {
            // authenticate using dn
            return super.authenticate(authentication);
        } else {
            return authenticateUsingFilter(authentication);
        }
    }

    /**
     * If userFilter is defined we extract user data using the filter and dnPattern (if defined) to
     * transform username for authentication.
     */
    protected DirContextOperations authenticateUsingFilter(Authentication authentication) {
        DirContextOperations user = null;
        Assert.isInstanceOf(
                UsernamePasswordAuthenticationToken.class,
                authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");

        String username = authentication.getName();
        String originalUser = username;
        String password = (String) authentication.getCredentials();
        // format given username if required
        if (userFormat != null && !userFormat.equals("")) {
            username = MessageFormat.format(userFormat, username);
        }
        if (!StringUtils.hasLength(password)) {
            logger.debug("Rejecting empty password for user " + username);
            throw new BadCredentialsException(
                    messages.getMessage("BindAuthenticator.emptyPassword", "Empty Password"));
        }

        DirContext ctx = null;
        String userDnStr = "";
        try {
            ctx = getContextSource().getContext(username, password);

            // Check for password policy control
            PasswordPolicyControl ppolicy = PasswordPolicyControlExtractor.extractControl(ctx);

            logger.debug("Retrieving user object using filter...");
            SearchControls searchCtls = new SearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            user =
                    SpringSecurityLdapTemplate.searchForSingleEntryInternal(
                            ctx, searchCtls, "", userFilter, new Object[] {username, originalUser});
            userDnStr = user.getDn().toString();
            if (ppolicy != null) {
                user.setAttributeValue(ppolicy.getID(), ppolicy);
            }

        } catch (NamingException e) {
            // This will be thrown if an invalid user name is used and the
            // method may
            // be called multiple times to try different names, so we trap the
            // exception
            // unless a subclass wishes to implement more specialized behaviour.
            if ((e instanceof org.springframework.ldap.AuthenticationException)
                    || (e instanceof org.springframework.ldap.OperationNotSupportedException)) {
                handleBindException(userDnStr, username, e);
            } else {
                throw e;
            }
        } catch (javax.naming.NamingException e) {
            throw LdapUtils.convertLdapException(e);
        } finally {
            LdapUtils.closeContext(ctx);
        }

        if (user == null) {
            throw new BadCredentialsException(
                    messages.getMessage("BindAuthenticator.badCredentials", "Bad credentials"));
        }

        return user;
    }

    public void setUserFormat(String userFormat) {
        this.userFormat = userFormat;
    }
}
