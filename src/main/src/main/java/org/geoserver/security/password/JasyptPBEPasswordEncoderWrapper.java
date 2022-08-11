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

import java.util.Objects;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.TextEncryptor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wrapper class for jasypt PBEPasswordEncoder enabling the class to return the Spring 5.1 version
 * of PasswordEncoder
 *
 * <p>Used by {@link GeoServerPBEPasswordEncoder}
 *
 * @author vickdw Created on 10/23/18
 */
public class JasyptPBEPasswordEncoderWrapper extends AbstractGeoserverPasswordEncoder
        implements PasswordEncoder {
    private TextEncryptor textEncryptor = null;
    private PBEStringEncryptor pbeStringEncryptor = null;
    private Boolean useTextEncryptor = null;

    public JasyptPBEPasswordEncoderWrapper() {}

    /** Creates the encoder instance used when source is a string. */
    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {

            @Override
            public boolean matches(CharSequence encPass, String rawPass)
                    throws DataAccessException {
                return false;
            }

            @Override
            public String encode(CharSequence rawPass) throws DataAccessException {
                return "";
            }
        };
    }

    /** Creates the encoder instance used when source is a char array. */
    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return false;
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return "";
            }
        };
    }

    public void setTextEncryptor(TextEncryptor textEncryptor) {
        this.textEncryptor = textEncryptor;
        this.useTextEncryptor = Boolean.TRUE;
    }

    public void setPbeStringEncryptor(PBEStringEncryptor pbeStringEncryptor) {
        this.pbeStringEncryptor = pbeStringEncryptor;
        this.useTextEncryptor = Boolean.FALSE;
    }

    public String encodePassword(String rawPass, Object salt) {
        this.checkInitialization();
        return this.useTextEncryptor
                ? this.textEncryptor.encrypt(rawPass)
                : this.pbeStringEncryptor.encrypt(rawPass);
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
        this.checkInitialization();
        String decPassword = null;
        if (this.useTextEncryptor) {
            decPassword = this.textEncryptor.decrypt(encPass);
        } else {
            decPassword = this.pbeStringEncryptor.decrypt(encPass);
        }

        return Objects.equals(decPassword, rawPass);
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.ENCRYPT;
    }

    private synchronized void checkInitialization() {
        if (this.useTextEncryptor == null) {
            this.textEncryptor = new BasicTextEncryptor();
            this.useTextEncryptor = Boolean.TRUE;
        } else if (this.useTextEncryptor) {
            if (this.textEncryptor == null) {
                throw new EncryptionInitializationException(
                        "PBE Password encoder not initialized: text encryptor is null");
            }
        } else if (this.pbeStringEncryptor == null) {
            throw new EncryptionInitializationException(
                    "PBE Password encoder not initialized: PBE string encryptor is null");
        }
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword != null) {
            return encodePassword(rawPassword.toString(), null);
        }
        return null;
    }
}
