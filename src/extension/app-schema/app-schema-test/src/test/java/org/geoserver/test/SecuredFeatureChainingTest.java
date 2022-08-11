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


package org.geoserver.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.decorators.ReadOnlyDataAccess;
import org.geoserver.security.decorators.SecuredDataStoreInfo;
import org.geotools.data.DataAccess;
import org.geotools.data.complex.expression.FeaturePropertyAccessorFactory;
import org.geotools.data.util.NullProgressListener;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.util.factory.Hints;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.w3c.dom.Document;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * WFS GetFeature to test secured feature with GeoServer.
 *
 * @author Victor Tey (CSIRO Earth Science and Resource Engineering)
 */
public class SecuredFeatureChainingTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureChainingMockData createTestData() {
        return new FeatureChainingMockData();
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);

        springContextLocations.add("classpath:/test-data/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite_readfilter", "cite", null, Arrays.asList("ROLE_DUMMY"));
        addUser("cite_readatts", "cite", null, Arrays.asList("ROLE_DUMMY"));

        NamespaceSupport ns = new NamespaceSupport();
        Map nsMap = ((FeatureChainingMockData) testData).getNamespaces();
        for (Object o : nsMap.entrySet()) {
            Entry entry = (Entry) o;
            String prefix = (String) entry.getKey();
            String namespace = (String) entry.getValue();
            ns.declarePrefix(prefix, namespace);
        }
        Hints hints = new Hints();
        hints.put(FeaturePropertyAccessorFactory.NAMESPACE_CONTEXT, ns);
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(hints);

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo gu = catalog.getFeatureTypeByName("gsml:GeologicUnit");

        // limits for mr readfilter
        Filter f =
                ff.equal(
                        new AttributeExpressionImpl("gsml:purpose", ns),
                        ff.literal("instance"),
                        false);
        tam.putLimits(
                "cite_readfilter",
                gu,
                new VectorAccessLimits(CatalogMode.HIDE, null, f, null, null));

        List<PropertyName> readAtts =
                Arrays.asList(
                        ff.property("gsml:composition"), ff.property("gsml:outcropCharacter"));

        tam.putLimits(
                "cite_readatts",
                gu,
                new VectorAccessLimits(CatalogMode.HIDE, readAtts, f, null, null));
    }

    /** Test that denormalized data reports the correct number of features */
    @Test
    public void testDenormalisedFeaturesCount() {
        setRequestAuth("cite_readatts", "cite");
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit"
                                + "&maxFeatures=3&resultType=hits");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:GeologicUnit&maxFeatures=3 response:\n"
                        + prettyString(doc));
        assertXpathEvaluatesTo("3", "//wfs:FeatureCollection/@numberOfFeatures", doc);
    }

    /** Test that denormalized data reports the right output */
    @Test
    public void testSecureFeatureContent() {
        setRequestAuth("cite_readatts", "cite");
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&maxFeatures=3");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:GeologicUnit&maxFeatures=3 response:\n"
                        + prettyString(doc));
        assertXpathCount(3, "//gsml:GeologicUnit", doc);
        assertXpathCount(0, "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:exposureColor", doc);
        assertXpathCount(0, "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:exposureColor", doc);
        assertXpathCount(0, "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:exposureColor", doc);

        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:composition", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:composition", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:composition", doc);
    }

    /**
     * Tests that {@link SecuredDataStoreInfo#getDataStore(org.opengis.util.ProgressListener)}
     * correctly returns a {@link DataAccess} instance.
     */
    @Test
    public void testSecuredDataStoreInfo() throws IOException {
        login("cite_readatts", "cite", "ROLE_DUMMY");

        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("gsml:GeologicUnit");
        assertNotNull(ftInfo);

        DataAccess<? extends FeatureType, ? extends Feature> dataAccess =
                ftInfo.getStore().getDataStore(new NullProgressListener());
        assertNotNull(dataAccess);
        assertTrue(dataAccess instanceof ReadOnlyDataAccess);
    }
}
