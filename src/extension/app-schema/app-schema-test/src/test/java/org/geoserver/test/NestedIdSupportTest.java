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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.appschema.jdbc.NestedFilterToSQL;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.filter.ComplexFilterSplitter;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.w3c.dom.Document;

/**
 * Test whether nested Id's can be used in a filter.
 *
 * @author Niels Charlier, Curtin University Of Technology *
 */
public class NestedIdSupportTest extends AbstractAppSchemaTestSupport {

    @Override
    protected NestedIdSupportTestData createTestData() {
        return new NestedIdSupportTestData();
    }

    /** Test Nested Id with Feature Chaining */
    @Test
    public void testNestedIdFeatureChaining() {
        String xml =
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "<wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "<ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "        <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/gsml:ControlledConcept/@gml:id</ogc:PropertyName>"
                        + "        <ogc:Literal>cc.1</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + " </ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("MappedFeature: WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf4", "wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
    }

    /** Test Nested Id with InlineMapping */
    @Test
    public void testNestedIdInlineMapping() {
        String xml =
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "<wfs:Query typeName=\"gsml:Borehole\">"
                        + "<ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "        <ogc:PropertyName>gsml:indexData/gsml:BoreholeDetails/@gml:id</ogc:PropertyName>"
                        + "        <ogc:Literal>bh.details.11.sp</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + " </ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("Borehole: WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:Borehole", doc);
        assertXpathEvaluatesTo(
                "11", "wfs:FeatureCollection/gml:featureMember/gsml:Borehole/@gml:id", doc);
    }

    @Test
    public void testNestedFiltersEncoding() throws IOException, FilterToSQLException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        FeatureSource fs = ftInfo.getFeatureSource(new NullProgressListener(), null);
        AppSchemaDataAccess da = (AppSchemaDataAccess) fs.getDataStore();
        FeatureTypeMapping rootMapping = da.getMappingByNameOrElement(ftInfo.getQualifiedName());

        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        JDBCDataStore store = (JDBCDataStore) rootMapping.getSource().getDataStore();
        NestedFilterToSQL nestedFilterToSQL = createNestedFilterEncoder(rootMapping);

        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(rootMapping.getNamespaces());

        /*
         * test filter on nested ID
         */
        PropertyIsEqualTo nestedIdFilter =
                ff.equals(
                        ff.property(
                                "gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/gsml:ControlledConcept/@gml:id"),
                        ff.literal("cc.1"));

        // Filter involves a single nested attribute --> can be encoded
        ComplexFilterSplitter splitter =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitter.visit(nestedIdFilter, null);
        Filter preFilter = splitter.getFilterPre();
        Filter postFilter = splitter.getFilterPost();

        assertEquals(nestedIdFilter, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        Filter unrolled = AppSchemaDataAccess.unrollFilter(nestedIdFilter, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        String encodedFilter = nestedFilterToSQL.encodeToString(unrolled);

        // this is the generated query in PostGIS, but the test limits to check the presence of the
        // a few keywords, as the actual SQL is dependent on the underlying database
        // EXISTS (SELECT "chain_link_3"."PKEY"
        //      FROM "appschematest"."CONTROLLEDCONCEPT" "chain_link_3"
        //           INNER JOIN "appschematest"."COMPOSITIONPART" "chain_link_2" ON
        // "chain_link_2"."ROW_ID" = "chain_link_3"."COMPOSITION_ID"
        //           INNER JOIN "appschematest"."GEOLOGICUNIT" "chain_link_1" ON
        // "chain_link_1"."COMPONENTPART_ID" = "chain_link_2"."ROW_ID"
        //      WHERE "chain_link_3"."GML_ID" = 'cc.1' AND
        // "appschematest"."MAPPEDFEATUREPROPERTYFILE"."GEOLOGIC_UNIT_ID" = "chain_link_1"."GML_ID")
        assertTrue(
                encodedFilter.matches("^EXISTS.*SELECT.*FROM.*INNER JOIN.*INNER JOIN.*WHERE.*$"));
        assertContainsFeatures(fs.getFeatures(nestedIdFilter), "mf4");
    }
}
