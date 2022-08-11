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

package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.List;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesAuthorityURLAndIdentifierTest extends WMSTestSupport {

    private void addAuthUrl(final String name, final String url, List<AuthorityURLInfo> target) {
        AuthorityURLInfo auth = new AuthorityURL();
        auth.setName(name);
        auth.setHref(url);
        target.add(auth);
    }

    private void addIdentifier(
            final String authName, final String id, List<LayerIdentifierInfo> target) {
        LayerIdentifierInfo identifier = new LayerIdentifier();
        identifier.setAuthority(authName);
        identifier.setIdentifier(id);
        target.add(identifier);
    }

    @Test
    public void testRootLayer() throws Exception {
        WMSInfo serviceInfo = getWMS().getServiceInfo();
        addAuthUrl("rootAuth1", "http://geoserver/wms/auth1", serviceInfo.getAuthorityURLs());
        addAuthUrl("rootAuth2", "http://geoserver/wms/auth2", serviceInfo.getAuthorityURLs());
        addIdentifier("rootAuth1", "rootId1", serviceInfo.getIdentifiers());
        addIdentifier("rootAuth2", "rootId2", serviceInfo.getIdentifiers());
        getGeoServer().save(serviceInfo);

        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.1.1", true);

        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/AuthorityURL[@name = 'rootAuth1']", doc);
        assertXpathEvaluatesTo(
                "http://geoserver/wms/auth1",
                "/WMT_MS_Capabilities/Capability/Layer/AuthorityURL[@name = 'rootAuth1']/OnlineResource/@xlink:href",
                doc);

        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/AuthorityURL[@name = 'rootAuth2']", doc);
        assertXpathEvaluatesTo(
                "http://geoserver/wms/auth2",
                "/WMT_MS_Capabilities/Capability/Layer/AuthorityURL[@name = 'rootAuth2']/OnlineResource/@xlink:href",
                doc);

        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Identifier[@authority = 'rootAuth1']", doc);
        assertXpathEvaluatesTo(
                "rootId1",
                "/WMT_MS_Capabilities/Capability/Layer/Identifier[@authority = 'rootAuth1']",
                doc);

        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Identifier[@authority = 'rootAuth2']", doc);
        assertXpathEvaluatesTo(
                "rootId2",
                "/WMT_MS_Capabilities/Capability/Layer/Identifier[@authority = 'rootAuth2']",
                doc);
    }

    @Test
    public void testLayer() throws Exception {

        String layerId = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        addAuthUrl("layerAuth1", "http://geoserver/wms/auth1", layer.getAuthorityURLs());
        addIdentifier("layerAuth1", "layerId1", layer.getIdentifiers());
        getCatalog().save(layer);

        String layerName = MockData.PRIMITIVEGEOFEATURE.getLocalPart();
        Document doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0",
                        true);

        assertXpathExists(
                "//Layer[Name='" + layerName + "']/AuthorityURL[@name = 'layerAuth1']", doc);
        assertXpathEvaluatesTo(
                "http://geoserver/wms/auth1",
                "//Layer[Name='"
                        + layerName
                        + "']/AuthorityURL[@name = 'layerAuth1']/OnlineResource/@xlink:href",
                doc);

        assertXpathExists(
                "//Layer[Name='" + layerName + "']/Identifier[@authority = 'layerAuth1']", doc);
        assertXpathEvaluatesTo(
                "layerId1",
                "//Layer[Name='" + layerName + "']/Identifier[@authority = 'layerAuth1']",
                doc);
    }
}
