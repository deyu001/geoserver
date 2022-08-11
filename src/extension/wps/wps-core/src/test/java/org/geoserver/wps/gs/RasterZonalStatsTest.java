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

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class RasterZonalStatsTest extends WPSTestSupport {

    static final double EPS = 1e-6;

    public static QName RESTRICTED = new QName(MockData.SF_URI, "restricted", MockData.SF_PREFIX);

    public static QName DEM = new QName(MockData.SF_URI, "sfdem", MockData.SF_PREFIX);

    // put everything in the SF namespace because the GML encoder always applies the "feature"
    // prefix to
    // the output GML, so we need all tests to generate GML in the same namespace
    public static QName TASMANIA_BM_ZONES =
            new QName(MockData.SF_URI, "BmZones", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add extra data used by this test
        addWcs11Coverages(testData);

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(LayerProperty.SRS, 26713);

        testData.addRasterLayer(DEM, "sfdem.tiff", ".tiff", props, getClass(), getCatalog());
        testData.addVectorLayer(
                RESTRICTED, props, "restricted.properties", getClass(), getCatalog());
        testData.addVectorLayer(
                TASMANIA_BM_ZONES, props, "tazdem_zones.properties", getClass(), getCatalog());
    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("feature", "http://cite.opengeospatial.org/gmlsf");
    }

    @Test
    public void testStatisticsTazDem() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:RasterZonalStatistics</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>data</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                        + "            <ows:Identifier>"
                        + getLayerId(MockData.TASMANIA_DEM)
                        + "</ows:Identifier>\n"
                        + "            <wcs:DomainSubset>\n"
                        + "              <gml:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                        + "                <ows:LowerCorner>145 -43</ows:LowerCorner>\n"
                        + "                <ows:UpperCorner>146 -41</ows:UpperCorner>\n"
                        + "              </gml:BoundingBox>\n"
                        + "            </wcs:DomainSubset>\n"
                        + "            <wcs:Output format=\"image/tiff\"/>\n"
                        + "          </wcs:GetCoverage>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>zones</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\""
                        + getLayerId(TASMANIA_BM_ZONES)
                        + "\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>statistics</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document dom = postAsDOM(root(), xml);
        // print(dom);

        // half of the cells
        assertXpathEvaluatesTo("14400", "//feature:BmZones[feature:z_cat=1]/feature:count", dom);
        // quarter of the cells
        assertXpathEvaluatesTo("7200", "//feature:BmZones[feature:z_cat=2]/feature:count", dom);
        assertXpathEvaluatesTo("7200", "//feature:BmZones[feature:z_cat=3]/feature:count", dom);
        // this can actually be counted by the naked eye
        assertXpathEvaluatesTo("77", "//feature:BmZones[feature:z_cat=4]/feature:count", dom);
        // all the cells
        assertXpathEvaluatesTo("28800", "//feature:BmZones[feature:z_cat=5]/feature:count", dom);

        assertXpathEvaluatesTo("860.0", "//feature:BmZones[feature:z_cat=4]/feature:min", dom);
        assertXpathEvaluatesTo("1357.0", "//feature:BmZones[feature:z_cat=4]/feature:max", dom);
        assertXpathEvaluatesTo("84511.0", "//feature:BmZones[feature:z_cat=4]/feature:sum", dom);
        assertXpathEvaluatesTo(
                "1097.5454545454547", "//feature:BmZones[feature:z_cat=4]/feature:avg", dom);
        assertXpathEvaluatesTo(
                "108.38400851341224", "//feature:BmZones[feature:z_cat=4]/feature:stddev", dom);
    }

    @Test
    public void testStatisticsSfDem() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:RasterZonalStatistics</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>data</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                        + "            <ows:Identifier>sf:sfdem</ows:Identifier>\n"
                        + "            <wcs:DomainSubset>\n"
                        + "              <gml:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#26713\">\n"
                        + "                <ows:LowerCorner>589980.0 4913700.0</ows:LowerCorner>\n"
                        + "                <ows:UpperCorner>609000.0 4928010.0</ows:UpperCorner>\n"
                        + "              </gml:BoundingBox>\n"
                        + "            </wcs:DomainSubset>\n"
                        + "            <wcs:Output format=\"image/tiff\"/>\n"
                        + "          </wcs:GetCoverage>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>zones</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"sf:restricted\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>statistics</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document dom = postAsDOM(root(), xml);
        // print(dom);

        assertXpathEvaluatesTo("424", "//feature:restricted[feature:z_cat=1]/feature:count", dom);
        assertXpathEvaluatesTo("218", "//feature:restricted[feature:z_cat=2]/feature:count", dom);
        assertXpathEvaluatesTo("18629", "//feature:restricted[feature:z_cat=3]/feature:count", dom);
        assertXpathEvaluatesTo("1697", "//feature:restricted[feature:z_cat=4]/feature:count", dom);

        assertXpathEvaluatesTo("1281.0", "//feature:restricted[feature:z_cat=3]/feature:min", dom);
        assertXpathEvaluatesTo("1695.0", "//feature:restricted[feature:z_cat=3]/feature:max", dom);
        assertXpathEvaluatesTo(
                "2.743144E7", "//feature:restricted[feature:z_cat=3]/feature:sum", dom);
        assertXpathEvaluatesTo(
                "1472.5127489398305", "//feature:restricted[feature:z_cat=3]/feature:avg", dom);
        assertXpathEvaluatesTo(
                "93.61336732245832", "//feature:restricted[feature:z_cat=3]/feature:stddev", dom);
    }
}
