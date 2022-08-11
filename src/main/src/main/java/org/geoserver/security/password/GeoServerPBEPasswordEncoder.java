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

import static org.geoserver.security.SecurityUtils.scramble;
import static org.geoserver.security.SecurityUtils.toBytes;
import static org.geoserver.security.SecurityUtils.toChars;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password Encoder using symmetric encryption
 *
 * <p>The salt parameter is not used, this implementation computes a random salt as default.
 *
 * <p>{@link #isPasswordValid(String, String, Object)} {@link #encodePassword(String, Object)}
 *
 * @author christian
 */
public class GeoServerPBEPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    StandardPBEStringEncryptor stringEncrypter;
    StandardPBEByteEncryptor byteEncrypter;

    private String providerName, algorithm;
    private String keyAliasInKeyStore = KeyStoreProviderImpl.CONFIGPASSWORDKEY;

    private KeyStoreProvider keystoreProvider;

    @Override
    public void initialize(GeoServerSecurityManager securityManager) throws IOException {
        this.keystoreProvider = securityManager.getKeyStoreProvider();
    }

    @Override
    public void initializeFor(GeoServerUserGroupService service) throws IOException {
        if (!keystoreProvider.hasUserGroupKey(service.getName())) {
            throw new IOException(
                    "No key alias: "
                            + keystoreProvider.aliasForGroupService(service.getName())
                            + " in key store: "
                            + keystoreProvider.getResource().path());
        }

        keyAliasInKeyStore = keystoreProvider.aliasForGroupService(service.getName());
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyAliasInKeyStore() {
        return keyAliasInKeyStore;
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        byte[] password = lookupPasswordFromKeyStore();

        char[] chars = toChars(password);
        try {
            stringEncrypter = new StandardPBEStringEncryptor();
            stringEncrypter.setPasswordCharArray(chars);

            if (getProviderName() != null && !getProviderName().isEmpty()) {
                stringEncrypter.setProviderName(getProviderName());
            }
            stringEncrypter.setAlgorithm(getAlgorithm());

            JasyptPBEPasswordEncoderWrapper encoder = new JasyptPBEPasswordEncoderWrapper();
            encoder.setPbeStringEncryptor(stringEncrypter);

            return encoder;
        } finally {
            scramble(password);
            scramble(chars);
        }
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        byte[] password = lookupPasswordFromKeyStore();
        char[] chars = toChars(password);

        byteEncrypter = new StandardPBEByteEncryptor();
        byteEncrypter.setPasswordCharArray(chars);

        if (getProviderName() != null && !getProviderName().isEmpty()) {
            byteEncrypter.setProviderName(getProviderName());
        }
        byteEncrypter.setAlgorithm(getAlgorithm());

        return new CharArrayPasswordEncoder() {
            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                byte[] decoded = Base64.getDecoder().decode(encPass.getBytes());
                byte[] decrypted = byteEncrypter.decrypt(decoded);

                char[] chars = toChars(decrypted);
                try {
                    return Arrays.equals(chars, rawPass);
                } finally {
                    scramble(decrypted);
                    scramble(chars);
                }
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                byte[] bytes = toBytes(rawPass);
                try {
                    return new String(Base64.getEncoder().encode(byteEncrypter.encrypt(bytes)));
                } finally {
                    scramble(bytes);
                }
            }
        };
    }

    byte[] lookupPasswordFromKeyStore() {
        try {
            if (!keystoreProvider.containsAlias(getKeyAliasInKeyStore())) {
                throw new RuntimeException(
                        "Keystore: "
                                + keystoreProvider.getResource().path()
                                + " does not"
                                + " contain alias: "
                                + getKeyAliasInKeyStore());
            }
            return keystoreProvider.getSecretKey(getKeyAliasInKeyStore()).getEncoded();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Cannot find alias: "
                            + getKeyAliasInKeyStore()
                            + " in "
                            + keystoreProvider.getResource().path());
        }
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.ENCRYPT;
    }

    public String decode(String encPass) throws UnsupportedOperationException {
        if (stringEncrypter == null) {
            // not initialized
            getStringEncoder();
        }

        return stringEncrypter.decrypt(removePrefix(encPass));
    }

    @Override
    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        if (byteEncrypter == null) {
            // not initialized
            getCharEncoder();
        }

        byte[] decoded = Base64.getDecoder().decode(removePrefix(encPass).getBytes());
        byte[] bytes = byteEncrypter.decrypt(decoded);
        try {
            return toChars(bytes);
        } finally {
            scramble(bytes);
        }
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return createCharEncoder().encodePassword(decodeToCharArray(rawPassword.toString()), null);
    }
}
