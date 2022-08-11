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

package org.geoserver.config.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests XStreamPersister integration with other beans in the app context
 *
 * @author Andrea Aime - GeoSolutions
 */
public class XStreamPersisterIntegrationTest extends GeoServerSystemTestSupport {

    private XStreamPersister persister;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Before
    public void setupPersister() {
        persister = new XStreamPersister();
        persister.setEncryptPasswordFields(true);
    }

    @Test
    public void testWmsStorePasswordEncryption() throws Exception {
        WMSStoreInfo wms = buildWmsStore();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persister.save(wms, out);
        Document dom = dom(new ByteArrayInputStream(out.toByteArray()));
        // print(dom);

        // check password has been encrypted
        XMLAssert.assertXpathExists("/wmsStore/password", dom);
        XMLAssert.assertXpathNotExists("/wmsStore[password = 'password']", dom);
        XMLAssert.assertXpathExists("/wmsStore[starts-with(password, 'crypt1:')]", dom);

        WMSStoreInfo loaded =
                persister.load(new ByteArrayInputStream(out.toByteArray()), WMSStoreInfo.class);
        assertEquals("password", loaded.getPassword());
    }

    @Test
    public void testWmsStoreBackwardsCompatibility() throws Exception {
        WMSStoreInfo wms = buildWmsStore();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // save with no encryption
        persister.setEncryptPasswordFields(false);
        persister.save(wms, out);
        Document dom = dom(new ByteArrayInputStream(out.toByteArray()));
        print(dom);

        // check password has not been encrypted
        XMLAssert.assertXpathExists("/wmsStore/password", dom);
        XMLAssert.assertXpathExists("/wmsStore[password = 'password']", dom);

        // load back with a password encrypting persister, should fall back reading plain text
        // password
        persister.setEncryptPasswordFields(true);
        WMSStoreInfo loaded =
                persister.load(new ByteArrayInputStream(out.toByteArray()), WMSStoreInfo.class);
        assertEquals("password", loaded.getPassword());

        // just to be thorough test also loading with no password encryption
        persister.setEncryptPasswordFields(false);
        WMSStoreInfo loaded2 =
                persister.load(new ByteArrayInputStream(out.toByteArray()), WMSStoreInfo.class);
        assertEquals("password", loaded2.getPassword());
    }

    private WMSStoreInfo buildWmsStore() {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        WMSStoreInfo wms = cFactory.createWebMapServer();
        wms.setName("bar");
        wms.setWorkspace(ws);
        wms.setCapabilitiesURL("http://fake.host/wms?request=GetCapabilities&service=wms");
        wms.setUsername("user");
        wms.setPassword("password");

        return wms;
    }
}
