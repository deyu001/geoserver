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

package org.geoserver.flow.controller;

import static org.junit.Assert.assertEquals;

import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;
import org.junit.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class IpFlowControllerTest extends AbstractFlowControllerTest {

    @Test
    public void testConcurrentRequestsSingleIPAddress() {
        // an ip based flow controller that will allow just one request at a time
        IpFlowController controller = new IpFlowController(1);
        String ipAddress = "127.0.0.1";
        Request firstRequest = buildIpRequest(ipAddress, "");
        FlowControllerTestingThread tSample =
                new FlowControllerTestingThread(firstRequest, 0, 0, controller);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);

        assertEquals(ThreadState.COMPLETE, tSample.state);

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        // make three testing threads that will "process" forever, and will use the ip to identify
        // themselves
        // as the same client, until we interrupt them
        FlowControllerTestingThread t1 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, controller);
        FlowControllerTestingThread t2 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, controller);

        try {
            // start threads making sure every one of them managed to block somewhere before
            // starting the next one
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            t2.start();
            waitBlocked(t2, MAX_WAIT);

            assertEquals(ThreadState.PROCESSING, t1.state);
            assertEquals(ThreadState.STARTED, t2.state);

            // let t1 go and wait until its termination. This should allow t2 to go
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);

            t2.interrupt();
        } catch (Exception e) {
            // System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }

    @Test
    public void testUserAndIPAddressFlowControl() {
        // an ip based flow controller that will allow just one request at a time
        IpFlowController ipController = new IpFlowController(1);
        UserConcurrentFlowController userController = new UserConcurrentFlowController(1);
        String ipAddress = "127.0.0.1";
        Request firstRequest = buildIpRequest(ipAddress, "");
        FlowControllerTestingThread tSample =
                new FlowControllerTestingThread(firstRequest, 0, 0, userController, ipController);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);

        assertEquals(ThreadState.COMPLETE, tSample.state);

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        // make three testing threads that will "process" forever, and will use the ip to identify
        // themselves
        // as the same client, until we interrupt them
        FlowControllerTestingThread t1 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, ipController);
        FlowControllerTestingThread t2 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, ipController);

        try {
            // start threads making sure every one of them managed to block somewhere before
            // starting the next one
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            t2.start();
            waitBlocked(t2, MAX_WAIT);

            assertEquals(ThreadState.PROCESSING, t1.state);
            assertEquals(ThreadState.STARTED, t2.state);

            // let t1 go and wait until its termination. This should allow t2 to go
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);

            t2.interrupt();
            waitTerminated(t2, MAX_WAIT);
            assertEquals(ThreadState.COMPLETE, t2.state);
        } catch (Exception e) {
            // System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }

    // Test 2 remote addresses that are reported as the same, but have gone through a proxy. These
    // two should not queue up
    @Test
    public void testConcurrentProxiedIPAddresses() {
        IpFlowController controller = new IpFlowController(1);
        String ipAddress = "192.168.1.1";

        Request firstRequest = buildIpRequest(ipAddress, "");

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        FlowControllerTestingThread t1 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, "192.168.1.2"), 0, Long.MAX_VALUE, controller);
        FlowControllerTestingThread t2 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, "192.168.1.3"), 0, Long.MAX_VALUE, controller);

        try {
            // start threads making sure every one of them managed to block somewhere before
            // starting the next one
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            t2.start();
            waitBlocked(t2, MAX_WAIT);

            // Both threads are processing, there is no queuing in this case
            assertEquals(ThreadState.PROCESSING, t1.state);
            assertEquals(ThreadState.PROCESSING, t2.state);

            // let t1 go and wait until its termination. This should allow t2 to go
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);

            t2.interrupt();
        } catch (Exception e) {
            // System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }
}
