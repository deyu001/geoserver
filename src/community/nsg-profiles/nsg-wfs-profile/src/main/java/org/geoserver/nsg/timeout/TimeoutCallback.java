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

package org.geoserver.nsg.timeout;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.opengis.wfs20.BaseRequestType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.Converters;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

/** Callback implementing NSG timeout extension */
public class TimeoutCallback extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(TimeoutCallback.class);

    /** The key storing the NSG request timeout in the {@link WFSInfo#getMetadata()} map */
    public static final String TIMEOUT_CONFIG_KEY = "org.geoserver.nsg.timeout";

    /** The default timeout according to specification (5 minutes) */
    public static final int TIMEOUT_CONFIG_DEFAULT = 300;

    static final String TIMEOUT_REQUEST_ATTRIBUTE = "timeout";

    static final Version V_20 = new Version("2.0");

    GeoServer gs;

    ThreadLocal<TimeoutVerifier> TIMEOUT_VERIFIER = new ThreadLocal<>();

    public TimeoutCallback(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public Request init(Request request) {
        return super.init(request);
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        String version = request.getVersion();
        String method = request.getRequest();
        long timeout = getTimeoutMilliseconds(operation);
        if ("WFS".equalsIgnoreCase(request.getService())
                && (version == null || V_20.compareTo(new Version(version)) <= 0)
                && method != null
                && (method.equalsIgnoreCase("GetFeature")
                        || method.equalsIgnoreCase("GetFeatureWithLock")
                        || method.equalsIgnoreCase("GetPropertyValue"))
                && timeout > 0
                && operation.getParameters().length > 0
                && operation.getParameters()[0] instanceof BaseRequestType) {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Starting to track NSG timeout on this request");
            }

            // start tracking time
            TimeoutVerifier timeoutVerifier =
                    new TimeoutVerifier((BaseRequestType) operation.getParameters()[0], timeout);
            // need to wrap the http response and its output stream
            request.setHttpResponse(
                    new TimeoutCancellingResponse(request.getHttpResponse(), timeoutVerifier));
            // set in the thread local for later use
            TIMEOUT_VERIFIER.set(timeoutVerifier);
        }

        return operation;
    }

    @Override
    public void finished(Request request) {
        TIMEOUT_VERIFIER.remove();
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        TimeoutVerifier timeoutVerifier = TIMEOUT_VERIFIER.get();
        if (timeoutVerifier != null) {
            // check before encode
            timeoutVerifier.checkTimeout();

            // wrap if needed
            if (result instanceof FeatureCollectionResponse) {
                FeatureCollectionResponse featureCollectionResponse =
                        (FeatureCollectionResponse) result;
                List<FeatureCollection> collections = featureCollectionResponse.getFeatures();
                List<FeatureCollection> wrappers =
                        collections
                                .stream()
                                .map(fc -> TimeoutFeatureCollection.wrap(timeoutVerifier, fc))
                                .collect(Collectors.toList());

                featureCollectionResponse.setFeatures(wrappers);
            }
        }

        return result;
    }

    private long getTimeoutMilliseconds(Operation operation) {
        // check if there is a timeout parameter
        Object[] parameters = operation.getParameters();
        if (parameters != null
                && parameters.length > 0
                && parameters[0] instanceof BaseRequestType) {
            BaseRequestType request = (BaseRequestType) parameters[0];
            Object timeout = request.getExtendedProperties().get(TIMEOUT_REQUEST_ATTRIBUTE);
            if (timeout != null) {
                Long converted = Converters.convert(timeout, Long.class);
                if (converted != null && converted > 0) {
                    return converted * 1000l;
                } else {
                    throw new WFSException(request, "Invalid timeout value: " + timeout);
                }
            }
        }

        // use the configured default
        WFSInfo wfs = gs.getService(WFSInfo.class);
        Integer timeoutSeconds = wfs.getMetadata().get(TIMEOUT_CONFIG_KEY, Integer.class);
        return Optional.ofNullable(timeoutSeconds).orElse(TIMEOUT_CONFIG_DEFAULT) * 1000L;
    }
}
