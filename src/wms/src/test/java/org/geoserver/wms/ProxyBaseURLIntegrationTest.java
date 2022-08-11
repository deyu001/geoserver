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

package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class ProxyBaseURLIntegrationTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    /** Do the links in getcaps respect the proxy base url? */
    @Test
    public void testProxyBaseUrl() throws Exception {
        // setup the proxy base
        final String proxyBaseUrl = "http://localhost/proxy";
        GeoServerInfo gs = getGeoServer().getGlobal();
        gs.getSettings().setProxyBaseUrl(proxyBaseUrl);
        getGeoServer().save(gs);

        // setup the wms online resource
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        String geoserverSite = "http://www.geoserver.org";
        wms.setOnlineResource(geoserverSite);
        getGeoServer().save(wms);

        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1");
        // print(dom);

        String serviceOnlineRes = "/WMT_MS_Capabilities/Service/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(geoserverSite, serviceOnlineRes, dom);

        String getCapsGet =
                "/WMT_MS_Capabilities/Capability/Request/GetCapabilities/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getCapsGet, dom);

        String getCapsPost =
                "/WMT_MS_Capabilities/Capability/Request/GetCapabilities/DCPType/HTTP/Post/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getCapsPost, dom);

        String getMapGet =
                "/WMT_MS_Capabilities/Capability/Request/GetMap/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getMapGet, dom);

        String getFeatureInfoGet =
                "/WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getFeatureInfoGet, dom);

        String getFeatureInfoPost =
                "/WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/DCPType/HTTP/Post/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getFeatureInfoPost, dom);

        String describeLayerGet =
                "/WMT_MS_Capabilities/Capability/Request/DescribeLayer/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", describeLayerGet, dom);

        String getLegentGet =
                "/WMT_MS_Capabilities/Capability/Request/GetLegendGraphic/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getLegentGet, dom);
    }
}
