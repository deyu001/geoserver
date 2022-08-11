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

import java.lang.reflect.Method;
import org.geoserver.platform.Service;

/**
 * Hook to indicate the {@link Dispatcher} that a given service object doesn't want its operations
 * to be called through reflection to avoid the performance penalty given by the synchronization
 * inside {@link Method#invoke(Object, Object...)}.
 *
 * <p>Service beans (as targeted by {@link Service#getService()}) doesn't need to implement this
 * inteface. It actually only makes sense if the service operations execute really fast and are
 * supposed to be hit by a large number of concurrent requests, as in the case of GeoWebCache tile
 * requests, where the synchronized blocks in the reflective method invocations may impose a
 * noticeable performance degradation.
 *
 * <p>At the time of writing (March 2012) this behaviour has been noticed in both Sun Java6 and
 * Java7 JDK's, other virtual machine implementations may or may not incurr in such performance
 * penalty.
 */
public interface DirectInvocationService {

    /**
     * Provides a more direct way of invoking a service operation than using reflection.
     *
     * @param operationName the name of the operation to execute, as declared in the {@link
     *     Service#getOperations() service operations} for the service descriptor that targets this
     *     service object.
     * @param parameters the list of parameters for the actual operation, as if the actual method
     *     were invoked through {@link Method#invoke(Object, Object...)}
     * @return the operation result
     * @throws IllegalArgumentException if either the operation name or arguments list doesn't match
     *     one of the service provided operations under any other circumstances, specific to the
     *     operation being executed
     */
    Object invokeDirect(String operationName, Object[] parameters)
            throws IllegalArgumentException, Exception;
}
