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

package org.geoserver.ows;

import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * Provides call backs for the life cycle of an ows request.
 *
 * <p>Instances of this interface should be registered in a spring application context like:
 *
 * <pre>
 *  &lt;bean id="myCallback" class="org.acme.MyCallback"/&gt;
 * </pre>
 *
 * @author Justin Deoliveira, OpenGEO
 */
public interface DispatcherCallback {

    /**
     * Called immediately after a request has been received and initialized by the dispatcher.
     *
     * <p>This method can modify the request object, or wrap and return it. If null is returned the
     * request passed in is used normally.
     *
     * @param request The request being executed.
     */
    Request init(Request request);

    /**
     * Called after the service for the request has been determined.
     *
     * <p>This method can modify the service object, or wrap and return it. If null is returned the
     * service passed in is used normally.
     *
     * @param request The request.
     * @param service The service descriptor for the service handling the request.
     */
    Service serviceDispatched(Request request, Service service) throws ServiceException;

    /**
     * Called after the operation for the request has been determined.
     *
     * <p>This method can modify the operation object, or wrap and return it. If null is returned
     * the operation passed in is used normally.
     *
     * @param request The request.
     * @param operation The operation for the request.
     */
    Operation operationDispatched(Request request, Operation operation);

    /**
     * Called after the operation for a request has been executed.
     *
     * <p><b>Note:</b>This method should handle the case where <tt>result</tt> is null as this
     * corresponds to an operation which does not return a value.
     *
     * <p>This method can modify the result object, or wrap and return it. If null is returned the
     * result passed in is used normally.
     *
     * @param request The request.
     * @param operation The operation.
     * @param result The result of the operation, may be <code>null</code>.
     */
    Object operationExecuted(Request request, Operation operation, Object result);

    /**
     * Called after the response to a request has been dispatched.
     *
     * <p><b>Note:</b> This method is only called when the operation returns a value.
     *
     * <p>This method can modify the response object, or wrap and return it. If null is returned the
     * response passed in is used normally.
     *
     * @param request The request.
     * @param operation The operation.
     * @param result The result of the operation.
     * @param response The response to the operation.
     */
    Response responseDispatched(
            Request request, Operation operation, Object result, Response response);

    /**
     * Called after the response to the operation has been executed.
     *
     * <p>This method is called regardless if the operation was successful or not. In the event of a
     * request that resulted in an error, the error is available at {@link Request#error}.
     *
     * @param request The request.
     */
    void finished(Request request);
}
