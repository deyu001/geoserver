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

package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.ServerBusyPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** @author Alessio Fabiani, GeoSolutions */
public class ServerBusyPageTest extends GeoServerWicketTestSupport {

    long defaultTimeout;

    @Before
    public void clearTimeout() {
        defaultTimeout = GeoServerConfigurationLock.DEFAULT_TRY_LOCK_TIMEOUT_MS;
        GeoServerConfigurationLock.DEFAULT_TRY_LOCK_TIMEOUT_MS = 1;
    }

    @After
    public void reinstateTimeout() {
        GeoServerConfigurationLock.DEFAULT_TRY_LOCK_TIMEOUT_MS = defaultTimeout;
    }

    @Test
    public void testStoreEditServerBusyPage() throws Exception {
        login();

        List<GrantedAuthority> l = new ArrayList<>();
        l.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));

        final LockType type = LockType.WRITE;
        final GeoServerConfigurationLock locker =
                (GeoServerConfigurationLock) GeoServerExtensions.bean("configurationLock");
        locker.setEnabled(true);
        locker.unlock(); // just to be on the safe side

        AtomicBoolean acquired = new AtomicBoolean(false);
        AtomicBoolean release = new AtomicBoolean(false);
        if (locker != null) {
            Thread configWriter =
                    new Thread("Config-writer") {

                        public void run() {
                            // Acquiring Configuration Lock as another user
                            locker.lock(type);
                            acquired.set(true);

                            try {
                                while (!release.get()) {
                                    Thread.sleep(50);
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } finally {
                                locker.unlock();
                            }
                        }
                    };
            configWriter.start();

            try {
                while (!acquired.get()) {
                    Thread.sleep(50);
                }

                tester.startPage(
                        DataAccessEditPage.class,
                        new PageParameters().add("wsName", "cite").add("storeName", "cite"));
                tester.assertRenderedPage(ServerBusyPage.class);
                tester.assertNoErrorMessage();
            } finally {
                release.set(true);
            }
        }
    }
}
