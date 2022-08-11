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

package org.geoserver.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Test;
import org.w3c.dom.Document;

public class UniqueProcessTest extends WPSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // setup an H2 datastore for the purpose of checking database delegation
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("h2");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/h2");
        cat.add(ds);

        SimpleFeatureSource fs3 = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init(fs3.getSchema());
        // remove the property types H2 has troubles with (including multiple geometries)
        tb.remove("surfaceProperty");
        tb.remove("curveProperty");
        tb.remove("uriProperty");
        store.createSchema(tb.buildFeatureType());
        SimpleFeatureStore targetFeatureStore =
                (SimpleFeatureStore)
                        store.getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE.getLocalPart());
        targetFeatureStore.addFeatures(fs3.getFeatures());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        FeatureTypeInfo ft = cb.buildFeatureType(targetFeatureStore);
        cat.add(ft);
    }

    @Test
    public void testUnique() throws Exception {
        String xml = getUniqueRequest(getLayerId(MockData.PRIMITIVEGEOFEATURE));

        Document doc = postAsDOM(root(), xml);
        // print(doc);
        assertXpathEvaluatesTo("5", "count(//gml:value)", doc);
    }

    @Test
    public void testUniqueDatabaseDelegation() throws Exception {
        org.apache.log4j.Logger logger =
                org.apache.log4j.Logger.getLogger(JDBCDataStore.class.getPackage().getName());
        logger.setLevel(Level.DEBUG);
        logger.removeAllAppenders();
        AtomicBoolean foundDistinctCall = new AtomicBoolean();
        logger.addAppender(
                new AppenderSkeleton() {
                    @Override
                    public void close() {}

                    @Override
                    public boolean requiresLayout() {
                        return false;
                    }

                    @Override
                    protected void append(LoggingEvent event) {
                        boolean curr = foundDistinctCall.get();
                        String message = event.getRenderedMessage();
                        curr |=
                                message != null
                                        && message.startsWith("SELECT distinct(\"intProperty\")");
                        foundDistinctCall.set(curr);
                    }
                });

        String xml = getUniqueRequest("gs:PrimitiveGeoFeature");

        Document doc = postAsDOM(root(), xml);
        assertXpathEvaluatesTo("5", "count(//gml:value)", doc);
        assertTrue(
                "Function has not been delegated to database, could not find select dinstinct in the logs",
                foundDistinctCall.get());
    }

    public String getUniqueRequest(String layerId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>gs:Unique</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>features</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                + "            <wfs:Query typeName=\""
                + layerId
                + "\"/>\n"
                + "          </wfs:GetFeature>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>attribute</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>intProperty</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput>\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n"
                + "</wps:Execute>";
    }
}
