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

package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.text.MessageFormat;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Test for {@link UserPasswordController}
 *
 * @author Emanuele Tajariol <etj at geo-solutions.it>
 */
public class UserPasswordControllerTest extends GeoServerSystemTestSupport {

    static final String UP_URI = RestBaseController.ROOT_PATH + "/security/self/password";

    static final String USERNAME = "restuser";
    static final String USERPW = "restpassword";

    protected static XpathEngine xp;

    String xmlTemplate =
            "<"
                    + UserPasswordController.XML_ROOT_ELEM
                    + ">"
                    + "<"
                    + UserPasswordController.UP_NEW_PW
                    + ">{0}</"
                    + UserPasswordController.UP_NEW_PW
                    + ">"
                    + "</"
                    + UserPasswordController.XML_ROOT_ELEM
                    + ">";

    String xmlBadTemplate =
            "<"
                    + UserPasswordController.XML_ROOT_ELEM
                    + ">"
                    + "<not_the_right_element>{0}</not_the_right_element>"
                    + "</"
                    + UserPasswordController.XML_ROOT_ELEM
                    + ">";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // Create the test restuser if needed
        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        if (service.getUserByUsername(USERNAME) == null) {
            GeoServerUser user = service.createUserObject(USERNAME, USERPW, true);
            GeoServerUserGroupStore store = service.createStore();

            store.addUser(user);
            store.store();
            service.load();
        }

        xp = XMLUnit.newXpathEngine();
    }

    public void resetUserPassword() throws IOException, PasswordPolicyException {
        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        GeoServerUser user = service.getUserByUsername(USERNAME);
        user.setPassword(USERPW);

        GeoServerUserGroupStore store = service.createStore();
        store.updateUser(user);
        store.store();
        service.load();
    }

    public void login() throws Exception {
        resetUserPassword();

        login(USERNAME, USERPW, "ROLE_AUTHENTICATED");
    }

    @Test
    public void testGetAsAuthorized() throws Exception {
        login();

        assertEquals(
                HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.valueOf(getAsServletResponse(UP_URI).getStatus()));
    }

    @Test
    public void testGetAsNotAuthorized() throws Exception {
        logout();

        assertEquals(
                HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.valueOf(getAsServletResponse(UP_URI).getStatus()));
    }

    @Test
    public void testPutUnauthorized() throws Exception {
        logout();

        String body = MessageFormat.format(xmlTemplate, "new01");
        assertEquals(405, putAsServletResponse(UP_URI, body, "text/xml").getStatus());
    }

    @Test
    public void testPutInvalidNewPassword() throws Exception {
        login();

        String body = MessageFormat.format(xmlTemplate, "   ");
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                putAsServletResponse(UP_URI, body, "text/xml").getStatus());
    }

    @Test
    public void testPutInvalidElement() throws Exception {
        login();

        String body = MessageFormat.format(xmlBadTemplate, "newpw42");
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                putAsServletResponse(UP_URI, body, "text/xml").getStatus());
    }

    @Test
    public void testPutAsXML() throws Exception {
        login();

        String body = MessageFormat.format(xmlTemplate, "pw01");
        assertEquals(200, putAsServletResponse(UP_URI, body, "text/xml").getStatus());
    }

    @Test
    public void checkUpdatedPassword() throws Exception {
        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        GeoServerUser user;

        login();

        // store proper starting encoding
        user = service.getUserByUsername(USERNAME);
        String originalPw = user.getPassword();

        String body = MessageFormat.format(xmlTemplate, "pw01");
        assertEquals(200, putAsServletResponse(UP_URI, body, "text/xml").getStatus());

        // check pw has been updated
        service.load();
        user = service.getUserByUsername(USERNAME);
        String pw1 = user.getPassword();
        assertNotEquals(originalPw, pw1);

        body = MessageFormat.format(xmlTemplate, "pw02");
        assertEquals(200, putAsServletResponse(UP_URI, body, "text/xml").getStatus());

        // check pw has been updated
        service.load();
        user = service.getUserByUsername(USERNAME);
        String pw2 = user.getPassword();
        assertNotEquals(originalPw, pw2);
        assertNotEquals(pw1, pw2);
    }
}
