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

package org.geoserver.gwc.wms;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.geowebcache.conveyor.Conveyor.CacheResult.MISS;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RawMap;
import org.geotools.util.logging.Logging;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;

/**
 * {@link WebMapService#getMap(GetMapRequest)} Spring's AOP method interceptor to serve cached tiles
 * whenever the request matches a GeoWebCache tile.
 *
 * @author Gabriel Roldan
 */
public class CachingWebMapService implements MethodInterceptor {

    private static final Logger LOGGER = Logging.getLogger(CachingWebMapService.class);

    private GWC gwc;

    public CachingWebMapService(GWC gwc) {
        this.gwc = gwc;
    }

    /**
     * Wraps {@link WebMapService#getMap(GetMapRequest)}, called by the {@link Dispatcher}
     *
     * @see WebMapService#getMap(GetMapRequest)
     * @see
     *     org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public WebMap invoke(MethodInvocation invocation) throws Throwable {
        GWCConfig config = gwc.getConfig();
        if (!config.isDirectWMSIntegrationEnabled()) {
            return (WebMap) invocation.proceed();
        }

        final GetMapRequest request = getRequest(invocation);
        boolean tiled = request.isTiled() || !config.isRequireTiledParameter();
        if (!tiled) {
            return (WebMap) invocation.proceed();
        }

        final StringBuilder requestMistmatchTarget = new StringBuilder();
        ConveyorTile cachedTile = gwc.dispatch(request, requestMistmatchTarget);

        if (cachedTile == null) {
            WebMap dynamicResult = (WebMap) invocation.proceed();
            dynamicResult.setResponseHeader("geowebcache-cache-result", MISS.toString());
            dynamicResult.setResponseHeader(
                    "geowebcache-miss-reason", requestMistmatchTarget.toString());
            return dynamicResult;
        }
        checkState(cachedTile.getTileLayer() != null);
        final TileLayer layer = cachedTile.getTileLayer();

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("GetMap request intercepted, serving cached content: " + request);
        }

        final byte[] tileBytes;
        {
            final Resource mapContents = cachedTile.getBlob();
            if (mapContents instanceof ByteArrayResource) {
                tileBytes = ((ByteArrayResource) mapContents).getContents();
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                mapContents.transferTo(Channels.newChannel(out));
                tileBytes = out.toByteArray();
            }
        }

        // Handle Etags
        final String ifNoneMatch = request.getHttpRequestHeader("If-None-Match");
        final String etag = GWC.getETag(tileBytes);
        if (etag.equals(ifNoneMatch)) {
            // Client already has the current version
            LOGGER.finer("ETag matches, returning 304");
            throw new HttpErrorCodeException(HttpServletResponse.SC_NOT_MODIFIED);
        }

        LOGGER.finer("No matching ETag, returning cached tile");
        final String mimeType = cachedTile.getMimeType().getMimeType();

        RawMap map = new RawMap(null, tileBytes, mimeType);

        map.setContentDispositionHeader(
                null, "." + cachedTile.getMimeType().getFileExtension(), false);

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        GWC.setCacheControlHeaders(headers, layer);
        GWC.setConditionalGetHeaders(
                headers, cachedTile, etag, request.getHttpRequestHeader("If-Modified-Since"));
        GWC.setCacheMetadataHeaders(headers, cachedTile, layer);
        headers.forEach((k, v) -> map.setResponseHeader(k, v));

        return map;
    }

    private GetMapRequest getRequest(MethodInvocation invocation) {
        final Method method = invocation.getMethod();
        checkArgument(method.getDeclaringClass().equals(WebMapService.class));
        checkArgument("getMap".equals(method.getName()));

        final Object[] arguments = invocation.getArguments();

        checkArgument(arguments.length == 1);
        checkArgument(arguments[0] instanceof GetMapRequest);

        final GetMapRequest request = (GetMapRequest) arguments[0];
        return request;
    }
}
