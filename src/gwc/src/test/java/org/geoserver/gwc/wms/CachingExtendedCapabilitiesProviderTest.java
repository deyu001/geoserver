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

package org.geoserver.gwc.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

public class CachingExtendedCapabilitiesProviderTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("../wms/src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    @Test
    public void testCapabilitiesContributedInternalDTD() throws Exception {

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        DocumentType doctype = dom.getDoctype();
        assertNotNull(doctype);

        assertEquals("WMT_MS_Capabilities", doctype.getName());

        String systemId = doctype.getSystemId();
        assertEquals(
                "../wms/src/test/resources/geoserver/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd",
                systemId);

        String internalSubset = doctype.getInternalSubset();
        assertTrue(internalSubset == null || !internalSubset.contains("TileSet"));

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
        dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        doctype = dom.getDoctype();
        assertNotNull(doctype);
        assertEquals("WMT_MS_Capabilities", doctype.getName());
        systemId = doctype.getSystemId();

        assertEquals(
                "../wms/src/test/resources/geoserver/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd",
                systemId);

        internalSubset = doctype.getInternalSubset();

        assertNotNull(internalSubset);
        assertTrue(
                internalSubset,
                internalSubset.trim().startsWith("<!ELEMENT VendorSpecificCapabilities"));
        assertTrue(internalSubset, internalSubset.contains("(TileSet*)"));
        assertTrue(
                internalSubset,
                internalSubset.contains(
                        "<!ELEMENT TileSet (SRS,BoundingBox?,Resolutions,Width,Height,Format,Layers*,Styles*)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Resolutions (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Width (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Height (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Layers (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Styles (#PCDATA)>"));
    }

    @Test
    public void testTileSets() throws Exception {
        final int numLayers;
        {
            int validLayers = 0;
            List<LayerInfo> layers = getCatalog().getLayers();
            for (LayerInfo l : layers) {
                if (CatalogConfiguration.isLayerExposable(l)) {
                    ++validLayers;
                }
            }
            numLayers = validLayers;
        }
        final int numCRSs = 2; // 4326 and 900913
        final int numFormats = 2; // png, jpeg
        final int numTileSets = numLayers * numCRSs * numFormats;

        String tileSetPath = "/WMT_MS_Capabilities/Capability/VendorSpecificCapabilities/TileSet";

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);

        assertXpathNotExists(tileSetPath, dom);

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
        dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);

        assertXpathExists(tileSetPath, dom);

        assertXpathEvaluatesTo(String.valueOf(numTileSets), "count(" + tileSetPath + ")", dom);

        assertXpathExists(tileSetPath + "[1]/SRS", dom);
        assertXpathExists(tileSetPath + "[1]/BoundingBox", dom);
        assertXpathExists(tileSetPath + "[1]/Resolutions", dom);
        assertXpathExists(tileSetPath + "[1]/Width", dom);
        assertXpathExists(tileSetPath + "[1]/Height", dom);
        assertXpathExists(tileSetPath + "[1]/Format", dom);
        assertXpathExists(tileSetPath + "[1]/Layers", dom);
        assertXpathExists(tileSetPath + "[1]/Styles", dom);

        // Test RequireTiledParameter==false
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
        GWC.get().getConfig().setRequireTiledParameter(false);
        dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        assertXpathExists(tileSetPath, dom);

        assertXpathEvaluatesTo(String.valueOf(numTileSets), "count(" + tileSetPath + ")", dom);

        assertXpathExists(tileSetPath + "[1]/SRS", dom);
        assertXpathExists(tileSetPath + "[1]/BoundingBox", dom);
        assertXpathExists(tileSetPath + "[1]/Resolutions", dom);
        assertXpathExists(tileSetPath + "[1]/Width", dom);
        assertXpathExists(tileSetPath + "[1]/Height", dom);
        assertXpathExists(tileSetPath + "[1]/Format", dom);
        assertXpathExists(tileSetPath + "[1]/Layers", dom);
        assertXpathExists(tileSetPath + "[1]/Styles", dom);
    }

    @Test
    public void testLocalWorkspaceIntegration() throws Exception {

        final String tileSetPath =
                "//WMT_MS_Capabilities/Capability/VendorSpecificCapabilities/TileSet";
        final String localName = MockData.BASIC_POLYGONS.getLocalPart();
        final String qualifiedName = super.getLayerId(MockData.BASIC_POLYGONS);

        Document dom;

        dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        assertXpathExists(tileSetPath + "/Layers[text() = '" + qualifiedName + "']", dom);

        dom = dom(get("cite/wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        assertXpathExists(tileSetPath + "/Layers[text() = '" + localName + "']", dom);
    }
}
