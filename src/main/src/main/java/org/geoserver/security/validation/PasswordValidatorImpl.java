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

package org.geoserver.security.validation;

import static org.geoserver.security.validation.PasswordPolicyException.MAX_LENGTH_$1;
import static org.geoserver.security.validation.PasswordPolicyException.MIN_LENGTH_$1;
import static org.geoserver.security.validation.PasswordPolicyException.NO_DIGIT;
import static org.geoserver.security.validation.PasswordPolicyException.NO_LOWERCASE;
import static org.geoserver.security.validation.PasswordPolicyException.NO_UPPERCASE;
import static org.geoserver.security.validation.PasswordPolicyException.RESERVED_PREFIX_$1;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;

/**
 * Implementation of the password {@link PasswordValidator} interface
 *
 * @author christian
 */
public class PasswordValidatorImpl extends AbstractSecurityValidator implements PasswordValidator {

    protected PasswordPolicyConfig config;
    protected static volatile Set<String> notAllowedPrefixes;
    protected static Object lock = new Object();

    /** Calculates not allowed prefixes */
    public PasswordValidatorImpl(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    public static Set<String> getNotAllowedPrefixes() {
        if (notAllowedPrefixes == null) {
            synchronized (lock) {
                if (notAllowedPrefixes == null) {
                    HashSet<String> nap = new HashSet<>();
                    for (GeoServerPasswordEncoder enc :
                            GeoServerExtensions.extensions(GeoServerPasswordEncoder.class)) {
                        nap.add(enc.getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER);
                    }
                    notAllowedPrefixes = nap;
                }
            }
        }
        return notAllowedPrefixes;
    }

    /**
     * Checks if the password starts with an encoder prefix, if true return the prefix, if false
     * return <code>null</code>
     */
    public static String passwordStartsWithEncoderPrefix(char[] password) {

        if (password == null) return null;

        O:
        for (String prefix : getNotAllowedPrefixes()) {
            if (prefix.length() > password.length) continue;
            for (int i = 0; i < prefix.length(); i++) {
                if (prefix.charAt(i) != password[i]) continue O;
            }
            return prefix;
        }
        return null;
    }

    @Override
    public void setConfig(PasswordPolicyConfig config) {
        this.config = config;
    }

    @Override
    public PasswordPolicyConfig getConfig() {
        return config;
    }

    @Override
    public void validatePassword(char[] password) throws PasswordPolicyException {
        // if (password==null)
        //    throw createSecurityException(PW_IS_NULL);

        if (password == null) {
            // treat as "empty"
            password = new char[] {};
        }

        if (password.length < config.getMinLength())
            throw createSecurityException(MIN_LENGTH_$1, config.getMinLength());

        if (config.getMaxLength() >= 0 && password.length > config.getMaxLength())
            throw createSecurityException(MAX_LENGTH_$1, config.getMaxLength());

        if (config.isDigitRequired()) {
            if (checkUsingMethod("isDigit", password) == false)
                throw createSecurityException(NO_DIGIT);
        }
        if (config.isUppercaseRequired()) {
            if (checkUsingMethod("isUpperCase", password) == false)
                throw createSecurityException(NO_UPPERCASE);
        }
        if (config.isLowercaseRequired()) {
            if (checkUsingMethod("isLowerCase", password) == false)
                throw createSecurityException(NO_LOWERCASE);
        }

        String prefix = passwordStartsWithEncoderPrefix(password);
        if (prefix != null) throw createSecurityException(RESERVED_PREFIX_$1, prefix);
    }

    /** Executes statis check methods from the character class */
    protected boolean checkUsingMethod(String methodname, char[] charArray) {
        try {
            Method m = getClass().getMethod(methodname, Character.class);
            for (char c : charArray) {
                Boolean result = (Boolean) m.invoke(this, c);
                if (result) return true;
            }
            return false;
        } catch (Exception ex) {
            throw new RuntimeException("never should reach this point", ex);
        }
    }

    public boolean isDigit(Character c) {
        return Character.isDigit(c);
    }

    public boolean isUpperCase(Character c) {
        return Character.isUpperCase(c);
    }

    public boolean isLowerCase(Character c) {
        return Character.isLowerCase(c);
    }

    /** Helper method for creating a proper {@link PasswordPolicyException} object */
    protected PasswordPolicyException createSecurityException(String errorid, Object... args) {
        PasswordPolicyException ex = new PasswordPolicyException(errorid, args);
        return ex;
    }
}
