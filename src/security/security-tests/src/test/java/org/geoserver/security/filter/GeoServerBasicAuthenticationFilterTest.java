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

package org.geoserver.security.filter;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.codec.Hex;

public class GeoServerBasicAuthenticationFilterTest {
    public static final String USERNAME = "admin:";
    public static final String PASSWORD = "geoserver";
    private static final int NTHREADS = 8;
    private String expected;
    private GeoServerBasicAuthenticationFilter authenticationFilter;

    @Before
    public void setUp() throws Exception {
        authenticationFilter = createAuthenticationFilter();
        StringBuffer buff = new StringBuffer(PASSWORD);
        buff.append(":");
        buff.append(authenticationFilter.getName());
        MessageDigest digest = MessageDigest.getInstance("MD5");
        String digestString =
                new String(Hex.encode(digest.digest(buff.toString().getBytes("utf-8"))));
        expected = USERNAME + digestString;
    }

    @Test
    public void testMultiThreadGetCacheKey() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(NTHREADS);
        List<Future<Boolean>> list = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            Callable<Boolean> worker = new AuthenticationCallable(authenticationFilter);
            Future<Boolean> submit = executor.submit(worker);
            list.add(submit);
        }

        for (Future<Boolean> future : list) {
            future.get();
        }

        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
    }

    private GeoServerBasicAuthenticationFilter createAuthenticationFilter() {
        GeoServerBasicAuthenticationFilter authenticationFilter =
                new GeoServerBasicAuthenticationFilter();
        GeoServerSecurityManager sm = null;
        try {
            sm = new GeoServerSecurityManager(new GeoServerDataDirectory(new File("target")));
            authenticationFilter.setSecurityManager(sm);
            BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
            authenticationFilter.initializeFromConfig(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize authentication authenticationFilter.");
        }
        return authenticationFilter;
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request =
                new GeoServerAbstractTestSupport.GeoServerMockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setContextPath("/geoserver");
        request.setRemoteAddr("127.0.0.1");
        String token = "admin:" + PASSWORD;
        request.addHeader(
                "Authorization", "Basic " + new String(Base64.encodeBase64(token.getBytes())));
        return request;
    }

    private class AuthenticationCallable implements Callable<Boolean> {
        private GeoServerBasicAuthenticationFilter authenticationFilter;

        private AuthenticationCallable(GeoServerBasicAuthenticationFilter authenticationFilter) {
            this.authenticationFilter = authenticationFilter;
        }

        @Override
        public Boolean call() throws Exception {
            MockHttpServletRequest request = createRequest();
            String result = authenticationFilter.getCacheKey(request);
            Assert.assertEquals(expected, result);
            return true;
        }
    }
}
