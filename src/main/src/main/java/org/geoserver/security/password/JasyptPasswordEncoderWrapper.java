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

package org.geoserver.security.password;

import org.jasypt.digest.StringDigester;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wrapper class for jasyptPasswordEncoder, for compatibility with the Spring 5.1 API
 *
 * <p>Used by {@link GeoServerDigestPasswordEncoder}
 *
 * @author vickdw Created on 10/23/18
 */
public class JasyptPasswordEncoderWrapper extends AbstractGeoserverPasswordEncoder
        implements PasswordEncoder {

    // The password encryptor or string digester to be internally used
    private PasswordEncryptor passwordEncryptor = null;
    private StringDigester stringDigester = null;
    private Boolean useEncryptor = null;

    /** Creates a new instance of <tt>PasswordEncoder</tt> */
    public JasyptPasswordEncoderWrapper() {
        super();
    }

    /**
     * Sets a password encryptor to be used. Only one of <tt>setPasswordEncryptor</tt> or
     * <tt>setStringDigester</tt> should be called. If both are, the last call will define which
     * method will be used.
     *
     * @param passwordEncryptor the password encryptor instance to be used.
     */
    public void setPasswordEncryptor(final PasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
        this.useEncryptor = Boolean.TRUE;
    }

    /**
     * Sets a string digester to be used. Only one of <tt>setPasswordEncryptor</tt> or
     * <tt>setStringDigester</tt> should be called. If both are, the last call will define which
     * method will be used.
     *
     * @param stringDigester the string digester instance to be used.
     */
    public void setStringDigester(final StringDigester stringDigester) {
        this.stringDigester = stringDigester;
        this.useEncryptor = Boolean.FALSE;
    }

    /**
     * Encodes a password. This implementation completely ignores salt, as jasypt's
     * <tt>PasswordEncryptor</tt> and <tt>StringDigester</tt> normally use a random one. Thus, it
     * can be safely passed as <tt>null</tt>.
     *
     * @param rawPass The password to be encoded.
     * @param salt The salt, which will be ignored. It can be null.
     */
    public String encodePassword(final String rawPass, final Object salt) {
        checkInitialization();
        if (this.useEncryptor.booleanValue()) {
            return this.passwordEncryptor.encryptPassword(rawPass);
        }
        return this.stringDigester.digest(rawPass);
    }

    /**
     * Checks a password's validity. This implementation completely ignores salt, as jasypt's
     * <tt>PasswordEncryptor</tt> and <tt>StringDigester</tt> normally use a random one. Thus, it
     * can be safely passed as <tt>null</tt>.
     *
     * @param encPass The encrypted password (digest) against which to check.
     * @param rawPass The password to be checked.
     * @param salt The salt, which will be ignored. It can be null.
     */
    public boolean isPasswordValid(final String encPass, final String rawPass, final Object salt) {
        checkInitialization();
        if (this.useEncryptor.booleanValue()) {
            return this.passwordEncryptor.checkPassword(rawPass, encPass);
        }
        return this.stringDigester.matches(rawPass, encPass);
    }

    /*
     * Checks that the PasswordEncoder has been correctly initialized
     * (either a password encryptor or a string digester has been set).
     */
    private synchronized void checkInitialization() {
        if (this.useEncryptor == null) {
            this.passwordEncryptor = new BasicPasswordEncryptor();
            this.useEncryptor = Boolean.TRUE;
        } else {
            if (this.useEncryptor.booleanValue()) {
                if (this.passwordEncryptor == null) {
                    throw new EncryptionInitializationException(
                            "Password encoder not initialized: password " + "encryptor is null");
                }
            } else {
                if (this.stringDigester == null) {
                    throw new EncryptionInitializationException(
                            "Password encoder not initialized: string " + "digester is null");
                }
            }
        }
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return encPass.equals(new String(rawPass));
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return PasswordEncoderFactories.createDelegatingPasswordEncoder()
                        .encode(new String(rawPass));
            }
        };
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.PLAIN;
    }

    public String decode(String encPass) throws UnsupportedOperationException {
        return removePrefix(encPass);
    }

    @Override
    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        return decode(encPass).toCharArray();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return this.encodePassword(rawPassword.toString(), null);
    }
}
