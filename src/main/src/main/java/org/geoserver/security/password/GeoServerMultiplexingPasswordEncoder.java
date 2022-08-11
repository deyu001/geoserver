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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geotools.util.logging.Logging;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Multiplexing password encoder.
 *
 * <p>The purpose of this class is to decode a previously encoded password without knowing before
 * hand which password encoder was used. The prefix contained in the encoded password is used to
 * route to the appropriate delegate encoder. Therefore only {@link GeoserverPasswordEncoder}
 * implementations that use a prefix in the encoded password are valid for this encoder.
 *
 * <p>This class can also encode (although not typically used to do so). Encoding simply returns the
 * first avaialble and successful encoder.
 *
 * @author christian
 */
public class GeoServerMultiplexingPasswordEncoder implements PasswordEncoder {

    static Logger LOG = Logging.getLogger(GeoServerMultiplexingPasswordEncoder.class);

    protected Set<GeoServerPasswordEncoder> encoders;

    public GeoServerMultiplexingPasswordEncoder(GeoServerSecurityManager secMgr) {
        this(secMgr, null);
    }

    public GeoServerMultiplexingPasswordEncoder(
            GeoServerSecurityManager secMgr, GeoServerUserGroupService service) {
        encoders = new HashSet<>();
        for (GeoServerPasswordEncoder enc : secMgr.loadPasswordEncoders()) {
            if (StringUtils.hasLength(enc.getPrefix())) {
                if (service != null) {
                    try {
                        if (enc instanceof GeoServerPBEPasswordEncoder) {
                            if (!secMgr.getKeyStoreProvider().hasUserGroupKey(service.getName())) {
                                continue; //   cannot use pbe encoder, no key
                            }
                        }
                        enc.initializeFor(service);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                encoders.add(enc);
            }
        }
    }

    GeoServerPasswordEncoder lookupEncoderForEncodedPassword(String encPass)
            throws UnsupportedOperationException {
        for (GeoServerPasswordEncoder enc : encoders) {
            if (enc.isResponsibleForEncoding(encPass)) {
                return enc;
            }
        }
        throw new UnsupportedOperationException("No password decoder for: " + encPass);
    }

    public String encodePassword(String rawPass, Object salt) throws UnsupportedOperationException {
        for (GeoServerPasswordEncoder enc : encoders) {
            try {
                return enc.encodePassword(rawPass, salt);
            } catch (Exception e) {
                LOG.fine("Password encode failed with " + enc.getName());
            }
        }
        throw new UnsupportedOperationException();
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt)
            throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.isPasswordValid(encPass, rawPass, salt);
    }

    public boolean isPasswordValid(String encPass, char[] rawPass, Object salt)
            throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.isPasswordValid(encPass, rawPass, salt);
    }

    public String decode(String encPass) throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.decode(encPass);
    }

    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.decodeToCharArray(encPass);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encodePassword(rawPassword.toString(), null);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return this.isPasswordValid(encodedPassword, rawPassword.toString(), null);
    }
}
