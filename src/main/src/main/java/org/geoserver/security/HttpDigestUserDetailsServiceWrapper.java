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

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserDetailsWrapper;
import org.geoserver.security.password.GeoServerMultiplexingPasswordEncoder;
import org.geoserver.security.password.UserDetailsPasswordWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;

/**
 * {@link UserDetailsService} implementation to be used for HTTP digest authentication
 *
 * <p>{@link UserDetails} objects have their password alreay md5a1 encoded.
 *
 * <p>{@link DigestAuthenticationFilter#setPasswordAlreadyEncoded(boolean)} must be called with a
 * value of <code>true</code>
 *
 * @author christian
 */
public class HttpDigestUserDetailsServiceWrapper implements UserDetailsService {

    public static class DigestUserDetails extends UserDetailsWrapper {
        private static final long serialVersionUID = 1L;

        private String password;
        private Collection<GrantedAuthority> roles;

        public DigestUserDetails(
                UserDetails details, String password, Collection<GrantedAuthority> roles) {
            super(details);
            this.password = password;
            this.roles = roles;
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return roles;
        }

        @Override
        public String getPassword() {
            return password;
        }
    }

    private GeoServerSecurityManager manager;
    protected GeoServerUserGroupService service;
    protected Charset charSet;
    protected final char[] delimArray = new char[] {':'};
    protected MessageDigest digest;
    protected GeoServerMultiplexingPasswordEncoder enc;

    public HttpDigestUserDetailsServiceWrapper(GeoServerUserGroupService service, Charset charSet) {
        this.service = service;
        this.charSet = charSet;
        manager = service.getSecurityManager();
        enc = new GeoServerMultiplexingPasswordEncoder(service.getSecurityManager(), service);
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {

        if (GeoServerUser.ROOT_USERNAME.equals(username)) return prepareForRootUser();

        GeoServerUser user = (GeoServerUser) service.loadUserByUsername(username);
        return prepareForUser(user);
    }

    UserDetails prepareForUser(GeoServerUser user) {
        char[] pw = null;
        try {
            pw = enc.decodeToCharArray(user.getPassword());
        } catch (UnsupportedOperationException ex) {
            pw = user.getPassword().toCharArray();
        }

        String a1 =
                encodePasswordInA1Format(user.getUsername(), GeoServerSecurityManager.REALM, pw);
        manager.disposePassword(pw);
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.addAll(user.getAuthorities());
        roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        return new DigestUserDetails(user, a1, roles);
    }

    UserDetails prepareForRootUser() {

        char[] mpw = null;
        try {
            mpw = manager.getMasterPassword();
            String a1 =
                    encodePasswordInA1Format(
                            GeoServerUser.ROOT_USERNAME, GeoServerSecurityManager.REALM, mpw);

            return new UserDetailsPasswordWrapper(GeoServerUser.createRoot(), a1);
        } finally {
            if (mpw != null) manager.disposePassword(mpw);
        }
    }

    String encodePasswordInA1Format(String username, String realm, char[] password) {
        char[] array = null;
        try {
            char[] usernameArray = username.toCharArray();
            char[] realmArray = realm.toCharArray();

            array = new char[usernameArray.length + realmArray.length + password.length + 2];
            int pos = 0;

            System.arraycopy(usernameArray, 0, array, pos, usernameArray.length);
            pos += usernameArray.length;

            System.arraycopy(delimArray, 0, array, pos, 1);
            pos++;

            System.arraycopy(realmArray, 0, array, pos, realmArray.length);
            pos += realmArray.length;

            System.arraycopy(delimArray, 0, array, pos, 1);
            pos++;

            System.arraycopy(password, 0, array, pos, password.length);

            MessageDigest md = null;
            try {
                md = (MessageDigest) digest.clone(); // thread safe
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            return new String(Hex.encode(md.digest(SecurityUtils.toBytes(array, charSet))));

        } finally {
            if (array != null) manager.disposePassword(array);
        }
    }
}
