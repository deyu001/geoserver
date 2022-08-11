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

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Validation testing with GeoServer
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class ValidationTest extends AbstractAppSchemaTestSupport {

    @Override
    protected ValidationTestMockData createTestData() {
        return new ValidationTestMockData();
    }

    /** Test that when minOccur=0 the validation should let it pass */
    @Test
    public void testAttributeMinOccur0() {
        Document doc = null;
        doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gml:name", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gml:name", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gml:name", doc);

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "myBody1",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value[@codeSpace='myBodyCodespace1']",
                doc);
        assertXpathEvaluatesTo(
                "compositionName",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:composition/gsml:CompositionPart/gsml:lithology[1]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "myBody1",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:composition/gsml:CompositionPart/gsml:lithology[2]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "myBody1",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:rank[@codeSpace='myBodyCodespace1']",
                doc);

        assertXpathCount(
                0,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "compositionName",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:composition/gsml:CompositionPart/gsml:lithology[1]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathCount(
                0,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:composition/gsml:CompositionPart/gsml:lithology[2]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:rank[@codeSpace='myBodyCodespace2']",
                doc);

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "myBody3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value[@codeSpace='myBodyCodespace3']",
                doc);
        assertXpathEvaluatesTo(
                "compositionName",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:composition/gsml:CompositionPart/gsml:lithology[1]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "myBody3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:composition/gsml:CompositionPart/gsml:lithology[2]/gsml:ControlledConcept/gml:name",
                doc);

        assertXpathEvaluatesTo(
                "myBody3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:rank[@codeSpace='myBodyCodespace3']",
                doc);
    }

    @Test
    public void testSimpleContentInteger() {
        Document doc = null;
        doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=er:Commodity");
        LOGGER.info("WFS GetFeature&typename=er:Commodity response:\n" + prettyString(doc));
        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.1']/gml:name", doc);
        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.1']/er:commodityRank", doc);
        assertXpathEvaluatesTo(
                "myName1", "//er:Commodity[@gml:id='er.commodity.gu.1']/gml:name", doc);
        assertXpathEvaluatesTo(
                "1", "//er:Commodity[@gml:id='er.commodity.gu.1']/er:commodityRank", doc);

        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.2']/gml:name", doc);
        assertXpathCount(0, "//er:Commodity[@gml:id='er.commodity.gu.2']/er:commodityRank", doc);
        assertXpathEvaluatesTo(
                "myName2", "//er:Commodity[@gml:id='er.commodity.gu.2']/gml:name", doc);

        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.3']/gml:name", doc);
        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.3']/er:commodityRank", doc);
        assertXpathEvaluatesTo(
                "myName3", "//er:Commodity[@gml:id='er.commodity.gu.3']/gml:name", doc);
        assertXpathEvaluatesTo(
                "3", "//er:Commodity[@gml:id='er.commodity.gu.3']/er:commodityRank", doc);
    }

    /** Test minOccur=1 and the attribute should always be encoded even when empty. */
    @Test
    public void testAttributeMinOccur1() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathCount(3, "//gsml:MappedFeature", doc);

        // with minOccur = 1 and null value, an empty tag would be encoded
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.1']", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.1']/gsml:observationMethod",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.1']/gsml:observationMethod/gsml:CGI_TermValue",
                doc);

        // the rest should be encoded as normal
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.2']", doc);
        assertXpathEvaluatesTo(
                "observation2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.2']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);

        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.3']", doc);
        assertXpathEvaluatesTo(
                "observation3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.3']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
    }
}
