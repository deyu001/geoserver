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

package org.geoserver.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.servlet.Filter;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.BruteForcePreventionConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMethod;

public class BruteForceAttackTest extends GeoServerSystemTestSupport {

    private static final String HELLO_GET_REQUEST =
            "ows?service=hello&request=hello&message=Hello_World";

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        // add the hello world services, in order to have a legit service to hit
        springContextLocations.add("classpath*:/org/geoserver/ows/applicationContext.xml");
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // only setup security, no data needed
        testData.setUpSecurity();
    }

    @Override
    protected List<Filter> getFilters() {
        // enable security
        return Arrays.asList(applicationContext.getBean(GeoServerSecurityFilterChainProxy.class));
    }

    @Before
    public void resetAuthentication() {
        setRequestAuth(null, null);
    }

    @Before
    public void resetBruteForceAttackConfig() throws Exception {
        GeoServerSecurityManager manager =
                applicationContext.getBean(GeoServerSecurityManager.class);
        final SecurityManagerConfig securityConfig = manager.getSecurityConfig();
        BruteForcePreventionConfig bruteForceConfig = securityConfig.getBruteForcePrevention();
        bruteForceConfig.setEnabled(true);
        // one second fixed delay
        bruteForceConfig.setMinDelaySeconds(1);
        bruteForceConfig.setMaxDelaySeconds(1);
        bruteForceConfig.setMaxBlockedThreads(100);
        bruteForceConfig.setWhitelistedMasks(Collections.emptyList());
        manager.saveSecurityConfig(securityConfig);
    }

    @Test
    public void testLoginDelay() throws Exception {
        // successful login, no wait (cannot actually test it)
        setRequestAuth("admin", "geoserver");
        assertEquals(200, getAsServletResponse(HELLO_GET_REQUEST).getStatus());
        // failing login, at least one second wait
        setRequestAuth("admin", "foobar");
        long start = System.currentTimeMillis();
        assertEquals(401, getAsServletResponse(HELLO_GET_REQUEST).getStatus());
        long end = System.currentTimeMillis();
        assertThat((end - start), greaterThan(1000l));
    }

    @Test
    public void testParallelLogin() throws Exception {
        testParallelLogin("Unauthorized", i -> "foo");
    }

    @Test
    public void testTooManyBlockedThreads() throws Exception {
        // configure it to allow only one thread in the wait list
        GeoServerSecurityManager manager =
                applicationContext.getBean(GeoServerSecurityManager.class);
        final SecurityManagerConfig securityConfig = manager.getSecurityConfig();
        BruteForcePreventionConfig bruteForceConfig = securityConfig.getBruteForcePrevention();
        bruteForceConfig.setMaxBlockedThreads(1);
        manager.saveSecurityConfig(securityConfig);

        // hit with many different users
        testParallelLogin("Unauthorized", i -> "foo" + i);
    }

    private void testParallelLogin(
            String expectedMessage, Function<Integer, String> userNameGenerator)
            throws InterruptedException, ExecutionException {
        // idea, setup several threads to do the same failing auth in parallel,
        // ensuring they are all ready to go at the same time using a latch
        final int NTHREADS = 32;
        ExecutorService service = Executors.newFixedThreadPool(NTHREADS);
        CountDownLatch latch = new CountDownLatch(NTHREADS);
        AtomicInteger concurrentLoginsPrevented = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < NTHREADS; i++) {
            final int idx = i;
            Future<?> future =
                    service.submit(
                            () -> {
                                // mark and ready and wait for others
                                latch.countDown();
                                latch.await();

                                // execute request timing how long it took
                                MockHttpServletRequest request = createRequest(HELLO_GET_REQUEST);
                                request.setMethod(RequestMethod.GET.toString());
                                request.setContent(new byte[] {});
                                String userName = userNameGenerator.apply(idx);
                                String token = userName + ":foobar";
                                request.addHeader(
                                        "Authorization",
                                        "Basic "
                                                + new String(
                                                        Base64.encodeBase64(token.getBytes())));
                                MockHttpServletResponse response = dispatch(request, "UTF-8");

                                // check the response and see the error message
                                assertEquals(401, response.getStatus());
                                final String message = response.getErrorMessage();
                                // System.out.println(message);
                                if (message.contains(expectedMessage)) {
                                    concurrentLoginsPrevented.incrementAndGet();
                                }

                                return null;
                            });
            futures.add(future);
        }

        // wait for termination
        for (Future<?> future : futures) {
            future.get();
        }
        long awaitTime = System.currentTimeMillis() - start;
        service.shutdown();

        // now, either the threads all serialized and waited (extremely unlikely, but
        // not impossible) or at least one got bumped immediately with a concurrent login message
        assertTrue(awaitTime > NTHREADS * 1000 || concurrentLoginsPrevented.get() > 0);
    }

    // "Too many failed logins waiting on delay already";
}
