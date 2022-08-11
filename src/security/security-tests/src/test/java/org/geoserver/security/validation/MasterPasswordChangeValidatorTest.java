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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import org.geoserver.security.password.MasterPasswordChangeRequest;
import org.geoserver.security.password.MasterPasswordProviderException;
import org.geoserver.security.password.URLMasterPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProvider.URLMasterPasswordProviderValidator;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;
import org.geoserver.security.password.URLMasterPasswordProviderException;
import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Before;
import org.junit.Test;

public class MasterPasswordChangeValidatorTest extends GeoServerMockTestSupport {

    MasterPasswordChangeValidator validator;

    @Before
    public void setValidator() {
        validator = new MasterPasswordChangeValidator(getSecurityManager());
    }

    protected void checkCurrentPassword(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(ex, MasterPasswordChangeException.CURRENT_PASSWORD_REQUIRED);
        }
        r.setCurrentPassword("blabalb".toCharArray());
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(ex, MasterPasswordChangeException.CURRENT_PASSWORD_ERROR);
        }
    }

    protected void checkConfirmationPassword(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(
                    ex, MasterPasswordChangeException.CONFIRMATION_PASSWORD_REQUIRED);
        }
    }

    protected void checkNewPassword(MasterPasswordChangeRequest r) throws Exception {
        boolean fail = false;
        try {
            validator.validateChangeRequest(r);
        } catch (MasterPasswordChangeException ex) {
            fail = true;
            assertSecurityException(ex, MasterPasswordChangeException.NEW_PASSWORD_REQUIRED);
        }
        assertTrue(fail);
    }

    protected void checkConfirmationEqualsNewPassword(MasterPasswordChangeRequest r)
            throws Exception {
        boolean fail = false;
        try {
            validator.validateChangeRequest(r);
        } catch (MasterPasswordChangeException ex) {
            fail = true;
            assertSecurityException(
                    ex, MasterPasswordChangeException.PASSWORD_AND_CONFIRMATION_NOT_EQUAL);
        }
        assertTrue(fail);
    }

    protected void checkCurrentEqualsNewPassword(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(ex, MasterPasswordChangeException.NEW_EQUALS_CURRENT);
        }
    }

    protected void validateAgainstPolicy(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (PasswordPolicyException ex) {
        }
    }

    @Test
    public void testUrlConfig() throws Exception {
        URLMasterPasswordProviderValidator validator =
                new URLMasterPasswordProviderValidator(getSecurityManager());

        URLMasterPasswordProviderConfig config = new URLMasterPasswordProviderConfig();
        config.setName("foo");
        config.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        try {
            validator.validateAddMasterPasswordProvider(config);
            // getSecurityManager().saveMasterPasswordProviderConfig(config);
            fail();
        } catch (URLMasterPasswordProviderException e) {
            assertSecurityException(e, URLMasterPasswordProviderException.URL_REQUIRED);
        }
        config.setURL(new URL("file:ABC"));
        config.setReadOnly(true);
        try {
            validator.validateAddMasterPasswordProvider(config);
            // getSecurityManager().saveMasterPasswordProviderConfig(config);
            fail();
        } catch (URLMasterPasswordProviderException e) {
            assertSecurityException(
                    e,
                    URLMasterPasswordProviderException.URL_LOCATION_NOT_READABLE,
                    new URL("file:ABC"));
        }
    }

    @Test
    public void testValidator() throws Exception {
        // test spring
        MasterPasswordChangeRequest r = new MasterPasswordChangeRequest();

        checkCurrentPassword(r);
        r.setCurrentPassword("geoserver".toCharArray());
        // r.setCurrentPassword(getMasterPassword().toCharArray());

        checkConfirmationPassword(r);
        r.setConfirmPassword("abc".toCharArray());

        checkNewPassword(r);
        r.setNewPassword("def".toCharArray());

        checkConfirmationEqualsNewPassword(r);
        r.setNewPassword("abc".toCharArray());

        validateAgainstPolicy(r);

        r.setConfirmPassword(r.getCurrentPassword());
        r.setNewPassword(r.getCurrentPassword());

        checkCurrentEqualsNewPassword(r);
        r.setConfirmPassword((new String(r.getCurrentPassword()) + "1").toCharArray());
        r.setNewPassword((new String(r.getCurrentPassword()) + "1").toCharArray());

        validator.validateChangeRequest(r);
    }

    protected void assertSecurityException(
            MasterPasswordChangeException ex, String id, Object... params) {

        assertEquals(id, ex.getId());
        assertEquals(params.length, ex.getArgs().length);
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], ex.getArgs()[i]);
        }
    }

    protected void assertSecurityException(
            MasterPasswordProviderException ex, String id, Object... params) {

        assertEquals(id, ex.getId());
        assertEquals(params.length, ex.getArgs().length);
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], ex.getArgs()[i]);
        }
    }
}
