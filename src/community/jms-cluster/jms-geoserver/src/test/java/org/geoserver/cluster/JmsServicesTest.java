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

package org.geoserver.cluster;

import static org.geoserver.cluster.JmsEventsListener.getMessagesForHandler;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.UUID;
import javax.jms.Message;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cluster.impl.events.configuration.JMSEventType;
import org.geoserver.cluster.impl.events.configuration.JMSServiceModifyEvent;
import org.geoserver.cluster.impl.handlers.configuration.JMSServiceHandlerSPI;
import org.geoserver.config.ServiceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.After;
import org.junit.Test;

/** Tests related with services events. */
public final class JmsServicesTest extends GeoServerSystemTestSupport {

    private static final String SERVICE_EVENT_HANDLER_KEY = "JMSServiceHandlerSPI";

    private WorkspaceInfoImpl workspace;
    private static JMSEventHandler<String, JMSServiceModifyEvent> serviceHandler;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        // adding our test spring context
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // create the test workspace if it doesn't exsist
        workspace = new WorkspaceInfoImpl();
        workspace.setId("test-workspace");
        workspace.setName("test-workspace");
        getCatalog().add(workspace);
        // initiate the handlers related with services
        serviceHandler = GeoServerExtensions.bean(JMSServiceHandlerSPI.class).createHandler();
    }

    @After
    public void afterTest() {
        // clear all pending events
        JmsEventsListener.clear();
    }

    @Test
    public void testAddService() throws Exception {
        // create a WMS service for the test workspace
        WMSInfoImpl serviceInfo = new WMSInfoImpl();
        serviceInfo.setName("TEST-WMS-NAME");
        serviceInfo.setId("TEST-WMS-ID");
        serviceInfo.setWorkspace(workspace);
        serviceInfo.setAbstract("TEST-WMS-ABSTRACT");
        // add the new service to GeoServer
        getGeoServer().add(serviceInfo);
        // waiting for a service add event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000, (selected) -> selected.size() >= 1, SERVICE_EVENT_HANDLER_KEY);
        // let's check if the new added service was correctly published
        assertThat(messages.size(), is(1));
        List<JMSServiceModifyEvent> serviceEvents =
                getMessagesForHandler(messages, SERVICE_EVENT_HANDLER_KEY, serviceHandler);
        assertThat(serviceEvents.size(), is(1));
        assertThat(serviceEvents.get(0).getEventType(), is(JMSEventType.ADDED));
        // check the service content
        ServiceInfo publishedService = serviceEvents.get(0).getSource();
        assertThat(publishedService.getName(), is("TEST-WMS-NAME"));
        assertThat(publishedService.getId(), is("TEST-WMS-ID"));
        assertThat(publishedService.getAbstract(), is("TEST-WMS-ABSTRACT"));
    }

    @Test
    public void testModifyService() throws Exception {
        // modify the abstract of the WMS service
        WMSInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        assertThat(serviceInfo, notNullValue());
        String newAbstract = UUID.randomUUID().toString();
        serviceInfo.setAbstract(newAbstract);
        getGeoServer().save(serviceInfo);
        // waiting for the service modify events
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000, (selected) -> selected.size() >= 2, SERVICE_EVENT_HANDLER_KEY);
        // checking if we got the correct events, modify event and a post modify event
        assertThat(messages.size(), is(2));
        List<JMSServiceModifyEvent> serviceEvents =
                getMessagesForHandler(messages, SERVICE_EVENT_HANDLER_KEY, serviceHandler);
        assertThat(serviceEvents.size(), is(2));
        // check the modify event
        JMSServiceModifyEvent modifyEvent =
                serviceEvents
                        .stream()
                        .filter(event -> event.getEventType() == JMSEventType.MODIFIED)
                        .findFirst()
                        .orElse(null);
        assertThat(modifyEvent, notNullValue());
        ServiceInfo modifiedService = serviceEvents.get(0).getSource();
        assertThat(modifiedService.getName(), is(serviceInfo.getName()));
        assertThat(modifiedService.getId(), is(serviceInfo.getId()));
        assertThat(modifiedService.getAbstract(), is(newAbstract));
        // check the post modify event
        JMSServiceModifyEvent postModifyEvent =
                serviceEvents
                        .stream()
                        .filter(event -> event.getEventType() == JMSEventType.ADDED)
                        .findFirst()
                        .orElse(null);
        assertThat(postModifyEvent, notNullValue());
        ServiceInfo postModifiedService = serviceEvents.get(0).getSource();
        assertThat(postModifiedService.getName(), is(serviceInfo.getName()));
        assertThat(postModifiedService.getId(), is(serviceInfo.getId()));
        assertThat(postModifiedService.getAbstract(), is(newAbstract));
    }
}
