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

package org.geoserver.test.onlineTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.security.AccessMode;
import org.geotools.data.DataAccess;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.mongodb.MongoDataStore;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Tests the integration between MongoDB and App-schema. */
public class ComplexMongoDBTest extends ComplexMongoDBSupport {

    @Override
    protected String getPathOfMappingsToUse() {
        return "/mappings/stations.xml";
    }

    @Test
    public void testAttributeIsNull() throws Exception {
        Document document =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature"
                                + "&filter=<Filter><PropertyIsNull>"
                                + "<PropertyName>StationFeature/nullableField</PropertyName>"
                                + "</PropertyIsNull></Filter>");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                12,
                "/wfs:FeatureCollection/gml:featureMembers/st:StationFeature");
    }

    /** Tests inferred attributes on improved mongodb schema generation. */
    @Test
    public void testGetStationInferredAttributes() throws Exception {
        Document document =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature");
        checkInferredAttributes(document);
    }

    private void checkInferredAttributes(Document document) {
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers"
                                + "/st:StationFeature[st:name='%s' and st:inferredAttribute2='%s']",
                        "station 1", "ok"));
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers"
                                + "/st:StationFeature[st:name='%s' and st:inferredAttribute='%s']",
                        "station 2", "ok"));
    }

    /**
     * Tests the clean and rebuild schemas Rest endpoints for mongodb internal appschema datastores.
     */
    @Test
    public void testStationRestCleanRebuildSchema() throws Exception {
        addUser("mongo", "geoserver", null, Collections.singletonList("ROLE_ADMINISTRATOR"));
        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
        login("mongo", "geoserver", "ROLE_ADMINISTRATOR");
        setRequestAuth("mongo", "geoserver");
        Document document =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature");
        String restPath =
                String.format(
                        "rest/workspaces/st/appschemastores/%s/datastores/%s/cleanSchemas",
                        STATIONS_STORE_NAME, "data_source");
        MockHttpServletResponse servletResponse = postAsServletResponse(restPath);
        assertEquals(200, servletResponse.getStatus());
        DataStoreInfo dataStoreInfo = getCatalog().getDataStoreByName("st", STATIONS_STORE_NAME);
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess =
                dataStoreInfo.getDataStore(null);
        assertTrue(dataAccess instanceof AppSchemaDataAccess);
        AppSchemaDataAccess appSchemaStore = (AppSchemaDataAccess) dataAccess;
        MongoDataStore mongoStore = null;
        List<Name> names = appSchemaStore.getNames();
        for (Name ename : names) {
            FeatureTypeMapping mapping = appSchemaStore.getMappingByName(ename);
            if (mapping.getSourceDatastoreId().filter(id -> "data_source".equals(id)).isPresent()) {
                DataAccess internalStore = mapping.getSource().getDataStore();
                if (internalStore instanceof MongoDataStore) {
                    mongoStore = (MongoDataStore) internalStore;
                    break;
                }
            }
        }
        assertNotNull(mongoStore);
        List<String> typeNames = mongoStore.getSchemaStore().typeNames();
        assertTrue(CollectionUtils.isEmpty(typeNames));
        document = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature");
        checkInferredAttributes(document);
        typeNames = mongoStore.getSchemaStore().typeNames();
        assertFalse(CollectionUtils.isEmpty(typeNames));

        checkStationRestrebuildMongoSchemas();
    }

    /** Tests the cleanSchemas Rest endpoint for mongodb internal appschema datastores. */
    public void checkStationRestrebuildMongoSchemas() throws Exception {
        Document document =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature");
        DataStoreInfo dataStoreInfo = getCatalog().getDataStoreByName("st", STATIONS_STORE_NAME);
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess =
                dataStoreInfo.getDataStore(null);
        assertTrue(dataAccess instanceof AppSchemaDataAccess);
        AppSchemaDataAccess appSchemaStore = (AppSchemaDataAccess) dataAccess;
        MongoDataStore mongoStore = null;
        List<Name> names = appSchemaStore.getNames();
        for (Name ename : names) {
            FeatureTypeMapping mapping = appSchemaStore.getMappingByName(ename);
            if (mapping.getSourceDatastoreId().filter(id -> "data_source".equals(id)).isPresent()) {
                DataAccess internalStore = mapping.getSource().getDataStore();
                if (internalStore instanceof MongoDataStore) {
                    mongoStore = (MongoDataStore) internalStore;
                    break;
                }
            }
        }
        assertNotNull(mongoStore);
        clearSchemas(mongoStore);
        String restPath =
                String.format(
                        "rest/workspaces/st/appschemastores/%s/rebuildMongoSchemas"
                                + "?ids=58e5889ce4b02461ad5af081,58e5889ce4b02461ad5af080",
                        STATIONS_STORE_NAME);
        MockHttpServletResponse servletResponse = postAsServletResponse(restPath);
        assertEquals(200, servletResponse.getStatus());
        // [stations]
        List<String> typeNames = mongoStore.getSchemaStore().typeNames();
        assertTrue(CollectionUtils.isNotEmpty(typeNames));
        // check inferredAttribute
        SimpleFeatureType featureType =
                mongoStore.getSchemaStore().retrieveSchema(new NameImpl("stations"));
        AttributeDescriptor attributeDescriptor = featureType.getDescriptor("inferredAttribute");
        assertNotNull(attributeDescriptor);
        clearSchemas(mongoStore);
        // return to default schema build
        restPath =
                String.format(
                        "rest/workspaces/st/appschemastores/%s/rebuildMongoSchemas" + "?max=1",
                        STATIONS_STORE_NAME);
        servletResponse = postAsServletResponse(restPath);
        assertEquals(200, servletResponse.getStatus());
        featureType = mongoStore.getSchemaStore().retrieveSchema(new NameImpl("stations"));
        attributeDescriptor = featureType.getDescriptor("inferredAttribute");
        assertNull(attributeDescriptor);
        // check all items schema generation
        restPath =
                String.format(
                        "rest/workspaces/st/appschemastores/%s/datastores/%s/rebuildMongoSchemas"
                                + "?max=-1&schema=%s",
                        STATIONS_STORE_NAME, "data_source", STATIONS_COLLECTION_NAME);
        servletResponse = postAsServletResponse(restPath);
        assertEquals(200, servletResponse.getStatus());
        // [stations]
        typeNames = mongoStore.getSchemaStore().typeNames();
        assertTrue(CollectionUtils.isNotEmpty(typeNames));
        // check inferredAttribute
        featureType = mongoStore.getSchemaStore().retrieveSchema(new NameImpl("stations"));
        attributeDescriptor = featureType.getDescriptor("inferredAttribute");
        assertNotNull(attributeDescriptor);
    }

    private void clearSchemas(MongoDataStore mongoStore) {
        final MongoDataStore finalMongoStore = mongoStore;
        finalMongoStore
                .getSchemaStore()
                .typeNames()
                .forEach(
                        tn -> {
                            try {
                                finalMongoStore.getSchemaStore().deleteSchema(new NameImpl(tn));
                            } catch (IOException e) {
                                // ok
                            }
                        });
        assertTrue(finalMongoStore.getSchemaStore().typeNames().isEmpty());
    }

    protected MockHttpServletResponse postAsServletResponse(String path) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType(MediaType.TEXT_XML_VALUE);
        return dispatch(request);
    }
}
