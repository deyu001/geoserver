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

package org.geoserver.gwc.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geowebcache.service.Service;

/**
 * Intercepts calls to {@link Service org.geowebcache.service.Service} and checks whether the
 * service is enabled, throwing a 404 http error code exception if it's not, and proceeding normaly
 * if the service is enabled.
 *
 * @author Gabriel Roldan
 */
public class GWCServiceEnablementInterceptor implements MethodInterceptor {

    private GWC gwcFacade;
    private final GeoServer geoServer;

    /**
     * @param gwc provides access to the {@link GWCConfig configuration} to check whether a service
     *     is {@link GWC#isServiceEnabled(Service) enabled}.
     */
    public GWCServiceEnablementInterceptor(final GWC gwc, GeoServer geoServer) {
        this.gwcFacade = gwc;
        this.geoServer = geoServer;
    }

    /**
     * Intercepts the {@code getConveyor} and {@code handleRequest} calls to a {@link Service}
     * instance and checks whether the service is enabled.
     *
     * @see
     *     org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        final String methodName = invocation.getMethod().getName();
        if ("getConveyor".equals(methodName) || "handleRequest".equals(methodName)) {
            final org.geowebcache.service.Service service = (Service) invocation.getThis();
            boolean serviceEnabled;
            if (service.getPathName().equals("wmts")) {
                serviceEnabled = geoServer.getService(WMTSInfo.class).isEnabled();
            } else {
                serviceEnabled = gwcFacade.isServiceEnabled(service);
            }
            if (!serviceEnabled) {
                throw new org.geowebcache.service.HttpErrorCodeException(
                        400, "Service is disabled");
            }
        }
        return invocation.proceed();
    }
}
