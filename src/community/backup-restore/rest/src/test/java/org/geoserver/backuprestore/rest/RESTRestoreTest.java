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

package org.geoserver.backuprestore.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import net.sf.json.JSONObject;
import org.geoserver.backuprestore.BackupRestoreTestSupport;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author Alessio Fabiani, GeoSolutions */
public class RESTRestoreTest extends BackupRestoreTestSupport {

    @Test
    public void testNewRestore() throws Exception {
        Resource archiveFile = file("geoserver-alfa2-backup.zip");

        if (archiveFile == null) {
            LOGGER.log(Level.WARNING, "Could not find source archive file.");
        } else {
            String json =
                    "{\"restore\": {"
                            + "   \"archiveFile\": \""
                            + archiveFile.path()
                            + "\", "
                            + "   \"options\": { \"option\": [\"BK_DRY_RUN=true\", \"BK_BEST_EFFORT=true\"] }"
                            + "  }"
                            + "}";

            JSONObject restore = postNewRestore(json);

            assertNotNull(restore);

            Thread.sleep(500);

            JSONObject execution =
                    readExecutionStatus(restore.getJSONObject("execution").getLong("id"));

            assertTrue(
                    "STARTED".equals(execution.getString("status"))
                            || "STARTING".equals(execution.getString("status")));

            int cnt = 0;
            while (cnt < 100
                    && ("STARTED".equals(execution.getString("status"))
                            || "STARTING".equals(execution.getString("status")))) {
                execution = readExecutionStatus(execution.getLong("id"));

                Thread.sleep(100);
                cnt++;
            }

            if (cnt < 100) {
                assertTrue("COMPLETED".equals(execution.getString("status")));
            }
        }
    }

    JSONObject postNewRestore(String body) throws Exception {
        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/br/restore", body, "application/json");

        assertEquals(201, resp.getStatus());
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);
        JSONObject execution = json.getJSONObject("restore");

        assertNotNull(execution);

        return execution;
    }

    JSONObject readExecutionStatus(long executionId) throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/br/restore/"
                                        + executionId
                                        + ".json");

        JSONObject restore = json.getJSONObject("restore");

        assertNotNull(restore);

        JSONObject execution = restore.getJSONObject("execution");

        assertNotNull(execution);

        return execution;
    }
}
