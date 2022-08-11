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

package org.geoserver.platform;

import java.lang.reflect.Method;

/**
 * An operation descriptor providing metadata about a service operation.
 *
 * <p>An operation is identified by an id,service pair. Two operation descriptors are considred
 * equal if they have the same id, service pair.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public final class Operation {
    /** Unique identifier withing service of the operation. */
    final String id;

    /** Service this operation is a component of. */
    final Service service;

    /** The method implementing the operation */
    final Method method;

    /** Parameters of the operation */
    final Object[] parameters;

    /**
     * Creates a new operation descriptor.
     *
     * @param id Id of the operation, must not be <code>null</code>
     * @param service The service containing the operation, must not be <code>null</code>
     * @param method THe method implementing the operation.
     * @param parameters The parameters of the operation, may be <code>null</code>
     */
    public Operation(String id, Service service, Method method, Object[] parameters) {
        this.id = id;
        this.service = service;
        this.method = method;
        this.parameters = parameters;

        if (id == null) {
            throw new NullPointerException("id");
        }

        if (service == null) {
            throw new NullPointerException("service");
        }
    }

    /** @return The id of the operation. */
    public String getId() {
        return id;
    }

    /** @return The service implementing the operation. */
    public Service getService() {
        return service;
    }

    /** @return The method implementing the operation. */
    public Method getMethod() {
        return method;
    }

    /** @return The parameters supplied to the operation */
    public Object[] getParameters() {
        return parameters;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Operation)) {
            return false;
        }

        Operation other = (Operation) obj;

        if (!id.equals(other.id)) {
            return false;
        }

        if (!service.equals(other.service)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (id.hashCode() * 17) + service.hashCode();
    }

    public String toString() {
        return "Operation( " + id + ", " + service.getId() + " )";
    }
}
