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

package org.geogig.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.locationtech.geogig.cli.test.functional.CLITestContextBuilder;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.FindTreeChild;
import org.locationtech.geogig.plumbing.LsTreeOp;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.porcelain.LogOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.repository.impl.GlobalContextBuilder;
import org.locationtech.geogig.test.TestPlatform;
import org.locationtech.geogig.test.integration.RepositoryTestCase;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;

@TestSetup(run = TestSetupFrequency.ONCE)
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // zz_testCommitsSurviveShutDown run the latest
public class WFSIntegrationTest extends WFSTestSupport {

    private static final String NAMESPACE = "http://geogig.org";

    private static final String WORKSPACE = "geogig";

    private static final String STORE = "geogigstore";

    private static TestHelper helper;

    private static class TestHelper extends RepositoryTestCase {
        @Override
        protected Context createInjector() {
            TestPlatform testPlatform = (TestPlatform) createPlatform();
            GlobalContextBuilder.builder(new CLITestContextBuilder(testPlatform));
            return GlobalContextBuilder.builder().build();
        }

        @Override
        protected void setUpInternal() throws Exception {}

        File getRepositoryDirectory() {
            return super.repositoryDirectory;
        }
    }

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        helper = new TestHelper();
        helper.repositoryTempFolder.create();
        helper.setUp();
        configureGeogigDataStore();
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        if (helper != null) {
            helper.tearDown();
            helper.repositoryTempFolder.delete();
        }
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // oevrride to avoid creating all the default feature types but call testData.setUp() only
        // instead
        testData.setUp();
    }

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        namespaces.put(WORKSPACE, NAMESPACE);
    }

    private void configureGeogigDataStore() throws Exception {

        helper.insertAndAdd(helper.lines1);
        helper.getGeogig().command(CommitOp.class).call();

        Catalog catalog = getCatalog();
        CatalogFactory factory = catalog.getFactory();
        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix(WORKSPACE);
        ns.setURI(NAMESPACE);
        catalog.add(ns);
        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName(ns.getName());
        catalog.add(ws);

        DataStoreInfo ds = factory.createDataStore();
        ds.setEnabled(true);
        ds.setDescription("Test Geogig DataStore");
        ds.setName(STORE);
        ds.setType(GeoGigDataStoreFactory.DISPLAY_NAME);
        ds.setWorkspace(ws);
        Map<String, Serializable> connParams = ds.getConnectionParameters();

        Optional<URI> geogigDir = helper.getGeogig().command(ResolveGeogigURI.class).call();
        File repositoryUrl = new File(geogigDir.get()).getParentFile();
        assertTrue(repositoryUrl.exists() && repositoryUrl.isDirectory());

        connParams.put(GeoGigDataStoreFactory.REPOSITORY.key, repositoryUrl);
        connParams.put(GeoGigDataStoreFactory.DEFAULT_NAMESPACE.key, ns.getURI());
        catalog.add(ds);

        DataStoreInfo dsInfo = catalog.getDataStoreByName(WORKSPACE, STORE);
        assertNotNull(dsInfo);
        assertEquals(GeoGigDataStoreFactory.DISPLAY_NAME, dsInfo.getType());
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = dsInfo.getDataStore(null);
        assertNotNull(dataStore);
        assertTrue(dataStore instanceof GeoGigDataStore);

        FeatureTypeInfo fti = factory.createFeatureType();
        fti.setNamespace(ns);
        fti.setCatalog(catalog);
        fti.setStore(dsInfo);
        fti.setSRS("EPSG:4326");
        fti.setName("Lines");
        fti.setAdvertised(true);
        fti.setEnabled(true);
        fti.setCqlFilter("INCLUDE");
        fti.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        ReferencedEnvelope bounds =
                new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326"));
        fti.setNativeBoundingBox(bounds);
        fti.setLatLonBoundingBox(bounds);
        catalog.add(fti);

        fti = catalog.getFeatureType(fti.getId());

        FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
        featureSource = fti.getFeatureSource(null, null);
        assertNotNull(featureSource);
    }

    @Test
    public void testInsert() throws Exception {
        Document dom;
        dom =
                getAsDOM(
                        "wfs?version=1.1.0&request=getfeature&typename=geogig:Lines&srsName=EPSG:4326&");
        int initial = dom.getElementsByTagName("geogig:Lines").getLength();

        dom = insert();
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());

        dom =
                getAsDOM(
                        "wfs?version=1.1.0&request=getfeature&typename=geogig:Lines&srsName=EPSG:4326&");
        // print(dom);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        assertEquals(1 + initial, dom.getElementsByTagName("geogig:Lines").getLength());
    }

    private Document insert() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" " //
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + " xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + " xmlns:geogig=\""
                        + NAMESPACE
                        + "\">" //
                        + "<wfs:Insert>" //
                        + "<geogig:Lines gml:id=\"Lines.1000\">" //
                        + "    <geogig:sp>StringProp new</geogig:sp>" //
                        + "    <geogig:ip>999</geogig:ip>" //
                        + "    <geogig:pp>" //
                        + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">" //
                        + "            <gml:posList>1.0 1.0 2.0 2.0</gml:posList>" //
                        + "        </gml:LineString>" //
                        + "    </geogig:pp>" //
                        + "</geogig:Lines>" //
                        + "</wfs:Insert>" //
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        return dom;
    }

    @Test
    public void testUpdate() throws Exception {
        Document dom = update();
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());

        dom =
                getAsDOM(
                        "wfs?version=1.1.0&request=getfeature&typename=geogig:Lines"
                                + "&"
                                + "cql_filter=ip%3D1000");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        assertEquals(1, dom.getElementsByTagName("geogig:Lines").getLength());
    }

    private Document update() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\"" //
                        + " xmlns:geogig=\""
                        + NAMESPACE
                        + "\"" //
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\"" //
                        + " xmlns:gml=\"http://www.opengis.net/gml\"" //
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">" //
                        + " <wfs:Update typeName=\"geogig:Lines\">" //
                        + "   <wfs:Property>" //
                        + "     <wfs:Name>geogig:pp</wfs:Name>" //
                        + "     <wfs:Value>"
                        + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">" //
                        + "            <gml:posList>1 2 3 4</gml:posList>" //
                        + "        </gml:LineString>" //
                        + "     </wfs:Value>" //
                        + "   </wfs:Property>" //
                        + "   <ogc:Filter>" //
                        + "     <ogc:PropertyIsEqualTo>" //
                        + "       <ogc:PropertyName>ip</ogc:PropertyName>" //
                        + "       <ogc:Literal>1000</ogc:Literal>" //
                        + "     </ogc:PropertyIsEqualTo>" //
                        + "   </ogc:Filter>" //
                        + " </wfs:Update>" //
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        return dom;
    }

    /**
     * Test case to expose issue https://github.com/boundlessgeo/geogig/issues/310 "Editing Features
     * changes the feature type"
     *
     * @see #testUpdateDoesntChangeFeatureType()
     */
    @Test
    public void testInsertDoesntChangeFeatureType() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" " //
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + " xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + " xmlns:geogig=\""
                        + NAMESPACE
                        + "\">" //
                        + "<wfs:Insert>" //
                        + "<geogig:Lines gml:id=\"Lines.1000\">" //
                        + "    <geogig:sp>added</geogig:sp>" //
                        + "    <geogig:ip>7</geogig:ip>" //
                        + "    <geogig:pp>" //
                        + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">" //
                        + "            <gml:posList>1 2 3 4</gml:posList>" //
                        + "        </gml:LineString>" //
                        + "    </geogig:pp>" //
                        + "</geogig:Lines>" //
                        + "</wfs:Insert>" //
                        + "</wfs:Transaction>";

        GeoGIG geogig = helper.getGeogig();
        final NodeRef initialTypeTreeRef =
                geogig.command(FindTreeChild.class).setChildPath("Lines").call().get();
        assertFalse(initialTypeTreeRef.getMetadataId().isNull());

        Document dom = postAsDOM("wfs", xml);
        try {
            assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        } catch (AssertionError e) {
            print(dom);
            throw e;
        }

        try {
            assertEquals(
                    "1",
                    getFirstElementByTagName(dom, "wfs:totalInserted")
                            .getFirstChild()
                            .getNodeValue());
        } catch (AssertionError e) {
            print(dom);
            throw e;
        }

        final NodeRef finalTypeTreeRef =
                geogig.command(FindTreeChild.class).setChildPath("Lines").call().get();
        assertFalse(initialTypeTreeRef.equals(finalTypeTreeRef));
        assertFalse(finalTypeTreeRef.getMetadataId().isNull());

        assertEquals(
                "Feature type tree metadataId shouuldn't change upon edits",
                initialTypeTreeRef.getMetadataId(),
                finalTypeTreeRef.getMetadataId());

        Iterator<NodeRef> featureRefs = geogig.command(LsTreeOp.class).setReference("Lines").call();
        while (featureRefs.hasNext()) {
            NodeRef ref = featureRefs.next();
            assertEquals(finalTypeTreeRef.getMetadataId(), ref.getMetadataId());
            assertFalse(ref.toString(), ref.getNode().getMetadataId().isPresent());
        }
    }

    /**
     * Test case to expose issue https://github.com/boundlessgeo/geogig/issues/310 "Editing Features
     * changes the feature type"
     *
     * @see #testInsertDoesntChangeFeatureType()
     */
    @Test
    public void testUpdateDoesntChangeFeatureType() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\"" //
                        + " xmlns:geogig=\""
                        + NAMESPACE
                        + "\"" //
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\"" //
                        + " xmlns:gml=\"http://www.opengis.net/gml\"" //
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">" //
                        + " <wfs:Update typeName=\"geogig:Lines\">" //
                        + "   <wfs:Property>" //
                        + "     <wfs:Name>geogig:pp</wfs:Name>" //
                        + "     <wfs:Value>"
                        + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">" //
                        + "            <gml:posList>3 4 5 6</gml:posList>" //
                        + "        </gml:LineString>" //
                        + "     </wfs:Value>" //
                        + "   </wfs:Property>" //
                        + "   <ogc:Filter>" //
                        + "     <ogc:PropertyIsEqualTo>" //
                        + "       <ogc:PropertyName>ip</ogc:PropertyName>" //
                        + "       <ogc:Literal>1000</ogc:Literal>" //
                        + "     </ogc:PropertyIsEqualTo>" //
                        + "   </ogc:Filter>" //
                        + " </wfs:Update>" //
                        + "</wfs:Transaction>";

        GeoGIG geogig = helper.getGeogig();
        final NodeRef initialTypeTreeRef =
                geogig.command(FindTreeChild.class).setChildPath("Lines").call().get();
        assertFalse(initialTypeTreeRef.getMetadataId().isNull());

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());

        final NodeRef finalTypeTreeRef =
                geogig.command(FindTreeChild.class).setChildPath("Lines").call().get();
        assertFalse(initialTypeTreeRef.equals(finalTypeTreeRef));
        assertFalse(finalTypeTreeRef.getMetadataId().isNull());

        assertEquals(
                "Feature type tree metadataId shouuldn't change upon edits",
                initialTypeTreeRef.getMetadataId(),
                finalTypeTreeRef.getMetadataId());
        Iterator<NodeRef> featureRefs = geogig.command(LsTreeOp.class).setReference("Lines").call();
        while (featureRefs.hasNext()) {
            NodeRef ref = featureRefs.next();
            assertEquals(finalTypeTreeRef.getMetadataId(), ref.getMetadataId());
            assertFalse(ref.toString(), ref.getNode().getMetadataId().isPresent());
        }
    }

    // HACK: forcing this test to run the latest through
    // @FixMethodOrder(MethodSorters.NAME_ASCENDING) as it calls destroyGeoserver() and we really
    // want to use TestSetupFrequency.ONCE
    @Test
    public void zz_testCommitsSurviveShutDown() throws Exception {
        GeoGIG geogig = helper.getGeogig();

        insert();
        update();

        List<RevCommit> expected = ImmutableList.copyOf(geogig.command(LogOp.class).call());

        File repoDir = helper.getRepositoryDirectory();
        assertTrue(repoDir.exists() && repoDir.isDirectory());
        // shut down server
        destroyGeoServer();

        TestPlatform testPlatform = new TestPlatform(repoDir);
        Context context = new CLITestContextBuilder(testPlatform).build();
        GeoGIG geogig2 = new GeoGIG(context);
        try {
            assertNotNull(geogig2.getRepository());
            List<RevCommit> actual = ImmutableList.copyOf(geogig2.command(LogOp.class).call());
            assertEquals(expected, actual);
        } finally {
            geogig2.close();
        }
    }
}
