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

import static org.geoserver.security.validation.PasswordPolicyException.IS_NULL;
import static org.geoserver.security.validation.PasswordPolicyException.MAX_LENGTH_$1;
import static org.geoserver.security.validation.PasswordPolicyException.MIN_LENGTH_$1;
import static org.geoserver.security.validation.PasswordPolicyException.NO_DIGIT;
import static org.geoserver.security.validation.PasswordPolicyException.NO_LOWERCASE;
import static org.geoserver.security.validation.PasswordPolicyException.NO_UPPERCASE;
import static org.geoserver.security.validation.PasswordPolicyException.RESERVED_PREFIX_$1;
import static org.junit.Assert.assertEquals;

import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Before;
import org.junit.Test;

public class PasswordValidatorTest extends GeoServerMockTestSupport {

    PasswordPolicyConfig config;
    PasswordValidatorImpl validator;

    @Before
    public void init() throws Exception {
        config = new PasswordPolicyConfig();
        validator = new PasswordValidatorImpl(getSecurityManager());
        validator.setConfig(config);
    }

    @Test
    public void testPasswords() throws PasswordPolicyException {
        checkForException(null, IS_NULL);

        validator.validatePassword("".toCharArray());
        validator.validatePassword("a".toCharArray());

        checkForException("plain:a", RESERVED_PREFIX_$1, "plain:");
        checkForException("crypt1:a", RESERVED_PREFIX_$1, "crypt1:");
        checkForException("digest1:a", RESERVED_PREFIX_$1, "digest1:");

        validator.validatePassword("plain".toCharArray());
        validator.validatePassword("plaina".toCharArray());

        config.setMinLength(2);
        checkForException("a", MIN_LENGTH_$1, 2);
        validator.validatePassword("aa".toCharArray());

        config.setMaxLength(10);
        checkForException("01234567890", MAX_LENGTH_$1, 10);
        validator.validatePassword("0123456789".toCharArray());

        config.setDigitRequired(true);
        checkForException("abcdef", NO_DIGIT);

        validator.validatePassword("abcde4".toCharArray());

        config.setUppercaseRequired(true);
        checkForException("abcdef4", NO_UPPERCASE);
        validator.validatePassword("abcde4F".toCharArray());

        config.setLowercaseRequired(true);
        checkForException("ABCDE4F", NO_LOWERCASE);
        validator.validatePassword("abcde4F".toCharArray());
    }

    protected void checkForException(String password, String id, Object... params) {
        try {
            validator.validatePassword(password != null ? password.toCharArray() : null);
        } catch (PasswordPolicyException ex) {
            assertEquals(id, ex.getId());
            assertEquals(params.length, ex.getArgs().length);
            for (int i = 0; i < params.length; i++) {
                assertEquals(params[i], ex.getArgs()[i]);
            }
        }
    }
}
