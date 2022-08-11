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

package org.geoserver.monitor.ows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestData.Category;
import org.geoserver.monitor.RequestData.Status;
import org.geoserver.monitor.ows.wcs10.DescribeCoverageHandler;
import org.geoserver.monitor.ows.wcs10.GetCoverageHandler;
import org.geoserver.monitor.ows.wfs.DescribeFeatureTypeHandler;
import org.geoserver.monitor.ows.wfs.GetFeatureHandler;
import org.geoserver.monitor.ows.wfs.LockFeatureHandler;
import org.geoserver.monitor.ows.wfs.TransactionHandler;
import org.geoserver.monitor.ows.wms.GetFeatureInfoHandler;
import org.geoserver.monitor.ows.wms.GetLegendGraphicHandler;
import org.geoserver.monitor.ows.wms.GetMapHandler;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

public class MonitorCallback implements DispatcherCallback {

    List<RequestObjectHandler> handlers = new ArrayList<>();

    Monitor monitor;

    public MonitorCallback(Monitor monitor, Catalog catalog) {
        this.monitor = monitor;

        // wfs
        handlers.add(new DescribeFeatureTypeHandler(monitor.getConfig(), catalog));
        handlers.add(new GetFeatureHandler(monitor.getConfig(), catalog));
        handlers.add(new LockFeatureHandler(monitor.getConfig(), catalog));
        handlers.add(new TransactionHandler(monitor.getConfig(), catalog));

        // wms
        handlers.add(new GetFeatureInfoHandler(monitor.getConfig()));
        handlers.add(new GetMapHandler(monitor.getConfig()));
        handlers.add(new GetLegendGraphicHandler(monitor.getConfig()));

        // wcs
        handlers.add(new DescribeCoverageHandler(monitor.getConfig()));
        handlers.add(new GetCoverageHandler(monitor.getConfig()));

        handlers.add(
                new org.geoserver.monitor.ows.wcs11.DescribeCoverageHandler(monitor.getConfig()));
        handlers.add(new org.geoserver.monitor.ows.wcs11.GetCoverageHandler(monitor.getConfig()));
    }

    public Request init(Request request) {
        return null;
    }

    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return null;
    }

    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return null;
    }

    public Operation operationDispatched(Request request, Operation operation) {
        RequestData data = monitor.current();
        if (data == null) {
            // will happen in cases where the filter is not active
            return operation;
        }

        data.setCategory(Category.OWS);
        data.setService(operation.getService().getId().toUpperCase());
        data.setOperation(normalizedOpId(operation));
        data.setOwsVersion(operation.getService().getVersion().toString());

        if (operation.getParameters().length > 0) {
            // TODO: a better check for the request object
            Object reqObj = operation.getParameters()[0];
            for (RequestObjectHandler h : handlers) {
                if (h.canHandle(reqObj)) {
                    h.handle(reqObj, data);
                    break;
                }
            }
        }

        monitor.update();

        return operation;
    }

    public Object operationExecuted(Request request, Operation operation, Object result) {
        return null;
    }

    public void finished(Request request) {
        if (request.getError() != null) {
            RequestData data = monitor.current();
            if (data == null) {
                // will happen in cases where the filter is not active
                return;
            }

            data.setStatus(Status.FAILED);
            data.setErrorMessage(request.getError().getLocalizedMessage());
            data.setError(request.getError());

            monitor.update();
        }
    }

    volatile Map<String, Map<String, String>> OPS;

    String normalizedOpId(Operation op) {
        if (OPS == null) {
            synchronized (this) {
                if (OPS == null) {
                    HashMap<String, Map<String, String>> tmp = new HashMap<>();
                    for (Service s : GeoServerExtensions.extensions(Service.class)) {
                        HashMap<String, String> map = new HashMap<>();
                        tmp.put(s.getId().toUpperCase(), map);

                        for (String o : s.getOperations()) {
                            map.put(o.toUpperCase(), o);
                        }
                    }
                    OPS = tmp;
                }
            }
        }

        Map<String, String> map = OPS.get(op.getService().getId().toUpperCase());
        if (map != null) {
            String normalized = map.get(op.getId().toUpperCase());
            if (normalized != null) {
                return normalized;
            }
        }

        return op.getId();
    }
}
