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

package org.geoserver.cluster.impl.handlers.configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cluster.impl.events.configuration.JMSEventType;
import org.geoserver.cluster.impl.events.configuration.JMSServiceModifyEvent;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JmsServiceHandlerTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void setup() {
        // create a test workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("jms-test-workspace");
        workspace.setName("jms-test-workspace");
        getCatalog().add(workspace);
    }

    @After
    public void clean() {
        // remove test workspace
        getCatalog().remove(getCatalog().getWorkspace("jms-test-workspace"));
        // remove any created service
        Collection<? extends ServiceInfo> services = getGeoServer().getServices();
        for (ServiceInfo service : services) {
            ServiceInfo finalService = ModificationProxy.unwrap(service);
            if (finalService instanceof JmsTestServiceInfoImpl) {
                getGeoServer().remove(finalService);
            }
        }
    }

    @Test
    public void testGlobalServiceSimpleCrud() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new global service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-1", "jms-test-service", "global-jms-test-service", null));
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
        // update global service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service", "global-jms-test-service-updated", null));
        checkServiceExists("jms-test-service", "global-jms-test-service-updated", null);
        // delete global service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", null));
        assertThat(findService("jms-test-service", null), nullValue());
    }

    @Test
    public void testVirtualServiceSimpleCrud() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new virtual service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-2",
                        "jms-test-service",
                        "virtual-jms-test-service",
                        "jms-test-workspace"));
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // update virtual service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service",
                        "virtual-jms-test-service-updated",
                        "jms-test-workspace"));
        checkServiceExists(
                "jms-test-service", "virtual-jms-test-service-updated", "jms-test-workspace");
        // delete virtual service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", "jms-test-workspace"));
        assertThat(findService("jms-test-service", "jms-test-workspace"), nullValue());
    }

    @Test
    public void testGlobalAndVirtualServiceSimpleCrud() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new global and virtual service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-1", "jms-test-service", "global-jms-test-service", null));
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-2",
                        "jms-test-service",
                        "virtual-jms-test-service",
                        "jms-test-workspace"));
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // update global service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service", "global-jms-test-service-updated", null));
        checkServiceExists("jms-test-service", "global-jms-test-service-updated", null);
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // update virtual service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service",
                        "virtual-jms-test-service-updated",
                        "jms-test-workspace"));
        checkServiceExists(
                "jms-test-service", "virtual-jms-test-service-updated", "jms-test-workspace");
        checkServiceExists("jms-test-service", "global-jms-test-service-updated", null);
        // delete virtual service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", "jms-test-workspace"));
        assertThat(findService("jms-test-service", "jms-test-workspace"), nullValue());
        assertThat(findService("jms-test-service", null), notNullValue());
        // delete global service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", null));
        assertThat(findService("jms-test-service", null), nullValue());
    }

    @Test
    public void testUpdatingNonExistingVirtualService() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new global and virtual service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-3", "jms-test-service", "global-jms-test-service", null));
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-4",
                        "jms-test-service",
                        "virtual-jms-test-service",
                        "jms-test-workspace"));
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // create update virtual service event
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service",
                        "virtual-jms-test-service-updated",
                        "jms-test-workspace"));
        // remove virtual service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", "jms-test-workspace"));
        assertThat(findService("jms-test-service", "jms-test-workspace"), nullValue());
        // check the update result
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
    }

    private void checkServiceExists(
            String serviceName, String serviceAbstract, String workspaceName) {
        ServiceInfo serviceInfo = findService(serviceName, workspaceName);
        assertThat(serviceInfo, notNullValue());
        assertThat(serviceInfo.getAbstract(), is(serviceAbstract));
    }

    private JMSServiceModifyEvent createNewServiceEvent(
            String serviceId, String serviceName, String serviceAbstract, String workspaceName) {
        // our virtual service information
        JmsTestServiceInfoImpl serviceInfo = new JmsTestServiceInfoImpl();
        serviceInfo.setName(serviceName);
        serviceInfo.setId(serviceId);
        if (workspaceName != null) {
            // this is a virtual service
            serviceInfo.setWorkspace(getCatalog().getWorkspace(workspaceName));
        }
        serviceInfo.setAbstract(serviceAbstract);
        // create jms service modify event
        return new JMSServiceModifyEvent(serviceInfo, JMSEventType.ADDED);
    }

    private JMSServiceModifyEvent createModifyServiceEvent(
            String serviceName, String newServiceAbstract, String workspaceName) {
        // service information
        ServiceInfo serviceInfo = findService(serviceName, workspaceName);
        String oldServiceAbstract = serviceInfo.getAbstract();
        serviceInfo.setAbstract(newServiceAbstract);
        // create jms service modify event
        return new JMSServiceModifyEvent(
                serviceInfo,
                Collections.singletonList("abstract"),
                Collections.singletonList(oldServiceAbstract),
                Collections.singletonList(newServiceAbstract),
                JMSEventType.MODIFIED);
    }

    private JMSServiceModifyEvent createRemoveServiceEvent(
            String serviceName, String workspaceName) {
        // our virtual service information
        ServiceInfo serviceInfo = findService(serviceName, workspaceName);
        // create jms service modify event
        return new JMSServiceModifyEvent(serviceInfo, JMSEventType.REMOVED);
    }

    private ServiceInfo findService(String serviceName, String workspaceName) {
        if (workspaceName == null) {
            // global service
            return ModificationProxy.unwrap(
                    getGeoServer().getServiceByName(serviceName, ServiceInfo.class));
        }
        // virtual service
        WorkspaceInfo workspaceInfo = getCatalog().getWorkspace(workspaceName);
        return ModificationProxy.unwrap(
                getGeoServer().getServiceByName(workspaceInfo, serviceName, ServiceInfo.class));
    }

    private JMSServiceHandler createHandler() {
        JMSServiceHandlerSPI handlerSpi = GeoServerExtensions.bean(JMSServiceHandlerSPI.class);
        return (JMSServiceHandler) handlerSpi.createHandler();
    }
}
