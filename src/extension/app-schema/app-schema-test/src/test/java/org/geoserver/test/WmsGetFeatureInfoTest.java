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

import org.geoserver.wms.WMSInfo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class WmsGetFeatureInfoTest extends AbstractAppSchemaTestSupport {

    public WmsGetFeatureInfoTest() throws Exception {
        super();
    }

    @Before
    public void setupAdvancedProjectionHandling() {
        // make sure GetFeatureInfo is not deactivated (this will only update the global service)
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(false);
        getGeoServer().save(wms);
    }

    @Override
    protected WmsSupportMockData createTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("positionalaccuracy21", "styles/positionalaccuracy21.sld");
        return mockData;
    }

    @Test
    public void testGetCapabilities() {
        Document doc = getAsDOM("wms?request=GetCapabilities");
        LOGGER.info("WMS =GetCapabilities response:\n" + prettyString(doc));
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());
        assertXpathCount(1, "//wms:Layer/wms:Name[.='gsml:MappedFeature']", doc);
        assertXpathCount(
                1, "//wms:GetFeatureInfo/wms:Format[.='application/vnd.ogc.gml/3.1.1']", doc);
    }

    @Test
    public void testGetFeatureInfoText() throws Exception {
        String str =
                getAsString(
                        "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100");
        LOGGER.info("WMS =GetFeatureInfo Text response:\n" + str);
        assertTrue(str.contains("FeatureImpl:MappedFeature<MappedFeatureType id=mf2>"));
    }

    @Test
    public void testGetFeatureInfoGML() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info("WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf2", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        validateGet(request);
    }

    @Test
    public void testGetFeatureInfoGMLReprojection() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:3857&BBOX=-144715.338031256,6800125.45439731,0,6891041.72389159&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info("WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf2", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // check that features coordinates where reprojected to EPSG:3857
        assertXpathMatches(
                ".*3857",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature[@gml:id='mf2']/gsml:shape/gml:Polygon/@srsName",
                doc);
        validateGet(request);
        // disable features reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the request
        doc = getAsDOM(request);
        // check that features were not reprojected and still in EPSG:4326
        assertXpathMatches(
                ".*4326",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature[@gml:id='mf2']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    @Test
    public void testGetFeatureInfoGML21() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&styles=positionalaccuracy21&SRS=EPSG:4326&BBOX=-1.3,53,0,53.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info("WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf4", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        validateGet(request);
    }

    @Test
    public void testGetFeatureInfoHTML() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=text/html");
        LOGGER.info("WMS =GetFeatureInfo HTML response:\n" + prettyString(doc));
        assertXpathCount(1, "/html/body/table/tr/td[.='mf2']", doc);
        assertXpathCount(
                1,
                "/html/body/table/tr/td/table[caption/.='CGI_TermValuePropertyType']/tr/td/table[caption/.='CGI_TermValueType']",
                doc);
        assertXpathCount(
                1,
                "/html/body/table/tr/td/table[caption/.='GeologicFeaturePropertyType']/tr/td/table[caption/.='GeologicUnitType']/tr/th[.='gml:description']",
                doc);
    }
}
