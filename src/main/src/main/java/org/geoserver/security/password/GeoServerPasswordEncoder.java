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
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * General Geoserver password encoding interface
 *
 * @author christian
 */
public interface GeoServerPasswordEncoder extends PasswordEncoder, BeanNameAware {

    public static final String PREFIX_DELIMTER = ":";

    /** Initialize this encoder. */
    void initialize(GeoServerSecurityManager securityManager) throws IOException;

    /** Initialize this encoder for a {@link GeoServerUserGroupService} object. */
    void initializeFor(GeoServerUserGroupService service) throws IOException;

    /** @return the {@link PasswordEncodingType} */
    PasswordEncodingType getEncodingType();

    /** The name of the password encoder. */
    String getName();

    /** @return true if this encoder has encoded encPass */
    boolean isResponsibleForEncoding(String encPass);

    /**
     * Decodes an encoded password. Only supported for {@link PasswordEncodingType#ENCRYPT} and
     * {@link PasswordEncodingType#PLAIN} encoders, ie those that return <code>true</code> from
     * {@link #isReversible()}.
     *
     * @param encPass The encoded password.
     */
    String decode(String encPass) throws UnsupportedOperationException;

    /**
     * Decodes an encoded password to a char array.
     *
     * @see #decode(String)
     */
    char[] decodeToCharArray(String encPass) throws UnsupportedOperationException;

    /**
     * Encodes a raw password from a char array.
     *
     * @see #encodePassword(char[], Object)
     */
    String encodePassword(char[] password, Object salt);

    /**
     * Encodes a raw password from a String.
     *
     * @see #encodePassword(String, Object)
     */
    String encodePassword(String password, Object salt);

    /**
     * Validates a specified "raw" password (as char array) against an encoded password.
     *
     * @see {@link #isPasswordValid(String, char[], Object)}
     */
    boolean isPasswordValid(String encPass, char[] rawPass, Object salt);

    /**
     * Validates a specified "raw" password (as char array) against an encoded password.
     *
     * @see {@link #isPasswordValid(String, String, Object)}
     */
    boolean isPasswordValid(String encPass, String rawPass, Object salt);

    /**
     * @return a prefix which is stored with the password. This prefix must be unique within all
     *     {@link GeoServerPasswordEncoder} implementations.
     *     <p>Reserved:
     *     <p>plain digest1 crypt1
     *     <p>A plain text password is stored as
     *     <p>plain:password
     */
    String getPrefix();

    /**
     * Is this encoder available without installing the unrestricted policy files of the java
     * cryptographic extension
     */
    boolean isAvailableWithoutStrongCryptogaphy();

    /**
     * Flag indicating if the encoder can decode an encrypted password back into its original plain
     * text form.
     */
    boolean isReversible();
}
