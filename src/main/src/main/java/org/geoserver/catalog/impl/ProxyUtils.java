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

package org.geoserver.catalog.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for working with proxies.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ProxyUtils {

    /** Avoids the cost of looking up over and over the same proxy class */
    static final Map<ProxyClassConstructorKey, Constructor> PROXY_CLASS_CACHE =
            new ConcurrentHashMap<>();

    static final class ProxyClassConstructorKey {
        Class c1;
        Class c2;

        public ProxyClassConstructorKey(Class c1, Class c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((c1 == null) ? 0 : c1.hashCode());
            result = prime * result + ((c2 == null) ? 0 : c2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ProxyClassConstructorKey other = (ProxyClassConstructorKey) obj;
            if (c1 == null) {
                if (other.c1 != null) {
                    return false;
                }
            } else if (!c1.equals(other.c1)) {
                return false;
            }
            if (c2 == null) {
                if (other.c2 != null) {
                    return false;
                }
            } else if (!c2.equals(other.c2)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Creates a proxy for the specified object.
     *
     * @param proxyObject The object to proxy.
     * @param clazz The explicit interface to proxy.
     * @param h The invocation handler to intercept method calls.
     */
    public static <T> T createProxy(T proxyObject, Class<? extends T> clazz, InvocationHandler h) {
        try {
            // proxy all interfaces implemented by the source object
            List<Class> proxyInterfaces = Arrays.asList(proxyObject.getClass().getInterfaces());

            // ensure that the specified class is included
            boolean add = true;
            for (Class interfce : proxyObject.getClass().getInterfaces()) {
                if (clazz.isAssignableFrom(interfce)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                // make the list mutable (Arrays.asList is not) and then add the extra
                // interfaces
                proxyInterfaces = new ArrayList<>(proxyInterfaces);
                proxyInterfaces.add(clazz);
            }

            @SuppressWarnings("unchecked")
            T instance =
                    (T)
                            Proxy.newProxyInstance(
                                    clazz.getClassLoader(),
                                    proxyInterfaces.toArray(new Class[proxyInterfaces.size()]),
                                    h);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unwraps a proxy returning the underlying object, if one exists.
     *
     * <p>This method handles two cases, the first is the case of a {@link WrappingProxy} in which
     * the underlying proxy object is returned.. The second is the {@link ProxyList} case in which
     * the underlying list is returned.
     *
     * @param object The proxy object.
     * @param handlerClass The invocation handler class.
     * @return The underlying proxied object, or the object passed in if no underlying object is
     *     recognized.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(T object, Class<? extends InvocationHandler> handlerClass) {
        if (object instanceof Proxy) {
            InvocationHandler h = handler(object, handlerClass);
            if (h != null && h instanceof WrappingProxy) {
                return (T) ((WrappingProxy) h).getProxyObject();
            }
        }
        if (object instanceof ProxyList) {
            return (T) ((ProxyList) object).proxyList;
        }

        return object;
    }

    /**
     * Returns the invocation handler from a proxy object.
     *
     * @param object The proxy object.
     * @param handlerClass The class of invocation handler to return.
     * @return THe invocation handler, or null if non matchining the specified class can be found.
     */
    public static <H extends InvocationHandler> H handler(Object object, Class<H> handlerClass) {
        if (object instanceof Proxy) {
            InvocationHandler h = Proxy.getInvocationHandler(object);
            if (handlerClass.isInstance(h)) {
                return handlerClass.cast(h);
            }
        }

        return null;
    }
}
