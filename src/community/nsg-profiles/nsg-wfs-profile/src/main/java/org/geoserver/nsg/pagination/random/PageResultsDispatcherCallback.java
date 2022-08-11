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


package org.geoserver.nsg.pagination.random;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * This dispatcher manages service of type {@link PageResultsWebFeatureService} and sets the
 * parameter ResultSetID present on KVP map.
 *
 * <p>Dummy featureId value is added to KVP map to allow dispatcher to manage it as usual WFS 2.0
 * request.
 *
 * @author sandr
 */
public class PageResultsDispatcherCallback extends AbstractDispatcherCallback
        implements ApplicationListener<ContextRefreshedEvent> {

    static final String PAGE_RESULTS = "PageResults";

    private static final Logger LOGGER = Logging.getLogger(PageResultsDispatcherCallback.class);
    private final PageResultsWebFeatureService service;
    private GeoServer gs;

    public PageResultsDispatcherCallback(GeoServer gs, PageResultsWebFeatureService service) {
        this.gs = gs;
        this.service = service;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Object req = request.getKvp().get("REQUEST");
        if ("wfs".equals(service.getId().toLowerCase()) && PAGE_RESULTS.equals(req)) {
            // allow the request to be successfully parsed as a GetFeature (needs at least a
            // typename or a featureId)
            request.getKvp().put("featureId", Collections.singletonList("dummy"));
            // replace the service
            return new Service(
                    service.getId(), this.service, service.getVersion(), service.getOperations());
        }
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Operation newOperation = operation;
        // Change operation from PageResults to GetFeature to allow management of request as
        // standard get feature
        if (operation.getId().equals("PageResults")) {
            newOperation =
                    new Operation(
                            "GetFeature",
                            operation.getService(),
                            operation.getMethod(),
                            operation.getParameters());
        }
        return super.operationDispatched(request, newOperation);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // configure the extra operation in WFS 2.0
        List<Service> services = GeoServerExtensions.extensions(Service.class);
        for (Service s : services) {
            if ("wfs".equals(s.getId().toLowerCase())
                    && Integer.valueOf(2).equals(s.getVersion().getMajor())) {
                if (!s.getOperations().contains(PAGE_RESULTS)) {
                    s.getOperations().add(PAGE_RESULTS);
                }
            }
        }
    }
}
