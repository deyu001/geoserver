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

package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.transform.IntegerFieldToDateTransform;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.rest.RestBaseController;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

public class ImportTransformTest extends ImporterTestSupport {

    DataStoreInfo store;

    private static String BASEPATH = RestBaseController.ROOT_PATH;

    /**
     * Create a test transform context: one import task with two transforms:
     *
     * <p>One ReprojectTransform and one IntegerFieldToDateTransform.
     */
    @Before
    public void setupTransformContext() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        ImportTask importTask = context.getTasks().get(0);
        importTask.addTransform(new ReprojectTransform(CRS.decode("EPSG:4326")));
        importTask.addTransform(new IntegerFieldToDateTransform("pretendDateIntField"));
        importer.changed(importTask);
    }

    @Test
    public void testGetTransforms() throws Exception {
        int id = lastId();
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms");
        List<JSONObject> txs = parseTransformObjectsFromResponse(j);

        assertEquals(2, txs.size());
        assertEquals("ReprojectTransform", txs.get(0).get("type"));
        assertEquals("IntegerFieldToDateTransform", txs.get(1).get("type"));
    }

    @Test
    public void testGetTransform() throws Exception {
        int id = lastId();
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms/0");

        assertTrue(j instanceof JSONObject);
        assertEquals("ReprojectTransform", ((JSONObject) j).get("type"));
    }

    @Test
    public void testGetTransformsExpandNone() throws Exception {
        int id = lastId();
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms?expand=none");
        List<JSONObject> txs = parseTransformObjectsFromResponse(j);

        assertEquals(2, txs.size());
        assertTrue(txs.get(0).containsKey("href"));
        assertTrue(txs.get(1).containsKey("href"));
    }

    @Test
    public void testPostTransform() throws Exception {
        int id = lastId();
        String json = "{\"type\": \"ReprojectTransform\", \"target\": \"EPSG:3005\"}";
        MockHttpServletResponse resp =
                postAsServletResponse(
                        BASEPATH + "/imports/" + id + "/tasks/0/transforms",
                        json,
                        "application/json");

        assertEquals(HttpStatus.CREATED.value(), resp.getStatus());

        // Make sure it was created
        ImportTask importTask = importer.getContext(id).getTasks().get(0);
        assertEquals(3, importTask.getTransform().getTransforms().size());
    }

    @Test
    public void testDeleteTransform() throws Exception {
        int id = lastId();
        MockHttpServletResponse resp =
                deleteAsServletResponse(BASEPATH + "/imports/" + id + "/tasks/0/transforms/0");
        assertEquals(HttpStatus.OK.value(), resp.getStatus());

        // Make sure it was deleted
        ImportTask importTask = importer.getContext(id).getTasks().get(0);
        assertEquals(1, importTask.getTransform().getTransforms().size());
    }

    @Test
    public void testPutTransform() throws Exception {
        String json = "{\"type\": \"ReprojectTransform\", \"target\": \"EPSG:3005\"}";

        int id = lastId();
        MockHttpServletResponse resp =
                putAsServletResponse(
                        BASEPATH + "/imports/" + id + "/tasks/0/transforms/0",
                        json,
                        "application/json");

        assertEquals(HttpStatus.OK.value(), resp.getStatus());

        // Get it again and make sure it changed.
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms/0");
        assertTrue(j instanceof JSONObject);
        assertEquals("EPSG:3005", ((JSONObject) j).get("target"));
    }

    /**
     * Parses the transforms list out of a /transforms response (example below), asserting that the
     * structure and types are as expected.
     *
     * <pre>
     *
     * {
     *     "transformChain": {
     *         "transforms": [
     *             {
     *                 "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/0",
     *                 "source": null,
     *                 "target": "EPSG:4326",
     *                 "type": "ReprojectTransform"
     *             },
     *             {
     *                 "field": "pretendDateIntField",
     *                 "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/1",
     *                 "type": "IntegerFieldToDateTransform"
     *             }
     *         ],
     *         "type": "vector"
     *     }
     * }
     * </pre>
     *
     * For the above example, this will check the structure and types and then return:
     *
     * <pre>
     * [
     *     {
     *         "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/0",
     *         "source": null,
     *         "target": "EPSG:4326",
     *         "type": "ReprojectTransform"
     *     },
     *     {
     *         "field": "pretendDateIntField",
     *         "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/1",
     *         "type": "IntegerFieldToDateTransform"
     *     }
     * ]
     * </pre>
     */
    List<JSONObject> parseTransformObjectsFromResponse(JSON transformsResponse) {
        assertTrue(transformsResponse instanceof JSONObject);
        JSONObject jo = (JSONObject) transformsResponse;
        assertTrue(
                jo.containsKey("transformChain") && jo.get("transformChain") instanceof JSONObject);
        JSONObject tco = (JSONObject) jo.get("transformChain");
        assertTrue(tco.containsKey("transforms") && tco.get("transforms") instanceof JSONArray);
        JSONArray array = (JSONArray) tco.get("transforms");

        List<JSONObject> transformsList = new ArrayList<>();
        for (Object i : array) {
            assertTrue(i instanceof JSONObject);
            transformsList.add((JSONObject) i);
        }

        return transformsList;
    }
}
