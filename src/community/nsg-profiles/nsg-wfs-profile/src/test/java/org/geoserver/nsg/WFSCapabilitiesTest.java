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

package org.geoserver.nsg;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.Arrays;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class WFSCapabilitiesTest extends WFS20TestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        service.getSRS().add("4326");
        service.getSRS().add("3395");
        geoServer.save(service);
    }

    @Test
    public void testCapabilitiesExtras() throws Exception {
        Document dom = getAsDOM("wfs?service=WFS&version=2.0.0&request=GetCapabilities");
        print(dom);

        // profiles
        assertXpathExists(
                "//ows:ServiceIdentification[ows:Profile='"
                        + NSGWFSExtendedCapabilitiesProvider.NSG_BASIC
                        + "']",
                dom);

        // constraints
        assertXpathExists(
                "//ows:OperationsMetadata/ows:Constraint[@name='"
                        + NSGWFSExtendedCapabilitiesProvider.IMPLEMENTS_FEATURE_VERSIONING
                        + "' and ows:DefaultValue='TRUE']",
                dom);
        assertXpathExists(
                "//ows:OperationsMetadata/ows:Constraint[@name='"
                        + NSGWFSExtendedCapabilitiesProvider.IMPLEMENTS_ENHANCED_PAGING
                        + "' and ows:DefaultValue='TRUE']",
                dom);

        // ensure "version" is there for all operations besides capabilities (10 basic plus paged
        // results)
        assertXpathEvaluatesTo(
                "11",
                "count(//ows:OperationsMetadata/ows:Operation[@name!='GetCapabilities']/ows"
                        + ":Parameter[@name='version']/ows:AllowedValues[ows:Value[1]='2.0.0'])",
                dom);

        // ensure the srsName parameter is available on GetFeature, GetFeatureWithLock, Transaction
        for (String operation : NSGWFSExtendedCapabilitiesProvider.SRS_OPERATIONS) {
            // the two configured SRSs plus one coming from the data
            for (Integer srsCode : Arrays.asList(4326, 3395, 32615)) {
                String xpath =
                        String.format(
                                "//ows:OperationsMetadata/ows:Operation[@name = "
                                        + "'%s']/ows:Parameter[@name='srsName' "
                                        + "and ows:AllowedValues/ows:Value='urn:ogc:def:crs:EPSG::%d']",
                                operation, srsCode);
                assertXpathExists(xpath, dom);
            }
        }

        // ensure the timeout parameter is configured on expected operations
        for (String operation : NSGWFSExtendedCapabilitiesProvider.TIMEOUT_OPERATIONS) {
            String xpath =
                    String.format(
                            "//ows:OperationsMetadata/ows:Operation[@name = "
                                    + "'%s']/ows:Parameter[@name='Timeout' and ows:DefaultValue='300']",
                            operation);
            assertXpathExists(xpath, dom);
        }

        // check the PageResults operation is there
        assertXpathExists("//ows:OperationsMetadata/ows:Operation[@name = 'PageResults']", dom);
        assertXpathExists(
                "//ows:OperationsMetadata/ows:Operation[@name = "
                        + "'PageResults']/ows:Parameter[@name='outputFormat' and ows:DefaultValue='"
                        + NSGWFSExtendedCapabilitiesProvider.GML32_FORMAT
                        + "']",
                dom);
    }
}
