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

package org.geoserver.security.password;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.auth.GeoServerRootAuthenticationProvider;
import org.geoserver.security.validation.MasterPasswordChangeException;
import org.geoserver.test.SystemTest;
import org.geotools.util.URLs;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

// @TestSetup(run=TestSetupFrequency.REPEAT)
@Category(SystemTest.class)
public class MasterPasswordChangeTest extends GeoServerSecurityTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        applicationContext
                .getBeanFactory()
                .registerSingleton("testMasterPasswordProvider", new TestMasterPasswordProvider());
    }

    @Test
    public void testMasterPasswordChange() throws Exception {
        // keytool -storepasswd -new geoserver1 -storepass geoserver -storetype jceks -keystore
        // geoserver.jks

        // We need to enable Master Root login first
        MasterPasswordProviderConfig masterPasswordConfig =
                getSecurityManager()
                        .loadMasterPassswordProviderConfig(
                                getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(true);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);

        String masterPWAsString = getMasterPassword();
        MasterPasswordConfig config = getSecurityManager().getMasterPasswordConfig();

        URLMasterPasswordProviderConfig mpConfig =
                (URLMasterPasswordProviderConfig)
                        getSecurityManager()
                                .loadMasterPassswordProviderConfig(config.getProviderName());

        assertTrue(
                mpConfig.getURL()
                        .toString()
                        .endsWith(URLMasterPasswordProviderConfig.MASTER_PASSWD_FILENAME));
        getSecurityManager().getKeyStoreProvider().reloadKeyStore();

        try {
            getSecurityManager().saveMasterPasswordConfig(config, null, null, null);
            fail();
        } catch (MasterPasswordChangeException ex) {
        }

        ///// First change using rw_url
        mpConfig = new URLMasterPasswordProviderConfig();
        mpConfig.setName("rw");
        mpConfig.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        mpConfig.setReadOnly(false);

        File tmp = new File(getSecurityManager().get("security").dir(), "mpw1.properties");
        mpConfig.setURL(URLs.fileToUrl(tmp));
        getSecurityManager().saveMasterPasswordProviderConfig(mpConfig);

        config = getSecurityManager().getMasterPasswordConfig();
        config.setProviderName(mpConfig.getName());
        getSecurityManager()
                .saveMasterPasswordConfig(
                        config,
                        masterPWAsString.toCharArray(),
                        "geoserver1".toCharArray(),
                        "geoserver1".toCharArray());
        assertEquals("geoserver1", getMasterPassword());

        getSecurityManager().getKeyStoreProvider().getConfigPasswordKey();

        /////////////// change with ro url
        mpConfig = new URLMasterPasswordProviderConfig();
        mpConfig.setName("ro");
        mpConfig.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        mpConfig.setReadOnly(true);

        tmp = new File(getSecurityManager().get("security").dir(), "mpw2.properties");
        mpConfig.setURL(URLs.fileToUrl(tmp));

        FileUtils.writeStringToFile(tmp, "geoserver2", "UTF-8");

        getSecurityManager().saveMasterPasswordProviderConfig(mpConfig);
        config = getSecurityManager().getMasterPasswordConfig();
        config.setProviderName("ro");

        getSecurityManager()
                .saveMasterPasswordConfig(
                        config, "geoserver1".toCharArray(), null, "geoserver2".toCharArray());

        assertEquals("geoserver2", getMasterPassword());
        getSecurityManager().getKeyStoreProvider().getConfigPasswordKey();

        /////////////////////// change simulating spring injection
        MasterPasswordProviderConfig mpConfig2 = new MasterPasswordProviderConfig();
        mpConfig2.setLoginEnabled(true);
        mpConfig2.setName("test");
        mpConfig2.setClassName(TestMasterPasswordProvider.class.getCanonicalName());
        getSecurityManager().saveMasterPasswordProviderConfig(mpConfig2);

        config = getSecurityManager().getMasterPasswordConfig();
        config.setProviderName("test");
        getSecurityManager()
                .saveMasterPasswordConfig(
                        config,
                        "geoserver2".toCharArray(),
                        "geoserver3".toCharArray(),
                        "geoserver3".toCharArray());

        // now, a geoserver restart should appear, simulate with
        getSecurityManager().getKeyStoreProvider().commitMasterPasswordChange();

        //////////
        assertEquals("geoserver3", getMasterPassword());
        getSecurityManager().getKeyStoreProvider().getConfigPasswordKey();

        /// Test root login after master password change
        Authentication auth = new UsernamePasswordAuthenticationToken("root", "geoserver3");
        GeoServerRootAuthenticationProvider authProvider =
                new GeoServerRootAuthenticationProvider();
        authProvider.setSecurityManager(getSecurityManager());
        auth = authProvider.authenticate(auth);
        assertTrue(auth.isAuthenticated());

        auth = new UsernamePasswordAuthenticationToken("root", "abcdefghijk");
        assertNull(authProvider.authenticate(auth));
        assertFalse(auth.isAuthenticated());
    }
}
