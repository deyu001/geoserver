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

package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.google.common.util.concurrent.ExecutionError;
import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerRestRoleServiceTest {

    public static final String uri = "http://rest.geoserver.org";

    private RestTemplate template;

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() throws Exception {
        template = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(template);

        mockServer
                .expect(requestTo(uri + "/api/roles"))
                .andRespond(
                        withSuccess(
                                "{\"groups\": [\"anonymous\", \"test\", \"admin\"]}",
                                MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(uri + "/api/adminRole"))
                .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(uri + "/api/users/test"))
                .andRespond(
                        withSuccess(
                                "{\"users\": [{\"username\": \"test\", \"groups\": [\"test\"]}]}",
                                MediaType.APPLICATION_JSON));

        // Not needed anymore thanks to the internal cache
        /* mockServer.expect(requestTo(uri + "/api/adminRole"))
        .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON)); */

        mockServer
                .expect(requestTo(uri + "/api/users/test@geoserver.org"))
                .andRespond(
                        withSuccess(
                                "{\"users\": [{\"username\": \"test@geoserver.org\", \"groups\": [\"test\"]}]}",
                                MediaType.APPLICATION_JSON));

        // Not needed anymore thanks to the internal cache
        /* mockServer.expect(requestTo(uri + "/api/adminRole"))
        .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON)); */

        mockServer
                .expect(requestTo(uri + "/api/users/admin"))
                .andRespond(
                        withSuccess(
                                "{\"users\": [{\"username\": \"admin\", \"groups\": [\"admin\"]}]}",
                                MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(uri + "/api/adminRole"))
                .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    public void testGeoServerRestRoleService() throws IOException {
        GeoServerRestRoleServiceConfig roleServiceconfig = new GeoServerRestRoleServiceConfig();
        roleServiceconfig.setBaseUrl(uri);

        GeoServerRestRoleService roleService = new GeoServerRestRoleService(roleServiceconfig);
        roleService.setRestTemplate(template);

        final SortedSet<GeoServerRole> roles = roleService.getRoles();
        final GeoServerRole adminRole = roleService.getAdminRole();
        final SortedSet<GeoServerRole> testUserRoles = roleService.getRolesForUser("test");
        final SortedSet<GeoServerRole> testUserEmailRoles =
                roleService.getRolesForUser("test@geoserver.org");
        final SortedSet<GeoServerRole> adminUserRoles = roleService.getRolesForUser("admin");

        assertNotNull(roles);
        assertNotNull(adminRole);
        assertNotNull(testUserRoles);
        assertNotNull(testUserEmailRoles);
        assertNotNull(adminUserRoles);

        assertEquals(3, roles.size());
        assertEquals("ROLE_ADMIN", adminRole.getAuthority());
        assertEquals(testUserEmailRoles.size(), testUserRoles.size());
        assertFalse(testUserRoles.contains(GeoServerRole.ADMIN_ROLE));
        assertFalse(testUserRoles.contains(adminRole));
        assertTrue(adminUserRoles.contains(GeoServerRole.ADMIN_ROLE));
    }

    @Test
    public void testGeoServerRestRoleServiceInternalCache()
            throws IOException, InterruptedException {
        GeoServerRestRoleServiceConfig roleServiceconfig = new GeoServerRestRoleServiceConfig();
        roleServiceconfig.setBaseUrl(uri);

        GeoServerRestRoleService roleService = new GeoServerRestRoleService(roleServiceconfig);
        roleService.setRestTemplate(template);

        roleService.getRoles();
        roleService.getAdminRole();
        roleService.getRolesForUser("test");
        Thread.sleep(31 * 1000);
        try {
            roleService.getRolesForUser("test@geoserver.org");
            fail("Expecting ExecutionError to be thrown");
        } catch (ExecutionError e) {
            // OK
        }
    }
}
