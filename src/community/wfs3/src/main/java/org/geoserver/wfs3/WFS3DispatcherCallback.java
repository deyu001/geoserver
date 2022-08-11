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

package org.geoserver.wfs3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs3.response.OpenAPIResponse;
import org.geoserver.wfs3.response.RFCGeoJSONFeaturesResponse;
import org.springframework.http.HttpHeaders;

public class WFS3DispatcherCallback extends AbstractDispatcherCallback {

    private Lazy<Service> wfs3 = new Lazy<>();
    private Lazy<Service> fallback = new Lazy<>();

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Service wfs3 =
                this.wfs3.getOrCompute(() -> (Service) GeoServerExtensions.bean("wfsService-3.0"));
        Service fallback =
                this.fallback.getOrCompute(
                        () -> (Service) GeoServerExtensions.bean("wfsService-2.0"));
        if (wfs3.equals(service) && "GetCapabilities".equals(request.getRequest())) {
            request.setServiceDescriptor(fallback);
            return fallback;
        }
        return service;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        Service wfs3 =
                this.wfs3.getOrCompute(() -> (Service) GeoServerExtensions.bean("wfsService-3.0"));
        if (wfs3.equals(request.getServiceDescriptor())) {
            String header = request.getHttpRequest().getHeader(HttpHeaders.ACCEPT);
            Object parsedRequest = operation.getParameters()[0];
            Method formatSetter =
                    OwsUtils.setter(parsedRequest.getClass(), "outputFormat", String.class);
            Method formatGetter =
                    OwsUtils.getter(parsedRequest.getClass(), "outputFormat", String.class);
            try {
                // can we manipulate the format, and it's not already set?
                if (formatGetter != null
                        && formatSetter != null
                        && formatGetter.invoke(parsedRequest) == null) {

                    if (header != null && !"*/*".equalsIgnoreCase(header)) {
                        // figure out which format we want to use, take the fist supported one
                        LinkedHashSet<String> acceptedFormats =
                                new LinkedHashSet<>(Arrays.asList(header.split("\\s*,\\s*")));
                        List<String> availableFormats =
                                DefaultWebFeatureService30.getAvailableFormats(result.getClass());
                        acceptedFormats.retainAll(availableFormats);
                        if (!acceptedFormats.isEmpty()) {
                            String format = acceptedFormats.iterator().next();
                            setOutputFormat(request, parsedRequest, formatSetter, format);
                        }
                    } else {
                        // handle defaults if really nothing is specified
                        String defaultType = BaseRequest.JSON_MIME;
                        if ("getFeature".equals(request.getRequest())) {
                            defaultType = RFCGeoJSONFeaturesResponse.MIME;
                        } else if ("api".equals(request.getRequest())) {
                            defaultType = OpenAPIResponse.OPEN_API_MIME;
                        }
                        // for getStyle we're going to use the "native" format if possible
                        if (!"getStyle".equals(request.getRequest())) {
                            setOutputFormat(request, parsedRequest, formatSetter, defaultType);
                        }
                    }
                }
            } catch (Exception e) {
                throw new ServiceException("Failed to handle Accept header", e);
            }
        }

        return super.operationExecuted(request, operation, result);
    }

    private void setOutputFormat(
            Request request, Object parsedRequest, Method formatSetter, String outputformat)
            throws IllegalAccessException, InvocationTargetException {
        request.setOutputFormat(outputformat);
        formatSetter.invoke(parsedRequest, outputformat);
    }

    /**
     * Thread safe lazy calculation, used to avoid bean circular dependencies
     *
     * @param <T>
     */
    public final class Lazy<T> {
        private volatile T value;

        public T getOrCompute(Supplier<T> supplier) {
            final T result = value; // Just one volatile read
            return result == null ? maybeCompute(supplier) : result;
        }

        private synchronized T maybeCompute(Supplier<T> supplier) {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }
}
