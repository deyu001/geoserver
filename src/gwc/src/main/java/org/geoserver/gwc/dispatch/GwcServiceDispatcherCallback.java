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

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;

/**
 * Adapts plain incoming requests to be resolved to the GWC proxy service.
 *
 * <p>The GeoServer {@link Dispatcher} will call {@link #init(Request)} as the first step before
 * processing the request. This callback will set the {@link Request}'s service, version, and
 * request properties to the "fake" gwc service (service=gwc, version=1.0.0, request=dispatch), so
 * that when the {@link Dispatcher} looks up for the actual service bean to process the request it
 * finds out the {@link GwcServiceProxy} instance that's configured to handle such a service
 * request.
 *
 * <p>See the package documentation for more insights on how these all fit together.
 */
public class GwcServiceDispatcherCallback extends AbstractDispatcherCallback
        implements DispatcherCallback {

    // contains the current gwc operation
    public static final ThreadLocal<String> GWC_OPERATION = new ThreadLocal<>();
    public static final ThreadLocal<String> GWC_ORIGINAL_BASEURL = new ThreadLocal<>();

    private static final Pattern GWC_WS_VIRTUAL_SERVICE_PATTERN =
            Pattern.compile("([^/]+)/gwc/service.*");
    private static final Pattern GWC_LAYER_VIRTUAL_SERVICE_PATTERN =
            Pattern.compile("([^/]+)/([^/]+)/gwc/service.*");

    static final String buildRestPattern(int numPathElements) {
        return ".*/service/wmts/rest" + Strings.repeat("/([^/]+)", numPathElements);
    }

    private static final Pattern TILE_1 = Pattern.compile(buildRestPattern(5));
    private static final Pattern TILE_2 = Pattern.compile(buildRestPattern(6));
    private static final Pattern FEATUREINFO_1 = Pattern.compile(buildRestPattern(7));
    private static final Pattern FEATUREINFO_2 = Pattern.compile(buildRestPattern(8));

    private final Catalog catalog;

    public GwcServiceDispatcherCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void finished(Request request) {
        // cleaning the current thread local operation
        GWC_OPERATION.remove();
        GWC_ORIGINAL_BASEURL.remove();
    }

    @Override
    public Request init(Request request) {
        String context = request.getContext();
        if (context == null || !isGwcServiceTargeted(context)) {
            return null;
        }

        // storing the current operation (bridge the RESTful bindings that do not have a
        // REQUEST parameter by matching their paths)
        String requestName = (String) request.getKvp().get("REQUEST");
        final String pathInfo = request.getHttpRequest().getPathInfo();
        if (requestName == null
                && pathInfo != null
                && pathInfo.contains("gwc/service/wmts/rest/")) {
            if (pathInfo.endsWith("WMTSCapabilities.xml")) {
                requestName = "GetCapabilities";
            } else if (TILE_1.matcher(pathInfo).matches() || TILE_2.matcher(pathInfo).matches()) {
                requestName = "GetTile";
            } else if (FEATUREINFO_1.matcher(pathInfo).matches()
                    || FEATUREINFO_2.matcher(pathInfo).matches()) {
                requestName = "GetFeatureInfo";
            }
        }
        GWC_OPERATION.set(requestName);
        GWC_ORIGINAL_BASEURL.set(ResponseUtils.baseURL(request.getHttpRequest()));

        Map<String, Object> kvp = new HashMap<>();
        kvp.put("service", "gwc");
        kvp.put("version", "1.0.0");
        kvp.put("request", "dispatch");

        // if we are in the presence of virtual service we need to adapt the request
        WorkspaceInfo localWorkspace = LocalWorkspace.get();
        PublishedInfo localPublished = LocalPublished.get();
        if (localWorkspace != null) {
            // this is a virtual service request
            String layerName = (String) request.getKvp().get("LAYER");
            if (layerName == null) {
                layerName = (String) request.getKvp().get("layer");
            }
            if (layerName != null) {
                // we have a layer name as parameter we need to adapt it (gwc doesn't care about
                // workspaces)
                layerName = CatalogConfiguration.removeWorkspacePrefix(layerName, catalog);
                layerName = localWorkspace.getName() + ":" + layerName;
                // we set the layer parameter with GWC expected name
                kvp.put("LAYER", layerName);
            }

            String localPublishedName = localPublished != null ? localPublished.getName() : null;
            // we need to setup a proper context path (gwc doesn't expect the workspace to be part
            // of the URL)
            request.setHttpRequest(
                    new VirtualServiceRequest(
                            request.getHttpRequest(),
                            localWorkspace.getName(),
                            localPublishedName,
                            layerName));
        } else if (localPublished != null) {
            request.setHttpRequest(
                    new VirtualServiceRequest(
                            request.getHttpRequest(), localPublished.getName(), null, null));
        }

        request.setKvp(kvp);
        request.setRawKvp(kvp);

        return request;
    }

    /** Helper method that checks if the GWC service is targeted based on the request context. */
    private boolean isGwcServiceTargeted(String context) {
        if (context.startsWith("gwc/service")) {
            // is gwc is targeted
            return true;
        }
        // we may be in the context of a workspace or group specific
        Matcher matcher = GWC_WS_VIRTUAL_SERVICE_PATTERN.matcher(context);
        if (matcher.matches()) {
            // this is a virtual service, let's see if we have a valid workspace
            if (LocalWorkspace.get() == null && !(LocalPublished.get() instanceof LayerGroupInfo)) {
                // the workspace name has to be valid
                throw new ServiceException("No such workspace '" + matcher.group(1) + "'");
            }
            // the local workspace is set so we have a valid workspace
            return true;
        }
        matcher = GWC_LAYER_VIRTUAL_SERVICE_PATTERN.matcher(context);
        if (matcher.matches()) {
            // this is a laye specific virtual service, let's see if we have a valid workspace
            if (LocalPublished.get() == null) {
                // the workspace name has to be valid
                throw new ServiceException(
                        "No such layer or layer group '" + matcher.group(2) + "'");
            }
            // the local workspace is set so we have a valid workspace
            return true;
        }

        // this request is not targeting gwc service
        return false;
    }

    /**
     * Helper wrapper that allow to match GWC expectations. GWC doesn't have the concept of
     * workspaces, so he always expect a layer name to be prefixed by is workspace.
     */
    private final class VirtualServiceRequest extends HttpServletRequestWrapper {

        private final String localWorkspaceName;
        private String localPublishedName;
        private final String layerName;

        private final Map<String, String[]> parameters;

        public VirtualServiceRequest(
                HttpServletRequest request,
                String localWorkspaceName,
                String localPublishedName,
                String layerName) {
            super(request);
            this.localWorkspaceName = localWorkspaceName;
            this.localPublishedName = localPublishedName;
            this.layerName = layerName;
            parameters = new HashMap<>(request.getParameterMap());
            if (layerName != null) {
                parameters.put("layer", new String[] {layerName});
            }
        }

        @Override
        public String getContextPath() {
            // to GWC the workspace is part of the request context
            if (localPublishedName == null) {
                return super.getContextPath() + "/" + localWorkspaceName;
            } else {
                return super.getContextPath() + "/" + localWorkspaceName + "/" + localPublishedName;
            }
        }

        @Override
        public String getParameter(String name) {
            if (layerName != null && name.equalsIgnoreCase("layer")) {
                return layerName;
            }
            return super.getParameter(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return parameters;
        }

        @Override
        public String[] getParameterValues(String name) {
            if (layerName != null && name.equalsIgnoreCase("layer")) {
                return new String[] {layerName};
            }
            return super.getParameterValues(name);
        }
    }
}
