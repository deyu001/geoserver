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
 * This test ensures that substitution groups in XSD schemas work as expected. Specifically,
 * test:DomainSet is substitutable for gml:domainSet and should appear in the response. This test
 * case is loosely based upon WXXM 2 schemas, but significant portions of the schemas were removed
 * or changed to simplify this test case
 *
 * @author Aaron Braeckel (National Center for Atmospheric Research)
 */
public class SubstitutionGroupCoverageWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new SubstitutionGroupCoverageMockData();
    }

    @Test
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&outputFormat=gml32&typeName=test:DiscreteCoverage";
        Document doc = getAsDOM(path);
        LOGGER.info(
                "WFS GetFeature, typename=test:DiscreteCoverage response:\n" + prettyString(doc));
        validateGet(path);
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(1, "//test:DiscreteCoverage", doc);
        String id = "a9274057-604d-427e-87f7-e6c9d846ceb5";
        assertXpathEvaluatesTo(id, "(//test:DiscreteCoverage)[1]/@gml:id", doc);
        assertXpathCount(1, "//test:DiscreteCoverage/test:domainSet", doc);
        assertXpathCount(1, "//test:DiscreteCoverage/test:domainSet/test:DomainObject", doc);
        assertXpathCount(
                1, "//test:DiscreteCoverage/test:domainSet/test:DomainObject/test:elements", doc);
        assertXpathCount(
                2, "//test:DiscreteCoverage/test:domainSet/test:DomainObject/test:elements/*", doc);
        assertXpathEvaluatesTo(
                "-0.6476 81.0527",
                "//test:DiscreteCoverage/test:domainSet/"
                        + "test:DomainObject/test:elements[1]/gml:Point/gml:pos",
                doc);
    }
}
