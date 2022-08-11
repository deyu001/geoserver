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

package org.vfny.geoserver.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.DataLinkInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class ResponseUtilsTest {

    void createAppContext(String proxyBaseUrl) {
        SettingsInfo settings = createNiceMock(SettingsInfo.class);
        expect(settings.getProxyBaseUrl()).andReturn(proxyBaseUrl).anyTimes();
        replay(settings);

        GeoServer geoServer = createNiceMock(GeoServer.class);
        expect(geoServer.getGlobal()).andReturn(new GeoServerInfoImpl());
        expect(geoServer.getSettings()).andReturn(settings).anyTimes();
        replay(geoServer);

        ProxifyingURLMangler mangler = new ProxifyingURLMangler(geoServer);
        ApplicationContext appContext = createNiceMock(ApplicationContext.class);
        expect(appContext.getBeanNamesForType(URLMangler.class))
                .andReturn(new String[] {"mangler"});
        expect(appContext.getBean("mangler")).andReturn(mangler).anyTimes();
        replay(appContext);
        GeoServerExtensionsHelper.init(appContext);
    }

    @After
    public void clearAppContext() {
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testProxyMetadataURL() throws Exception {
        createAppContext("http://foo.org/geoserver");
        MetadataLinkInfo link = new MetadataLinkInfoImpl();
        link.setContent("http://bar.com/geoserver/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyMetadataLink(link, "http://localhost/geoserver");
        assertEquals(link.getContent(), url);
    }

    @Test
    public void testProxyMetadataURLBackReference() throws Exception {
        createAppContext("http://foo.org/geoserver");
        MetadataLinkInfo link = new MetadataLinkInfoImpl();
        link.setContent("/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyMetadataLink(link, "http://localhost/geoserver");
        assertEquals("http://foo.org/geoserver/metadata.xml?foo=bar", url);
    }

    @Test
    public void testProxyMetadataURLBackReferenceNoProxyBaseUrl() throws Exception {
        createAppContext(null);
        MetadataLinkInfo link = new MetadataLinkInfoImpl();
        link.setContent("/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyMetadataLink(link, "http://localhost/geoserver");
        assertEquals("http://localhost/geoserver/metadata.xml?foo=bar", url);
    }

    @Test
    public void testProxyDataURL() throws Exception {
        createAppContext("http://foo.org/geoserver");
        DataLinkInfo link = new DataLinkInfoImpl();
        link.setContent("http://bar.com/geoserver/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyDataLink(link, "http://localhost/geoserver");
        assertEquals(link.getContent(), url);
    }

    @Test
    public void testProxyDataURLBackReference() throws Exception {
        createAppContext("http://foo.org/geoserver");
        DataLinkInfo link = new DataLinkInfoImpl();
        link.setContent("/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyDataLink(link, "http://localhost/geoserver");
        assertEquals("http://foo.org/geoserver/metadata.xml?foo=bar", url);
    }

    @Test
    public void testDataURLBackReferenceNoProxyBaseUrl() throws Exception {
        createAppContext(null);
        DataLinkInfo link = new DataLinkInfoImpl();
        link.setContent("/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyDataLink(link, "http://localhost/geoserver");
        assertEquals("http://localhost/geoserver/metadata.xml?foo=bar", url);
    }
}
