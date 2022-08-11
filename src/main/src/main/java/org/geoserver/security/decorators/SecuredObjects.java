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

package org.geoserver.security.decorators;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.WrapperPolicy;

/**
 * Utility class that provides easy and fast access to the registered {@link SecuredObjectFactory}
 * implementations
 *
 * @author Andrea Aime - TOPP
 */
public class SecuredObjects {
    /** Caches factories that can handle a certain class for quick access */
    static final Map<Class<?>, SecuredObjectFactory> FACTORY_CACHE = new ConcurrentHashMap<>();

    /**
     * Given an object to secure and a wrapping policy, scans the extension points for a factory
     * that can do the proper wrapping and invokes it, or simply gives up and throws an {@link
     * IllegalArgumentException} if no factory can deal with securing the specified object.
     *
     * @param object the raw object to be secured
     * @param policy the wrapping policy (how the secured wrapper should behave)
     * @return the secured object, or null if the input is null.
     * @throws IllegalArgumentException if the factory is not able to wrap the object
     */
    public static <T> T secure(T object, WrapperPolicy policy) {
        // null safety
        if (object == null) return null;

        // In case the object to be secured is wrapped in a ModificationProxy, get the proxied
        // Object class
        // if the object is not wrapped, ModificationProxy.unwrap() will just return the object
        // reference.
        final Object unwrappedObject = ModificationProxy.unwrap(object);

        // if we already know what can handle the wrapping, just do it, don't
        // scan the extension points once more
        Class<?> clazz = unwrappedObject.getClass();
        SecuredObjectFactory candidate = FACTORY_CACHE.get(clazz);

        // otherwise scan and store (or complain)
        if (candidate == null) {
            // scan the application context
            List<SecuredObjectFactory> factories =
                    GeoServerExtensions.extensions(SecuredObjectFactory.class);
            for (SecuredObjectFactory factory : factories) {
                if (factory.canSecure(clazz)) {
                    candidate = factory;
                    break;
                }
            }
            if (candidate == null)
                throw new IllegalArgumentException(
                        "Could not find a security wrapper for class "
                                + clazz
                                + ", cannot secure the object");
            FACTORY_CACHE.put(clazz, candidate);
        }

        @SuppressWarnings("unchecked")
        T result = (T) candidate.secure(object, policy);
        return result;
    }
}
