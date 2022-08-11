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

package org.geoserver.gwc.dispatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.junit.After;
import org.junit.Test;

public final class GwcServiceDispatcherCallbackTest {

    @After
    public void cleanLocalWorkspace() {
        // clean any set local workspace
        LocalWorkspace.remove();
    }

    @Test
    public void testThatGwcServiceRequestsAreAccepted() {
        // creating some mocks needed for the test and instantiating the dispatcher call back
        HttpServletRequest httpRequest = newMockHttpRequest();
        when(httpRequest.getParameterMap()).thenReturn(Collections.emptyMap());
        Request request = mock(Request.class);
        when(request.getHttpRequest()).thenReturn(httpRequest);
        // the catalog will not be used so it can be null
        GwcServiceDispatcherCallback callback = new GwcServiceDispatcherCallback(null);
        // not a gwc request
        when(request.getContext()).thenReturn("wms/service");
        assertThat(callback.init(request), nullValue());
        // a simple gwc request
        when(request.getContext()).thenReturn("gwc/service");
        assertThat(callback.init(request), notNullValue());
        // a valid virtual service request (setting a local workspace will make the workspace valid)
        LocalWorkspace.set(mock(WorkspaceInfo.class));
        when(request.getContext()).thenReturn("validWorkspace/gwc/service");
        assertThat(callback.init(request), notNullValue());
        // an invalid virtual service request (a missing local workspace will make the workspace
        // invalid)
        LocalWorkspace.remove();
        when(request.getContext()).thenReturn("invalidWorkspace/gwc/service");
        try {
            callback.init(request);
            fail("The workspace is not valid, an exception should have been throw.");
        } catch (ServiceException serviceException) {
            assertThat(serviceException.getMessage(), is("No such workspace 'invalidWorkspace'"));
        }
    }

    public HttpServletRequest newMockHttpRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("http");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/geoserver/gwc");
        return request;
    }

    @Test
    public void testGwcVirtualServiceRequestWrapper() {
        // we create a mock for the http request
        HttpServletRequest httpRequest = newMockHttpRequest();
        when(httpRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(httpRequest.getContextPath()).thenReturn("geoserver");
        // we create a mock for the geoserver request
        Request request = new Request();
        request.setKvp(Collections.singletonMap("LAYER", "someLayer"));
        request.setHttpRequest(httpRequest);
        request.setContext("someWorkspace/gwc/service");
        // mock for the local workspace
        WorkspaceInfo workspace = mock(WorkspaceInfo.class);
        when(workspace.getName()).thenReturn("someWorkspace");
        // instantiating the dispatcher callback
        GwcServiceDispatcherCallback callback = new GwcServiceDispatcherCallback(null);
        // setting a local workspace
        LocalWorkspace.set(workspace);
        Request wrappedRequest = callback.init(request);
        assertThat(wrappedRequest, notNullValue());
        assertThat(wrappedRequest.getHttpRequest(), notNullValue());
        assertThat(wrappedRequest.getHttpRequest().getContextPath(), is("geoserver/someWorkspace"));
        assertThat(
                wrappedRequest.getHttpRequest().getParameter("layer"),
                is("someWorkspace:someLayer"));
        assertThat(wrappedRequest.getHttpRequest().getParameterMap(), notNullValue());
        assertThat(
                wrappedRequest.getHttpRequest().getParameterMap().get("layer"),
                is(new String[] {"someWorkspace:someLayer"}));
        assertThat(
                wrappedRequest.getHttpRequest().getParameterValues("layer"),
                is(new String[] {"someWorkspace:someLayer"}));
    }

    @Test
    public void testThatGwcOperationIsStored() {
        // creating some mocks needed for the test and instantiating the dispatcher call back
        HttpServletRequest httpRequest = newMockHttpRequest();
        when(httpRequest.getParameterMap()).thenReturn(Collections.emptyMap());
        Request request = new Request();
        request.setKvp(Collections.singletonMap("REQUEST", "GetCapabilities"));
        request.setHttpRequest(httpRequest);
        request.setContext("gwc/service");
        // the catalog will not be used so it can be null
        GwcServiceDispatcherCallback callback = new GwcServiceDispatcherCallback(null);
        // invoke the dispatcher callback
        Request wrappedRequest = callback.init(request);
        assertThat(wrappedRequest, notNullValue());
        assertThat(GwcServiceDispatcherCallback.GWC_OPERATION.get(), notNullValue());
        assertThat(GwcServiceDispatcherCallback.GWC_OPERATION.get(), is("GetCapabilities"));
    }
}
