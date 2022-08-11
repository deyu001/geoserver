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

import java.security.SecureRandom;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Class for generating random passwords using {@link SecureRandom}.
 *
 * <p>The password alphabet is {@link #PRINTABLE_ALPHABET}. Since the alphabet is not really big,
 * the length of the password is important.
 *
 * @author christian
 */
public class RandomPasswordProvider {

    /** logger */
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** alphabet */
    public static final char[] PRINTABLE_ALPHABET = {
        '!', '\"', '#', '$', '%', '&', '\'', '(',
        ')', '*', '+', ',', '-', '.', '/', '0',
        '1', '2', '3', '4', '5', '6', '7', '8',
        '9', ':', ';', '<', '?', '@', 'A', 'B',
        'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
        'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        '[', '\\', ']', '^', '_', '`', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
        's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '{', '|', '}', '~',
    };

    /**
     * The default password length assures a key strength of 2 ^ 261 {@link #PRINTABLE_ALPHABET} has
     * 92 characters ln (92 ^ 40 ) / ln (2) = 260.942478242
     */
    public static int DefaultPasswordLength = 40;

    /**
     * Creates a random password of the specified length, if length <=0, return <code>null</code>
     */
    public char[] getRandomPassword(int length) {
        if (length <= 0) return null;
        char[] buff = new char[length];
        getRandomPassword(buff);
        return buff;
    }

    public char[] getRandomPasswordWithDefaultLength() {
        char[] buff = new char[DefaultPasswordLength];
        getRandomPassword(buff);
        return buff;
    }

    /** Creates a random password filling the specified character array. */
    public void getRandomPassword(char[] buff) {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < buff.length; i++) {
            int index = random.nextInt(Integer.MAX_VALUE) % PRINTABLE_ALPHABET.length;
            if (index < 0) index += PRINTABLE_ALPHABET.length;
            buff[i] = PRINTABLE_ALPHABET[index];
        }
    }
    /** Creates a random password filling the specified byte array. */
    public void getRandomPassword(byte[] buff) {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < buff.length; i++) {
            int index = random.nextInt(Integer.MAX_VALUE) % PRINTABLE_ALPHABET.length;
            if (index < 0) index += PRINTABLE_ALPHABET.length;
            buff[i] = (byte) PRINTABLE_ALPHABET[index];
        }
    }
}
