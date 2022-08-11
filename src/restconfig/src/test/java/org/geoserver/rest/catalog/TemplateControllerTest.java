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

package org.geoserver.rest.catalog;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class TemplateControllerTest extends CatalogRESTTestSupport {

    public void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    private String getIndexAsString(String childPath, String format) throws Exception {
        String indexUrl = childPath.substring(0, childPath.lastIndexOf("/"));
        if (format != null) {
            indexUrl += "." + format;
        }
        return getAsString(indexUrl);
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    private void testGetPutGetDeleteGet(String path, String content) throws Exception {
        String name = getName(path);

        String htmlIndexToken = "geoserver" + path + "\">" + name + "</a></li>";
        String xmlIndexToken = "<name>" + name + "</name>";
        String jsonIndexToken = "{\"name\":\"" + name + "\"";

        // GET
        assertNotFound(path);
        assertFalse(getIndexAsString(path, null).contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "html").contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "xml").contains(xmlIndexToken));
        assertFalse(getIndexAsString(path, "json").contains(jsonIndexToken));

        // PUT
        put(path, content).close();
        String list = getIndexAsString(path, null);
        if (!list.contains(htmlIndexToken)) {
            assertTrue("list " + path, list.contains(htmlIndexToken));
        }
        assertTrue("list " + path, getIndexAsString(path, "html").contains(htmlIndexToken));
        assertTrue("list " + path, getIndexAsString(path, "xml").contains(xmlIndexToken));
        assertTrue("list " + path, getIndexAsString(path, "json").contains(jsonIndexToken));

        // GET
        assertEquals(content, getAsString(path).trim());

        // DELETE
        assertEquals(200, deleteAsServletResponse(path).getStatus());

        // GET
        assertNotFound(path);
        assertFalse(getIndexAsString(path, null).contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "html").contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "xml").contains(xmlIndexToken));
        assertFalse(getIndexAsString(path, "json").contains(jsonIndexToken));
    }

    @Test
    public void testGetPutGetDeleteGet() throws Exception {
        String path = ROOT_PATH + "/templates/my_template.ftl";
        testGetPutGetDeleteGet(path, "hello world");
    }

    private List<String> getAllPaths() {
        List<String> paths = new ArrayList<>();

        paths.add(ROOT_PATH + "/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/templates/anotherTemplate.ftl");

        paths.add(ROOT_PATH + "/workspaces/topp/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/workspaces/topp/templates/anotherTemplate.ftl");

        paths.add(
                ROOT_PATH + "/workspaces/topp/datastores/states_shapefile/templates/aTemplate.ftl");
        paths.add(
                ROOT_PATH
                        + "/workspaces/topp/datastores/states_shapefile/templates/anotherTemplate.ftl");

        paths.add(
                ROOT_PATH
                        + "/workspaces/topp/datastores/states_shapefile/featuretypes/states/templates/aTemplate.ftl");
        paths.add(
                ROOT_PATH
                        + "/workspaces/topp/datastores/states_shapefile/featuretypes/states/templates/anotherTemplate.ftl");

        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/templates/anotherTemplate.ftl");

        paths.add(
                ROOT_PATH
                        + "/workspaces/wcs/coveragestores/DEM/coverages/tazdem.tiff/templates/aTemplate.ftl");
        paths.add(
                ROOT_PATH
                        + "/workspaces/wcs/coveragestores/DEM/coverages/tazdem.tiff/templates/anotherTemplate.ftl");

        return paths;
    }

    @Test
    public void testAllPathsSequentially() throws Exception {
        Random random = new Random();
        for (String path : getAllPaths()) {
            testGetPutGetDeleteGet(path, "hello test " + random.nextInt(1000));
        }
    }

    void assertNotFound(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path + "?quietOnNotFound=true");
        assertEquals("404 expected for '" + path + "'", 404, response.getStatus());
    }

    @Test
    public void testAllPaths() throws Exception {
        String contentHeader = "hello path ";
        List<String> paths = getAllPaths();

        for (String path : paths) { // GET - confirm template not there
            assertNotFound(path);
        }

        for (String path : paths) { // PUT
            put(path, contentHeader + path).close();
        }

        for (String path : paths) { // GET
            assertEquals(contentHeader + path, getAsString(path).trim());
        }

        for (String path : paths) { // DELETE
            MockHttpServletResponse response = deleteAsServletResponse(path);
            assertEquals(200, response.getStatus());
        }

        for (String path : paths) { // GET - confirm template removed
            assertNotFound(path);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        String fooTemplate = ROOT_PATH + "/templates/foo.ftl";
        String barTemplate = ROOT_PATH + "/templates/bar.ftl";

        String fooContent = "hello foo - longer than bar";
        String barContent = "hello bar";

        // PUT
        put(fooTemplate, fooContent).close();
        put(barTemplate, barContent).close();

        // GET
        assertEquals(fooContent, getAsString(fooTemplate).trim());
        assertEquals(barContent, getAsString(barTemplate).trim());

        fooContent = "goodbye foo";

        // PUT
        put(fooTemplate, fooContent).close();

        // GET
        assertEquals(fooContent, getAsString(fooTemplate).trim());
        assertEquals(barContent, getAsString(barTemplate).trim());
    }

    @Test
    public void testAllPathsSequentiallyForJson() throws Exception {
        Random random = new Random();
        for (String path : getAllJsonTemplatePaths()) {
            testGetPutGetDeleteGet(path, "{key: a json template} " + random.nextInt(1000));
        }
    }

    private List<String> getAllJsonTemplatePaths() {
        List<String> paths = new ArrayList<>();

        paths.add(ROOT_PATH + "/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH + "/templates/anotherTemplate_json.ftl");

        paths.add(ROOT_PATH + "/workspaces/topp/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH + "/workspaces/topp/templates/anotherTemplate_json.ftl");

        paths.add(
                ROOT_PATH
                        + "/workspaces/topp/datastores/states_shapefile/templates/aTemplate_json.ftl");
        paths.add(
                ROOT_PATH
                        + "/workspaces/topp/datastores/states_shapefile/templates/anotherTemplate_json.ftl");

        paths.add(
                ROOT_PATH
                        + "/workspaces/topp/datastores/states_shapefile/featuretypes/states/templates/aTemplate_json.ftl");
        paths.add(
                ROOT_PATH
                        + "/workspaces/topp/datastores/states_shapefile/featuretypes/states/templates/anotherTemplate_json.ftl");

        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/templates/aTemplate_json.ftl");
        paths.add(
                ROOT_PATH
                        + "/workspaces/wcs/coveragestores/DEM/templates/anotherTemplate_json.ftl");

        paths.add(
                ROOT_PATH
                        + "/workspaces/wcs/coveragestores/DEM/coverages/tazdem.tiff/templates/aTemplate_json.ftl");
        paths.add(
                ROOT_PATH
                        + "/workspaces/wcs/coveragestores/DEM/coverages/tazdem.tiff/templates/anotherTemplate_json.ftl");

        return paths;
    }
}
