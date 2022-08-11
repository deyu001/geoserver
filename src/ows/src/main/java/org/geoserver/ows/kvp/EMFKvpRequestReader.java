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

package org.geoserver.ows.kvp;

import java.lang.reflect.Method;
import java.util.Map;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.xsd.EMFUtils;

/**
 * Web Feature Service Key Value Pair Request reader.
 *
 * <p>This request reader makes use of the Eclipse Modelling Framework reflection api.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime, TOPP
 */
public class EMFKvpRequestReader extends KvpRequestReader {
    /** Factory used to create model objects / requests. */
    protected EFactory factory;

    /**
     * Creates the Wfs Kvp Request reader.
     *
     * @param requestBean The request class, which must be an emf class.
     */
    public EMFKvpRequestReader(Class requestBean, EFactory factory) {
        super(requestBean);

        // make sure an eobject is passed in
        if (!EObject.class.isAssignableFrom(requestBean)) {
            String msg = "Request bean must be an EObject";
            throw new IllegalArgumentException(msg);
        }

        this.factory = factory;
    }

    /** Reflectivley creates the request bean instance. */
    public Object createRequest() {
        String className = getRequestBean().getName();

        // strip off package
        int index = className.lastIndexOf('.');

        if (index != -1) {
            className = className.substring(index + 1);
        }

        Method create = OwsUtils.method(factory.getClass(), "create" + className);

        try {
            return create.invoke(factory, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object read(Object request, Map<String, Object> kvp, Map<String, Object> rawKvp)
            throws Exception {
        // use emf reflection
        EObject eObject = (EObject) request;

        for (Map.Entry<String, Object> entry : kvp.entrySet()) {
            String property = entry.getKey();
            Object value = entry.getValue();

            // respect the filter
            if (filter(property)) {
                continue;
            }

            if (EMFUtils.has(eObject, property)) {
                try {
                    setValue(eObject, property, value);
                } catch (Exception ex) {
                    throw new ServiceException(
                            "Failed to set property "
                                    + property
                                    + " in request object using value "
                                    + value
                                    + (value != null ? " of type " + value.getClass() : ""),
                            ex,
                            ServiceException.INVALID_PARAMETER_VALUE,
                            property);
                }
            }
        }

        return request;
    }

    /**
     * Sets a value in the target EMF object, adding it to a collection if the target is a
     * collection, setting it otherwise. Subclasses can override this behavior
     */
    protected void setValue(EObject eObject, String property, Object value) {
        // check for a collection
        if (EMFUtils.isCollection(eObject, property)) {
            EMFUtils.add(eObject, property, value);
        } else {
            EMFUtils.set(eObject, property, value);
        }
    }
}
