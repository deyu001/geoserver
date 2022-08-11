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

package org.geoserver.inspire;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireTestSupport.assertInspireCommonScenario1Response;
import static org.geoserver.inspire.InspireTestSupport.assertInspireMetadataUrlResponse;
import static org.geoserver.inspire.InspireTestSupport.assertSchemaLocationContains;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class ViewServicesTestSupport extends GeoServerSystemTestSupport {

    protected abstract String getGetCapabilitiesRequestPath();

    protected abstract String getMetadataUrl();

    protected abstract String getMetadataType();

    protected abstract String getLanguage();

    protected abstract String getAlternateMetadataType();

    protected abstract ServiceInfo getServiceInfo();

    protected abstract String getInspireNameSpace();

    protected abstract String getInspireSchema();

    @Test
    public void testNoInspireSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testCreateExtCapsOff() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testExtCapsWithFullSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, getInspireNameSpace(), getInspireSchema());
        final Element extendedCaps = (Element) nodeList.item(0);
        assertInspireCommonScenario1Response(
                extendedCaps, getMetadataUrl(), getMetadataType(), getLanguage());
    }

    @Test
    public void testReloadSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        getGeoServer().reload();
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals(
                "Number of INSPIRE ExtendedCapabilities elements after settings reload",
                1,
                nodeList.getLength());
    }

    // Test ExtendedCapabilities is not produced if required settings missing
    @Test
    public void testNoMetadataUrl() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    // Test ExtendedCapabilities response when optional settings missing
    @Test
    public void testNoMediaType() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MediaType");
        assertEquals("Number of MediaType elements", 0, nodeList.getLength());
    }

    // If settings were created with older version of INSPIRE extension before
    // the on/off check box setting existed we create the extended capabilities
    // if the other required settings exist and don't if they don't
    @Test
    public void testCreateExtCapMissingWithRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
    }

    @Test
    public void testCreateExtCapMissingWithoutRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testChangeMediaType() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        Element mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(mdUrl, getMetadataUrl(), getMetadataType());
        serviceInfo.getMetadata().put(SERVICE_METADATA_TYPE.key, getAlternateMetadataType());
        getGeoServer().save(serviceInfo);
        dom = getAsDOM(getGetCapabilitiesRequestPath());
        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(mdUrl, getMetadataUrl(), getAlternateMetadataType());
    }
}
