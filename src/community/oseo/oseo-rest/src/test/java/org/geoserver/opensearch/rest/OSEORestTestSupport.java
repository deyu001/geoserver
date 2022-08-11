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

package org.geoserver.opensearch.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.opensearch.eo.OSEOTestSupport;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.data.FeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class OSEORestTestSupport extends OSEOTestSupport {

    protected static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Before
    public void loginAdmin() {
        login("admin", "geoserver", GeoServerRole.ADMIN_ROLE.getAuthority());
    }

    @Before
    public void cleanupTestCollection() throws IOException {
        DataStoreInfo ds = getCatalog().getDataStoreByName("oseo");
        OpenSearchAccess access = (OpenSearchAccess) ds.getDataStore(null);
        FeatureStore store = (FeatureStore) access.getCollectionSource();
        store.removeFeatures(
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("TEST123"),
                        true));
    }

    @Before
    public void cleanupTestCollectionPublishing() throws IOException {
        Catalog catalog = getCatalog();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        removePublishing(catalog, visitor, "gs", "test123");
        removePublishing(catalog, visitor, "gs", "test123-secondary");
    }

    private void removePublishing(
            Catalog catalog, CascadeDeleteVisitor visitor, String workspace, String resourceName) {
        CoverageStoreInfo store =
                catalog.getStoreByName(workspace, resourceName, CoverageStoreInfo.class);
        if (store != null) {
            visitor.visit(store);
        }
        StyleInfo style = catalog.getStyleByName(workspace, resourceName);
        if (style != null) {
            visitor.visit(style);
        }
        Resource data = catalog.getResourceLoader().get("data/" + workspace + "/" + resourceName);
        if (data != null && Resources.exists(data)) {
            data.delete();
        }
    }

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        if (!isQuietTests()) {
            System.out.println(response.getContentAsString());
        }

        assertEquals(expectedHttpCode, response.getStatus());
        assertThat(response.getContentType(), startsWith("application/json"));
        return JsonPath.parse(response.getContentAsString());
    }

    protected byte[] getTestData(String location) throws IOException {
        return IOUtils.toByteArray(getClass().getResourceAsStream(location));
    }

    protected void createTest123Collection() throws Exception, IOException {
        // create the collection
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections",
                        getTestData("/collection.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/TEST123",
                response.getHeader("location"));
    }
}
