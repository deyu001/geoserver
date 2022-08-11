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

import java.io.IOException;
import java.security.Security;
import java.util.logging.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geotools.util.logging.Logging;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Abstract base implementation, delegating the encoding to third party encoders implementing {@link
 * PasswordEncoder}
 *
 * @author christian
 */
public abstract class AbstractGeoserverPasswordEncoder implements GeoServerPasswordEncoder {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    protected volatile PasswordEncoder stringEncoder;
    protected volatile CharArrayPasswordEncoder charEncoder;

    protected String name;

    private boolean availableWithoutStrongCryptogaphy;
    private boolean reversible = true;
    private String prefix;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public String getName() {
        return name;
    }

    public void setBeanName(String beanName) {
        this.name = beanName;
    }

    /** Does nothing, subclases may override. */
    public void initialize(GeoServerSecurityManager securityManager) throws IOException {}

    /** Does nothing, subclases may override. */
    public void initializeFor(GeoServerUserGroupService service) throws IOException {}

    public AbstractGeoserverPasswordEncoder() {
        setAvailableWithoutStrongCryptogaphy(true);
    }

    protected PasswordEncoder getStringEncoder() {
        if (stringEncoder == null) {
            synchronized (this) {
                if (stringEncoder == null) {
                    stringEncoder = createStringEncoder();
                }
            }
        }
        return stringEncoder;
    }

    /** Creates the encoder instance used when source is a string. */
    protected abstract PasswordEncoder createStringEncoder();

    protected CharArrayPasswordEncoder getCharEncoder() {
        if (charEncoder == null) {
            synchronized (this) {
                if (charEncoder == null) {
                    charEncoder = createCharEncoder();
                }
            }
        }
        return charEncoder;
    }

    /** Creates the encoder instance used when source is a char array. */
    protected abstract CharArrayPasswordEncoder createCharEncoder();

    /** @return the concrete {@link PasswordEncoder} object */
    protected final PasswordEncoder getActualEncoder() {
        return null;
    }

    @Override
    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        return doEncodePassword(getStringEncoder().encode(rawPass));
    }

    @Override
    public String encodePassword(char[] rawPass, Object salt) throws DataAccessException {
        return doEncodePassword(getCharEncoder().encodePassword(rawPass, salt));
    }

    String doEncodePassword(String encPass) {
        if (encPass == null) {
            return encPass;
        }

        StringBuffer buff = initPasswordBuffer();
        buff.append(encPass);
        return buff.toString();
    }

    StringBuffer initPasswordBuffer() {
        StringBuffer buff = new StringBuffer();
        if (getPrefix() != null) {
            buff.append(getPrefix()).append(GeoServerPasswordEncoder.PREFIX_DELIMTER);
        }
        return buff;
    }

    @Override
    public boolean isPasswordValid(String encPass, String rawPass, Object salt)
            throws DataAccessException {
        if (encPass == null) return false;
        return getStringEncoder().matches(rawPass, stripPrefix(encPass));
    }

    @Override
    public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
        if (encPass == null) return false;
        return getCharEncoder().isPasswordValid(stripPrefix(encPass), rawPass, salt);
    }

    String stripPrefix(String encPass) {
        return getPrefix() != null ? removePrefix(encPass) : encPass;
    }

    protected String removePrefix(String encPass) {
        return encPass.replaceFirst(getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER, "");
    }

    @Override
    public abstract PasswordEncodingType getEncodingType();

    /** @return true if this encoder has encoded encPass */
    public boolean isResponsibleForEncoding(String encPass) {
        if (encPass == null) return false;
        return encPass.startsWith(getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER);
    }

    public String decode(String encPass) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("decoding passwords not supported");
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return this.isPasswordValid(encodedPassword, rawPassword.toString(), null);
    }

    @Override
    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        return encPass.toCharArray();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isAvailableWithoutStrongCryptogaphy() {
        return availableWithoutStrongCryptogaphy;
    }

    public void setAvailableWithoutStrongCryptogaphy(boolean availableWithoutStrongCryptogaphy) {
        this.availableWithoutStrongCryptogaphy = availableWithoutStrongCryptogaphy;
    }

    public boolean isReversible() {
        return reversible;
    }

    public void setReversible(boolean reversible) {
        this.reversible = reversible;
    }

    /** Interface for password encoding when source password is specified as char array. */
    protected interface CharArrayPasswordEncoder {

        String encodePassword(char[] rawPass, Object salt);

        boolean isPasswordValid(String encPass, char[] rawPass, Object salt);
    }
}
