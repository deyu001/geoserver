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

package org.geoserver.ows.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.platform.ServiceException;
import org.geotools.util.SoftValueHashMap;

/**
 * Utility class for performing reflective operations and other ows utility functions.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class OwsUtils {

    /**
     * Reflectively sets a property on an object.
     *
     * <p>This method uses {@link #setter(Class, String, Class)} to locate teh setter method for the
     * property and then invokes it with teh specified <tt>value</tt>.
     *
     * <p>The <tt>property</tt> parameter may be specified as a "path" of the form "prop1.prop2". If
     * any of the resulting properties along the path result in null this method will throw {@link
     * NullPointerException}
     *
     * @param object The target object.
     * @param property The property to set.
     * @param value The value to set, may be <code>null</code>.
     * @throws IllegalArgumentException If no such property exists.
     * @throws RuntimeException If an error occurs setting the property
     * @throws NullPointerException If the property specifies a property that results in null.
     */
    public static void set(Object object, String property, Object value)
            throws IllegalArgumentException {
        String[] props = property.split("\\.");
        Method s = null;
        if (props.length > 1) {
            for (int i = 0; i < props.length - 1 && object != null; i++) {
                object = get(object, props[i]);
            }
            if (object == null) {
                throw new NullPointerException(
                        "Property '" + property + "' is null for object " + object);
            }
            s =
                    setter(
                            object.getClass(),
                            props[props.length - 1],
                            value != null ? value.getClass() : null);
        } else {
            s = setter(object.getClass(), property, value != null ? value.getClass() : null);
        }

        if (s == null) {
            throw new IllegalArgumentException(
                    "No such property '" + property + "' for object " + object);
        }

        try {
            s.invoke(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Cache of reflection information about a class, keyed by class. */
    static Map<Class, ClassProperties> classPropertiesCache = new SoftValueHashMap<>();

    /** Accessor for the class to property info cache. */
    static ClassProperties classProperties(Class clazz) {
        // SoftValueHashMap is thread safe, no need to synch
        ClassProperties properties = classPropertiesCache.get(clazz);
        if (properties == null) {
            properties = new ClassProperties(clazz);
            classPropertiesCache.put(clazz, properties);
        }
        return properties;
    }

    /** Returns the properties object describing the properties of a class. */
    public static ClassProperties getClassProperties(Class clazz) {
        return classProperties(clazz);
    }

    /**
     * Returns a setter method for a property of java bean.
     *
     * <p>The <tt>type</tt> parameter may be <code>null</code> to indicate the the setter for the
     * property should be returned regardless of the type. If not null it will be used to filter the
     * returned method.
     *
     * @param clazz The type of the bean.
     * @param property The property name.
     * @param type The type of the property, may be <code>null</code>.
     * @return The setter method, or <code>null</code> if not found.
     */
    public static Method setter(Class clazz, String property, Class type) {
        return classProperties(clazz).setter(property, type);
    }

    /**
     * Reflectively determines if an object has a specified property.
     *
     * @param object The target object.
     * @param property The property to lookup.
     * @return True if the property exists, otherwise false.
     */
    public static boolean has(Object object, String property) {
        return getter(object.getClass(), property, null) != null;
    }

    /**
     * Reflectively gets a property from an object.
     *
     * <p>This method uses {@link #getter(Class, String, Class)} to locate the getter method for the
     * property and then invokes it.
     *
     * <p>The <tt>property</tt> parameter may be specified as a "path" of the form "prop1.prop2". If
     * any of the resulting properties along the path result in null this method will return null.
     *
     * @param object The target object.
     * @param property The property to set.
     * @throws IllegalArgumentException If no such property exists.
     * @throws RuntimeException If an error occurs getting the property
     */
    public static Object get(Object object, String property) {
        String[] props = property.split("\\.");
        Object result = object;
        for (int i = 0; i < props.length && result != null; i++) {
            String prop = props[i];
            Method g = getter(result.getClass(), props[i], null);
            if (g == null) {
                throw new IllegalArgumentException(
                        "No such property '" + prop + "' for object " + result);
            }
            try {
                result = g.invoke(result, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    /**
     * Reflectively puts a key, value into a Map property.
     *
     * @param object The target object.
     * @param property The Map property.
     * @param key The key to place into the map.
     * @param value The value to place into the map.
     * @throws IllegalArgumentException If the property specified is not a map
     * @throws NullPointerException If the property specifies is null
     */
    public static void put(Object object, String property, Object key, Object value) {
        Object o = get(object, property);
        if (o == null) {
            throw new NullPointerException("Property " + property + " is null");
        }

        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("Property " + property + " is not a map");
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> o1 = (Map) o;
        o1.put(key, value);
    }

    /**
     * Returns a getter method for a property of java bean.
     *
     * @param clazz The type of the bean.
     * @param property The property name.
     * @param type The type of the property, may be null.
     * @return The setter method, or <code>null</code> if not found.
     */
    public static Method getter(Class clazz, String property, Class type) {
        return classProperties(clazz).getter(property, type);
    }

    /**
     * Reflectivley retreives a propety from a java bean.
     *
     * @param <T>
     * @param object The java bean.
     * @param property The property to retreive.
     * @param type Teh type of the property to retreive.
     * @return The property, or null if it could not be found..
     */
    public static <T> T property(Object object, String property, Class<T> type) {
        Method getter = getter(object.getClass(), property, type);

        if (getter != null) {
            try {
                return type.cast(getter.invoke(object, (Object[]) null));
            } catch (Exception e) {
                // TODO: log this
            }
        }

        return null;
    }

    /**
     * Returns a method with a pariticular name of a class, ignoring method paramters.
     *
     * @param clazz The class
     * @param name The name of the method.
     * @return The method, or <code>null</code> if it could not be found.
     */
    public static Method method(Class clazz, String name) {
        return classProperties(clazz).method(name);
    }

    /**
     * Returns an object of a particular type in a list of objects of various types.
     *
     * @param parameters A list of objects, of various types.
     * @param type The type of paramter to be returned.
     * @return The object of the specified type, or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Object> T parameter(Object[] parameters, Class<T> type) {
        for (Object parameter : parameters) {
            if ((parameter != null) && type.isAssignableFrom(parameter.getClass())) {
                return (T) parameter;
            }
        }

        return null;
    }

    /** Dumps a stack of service exception messages to a string buffer. */
    public static void dumpExceptionMessages(Throwable e, StringBuffer s, boolean xmlEscape) {
        Throwable ex = e;
        do {
            Throwable cause = ex.getCause();
            final String message = ex.getMessage();
            String lastMessage = message;
            if (message != null && !"".equals(message)) {
                if (xmlEscape) s.append(ResponseUtils.encodeXML(message));
                else s.append(message);
                if (ex instanceof ServiceException) {
                    for (String value : ((ServiceException) ex).getExceptionText()) {
                        s.append("\n");
                        String msg = value;
                        if (!lastMessage.equals(msg)) {
                            if (xmlEscape) s.append(ResponseUtils.encodeXML(msg));
                            else s.append(msg);
                            lastMessage = msg;
                        }
                    }
                }
                if (cause != null && cause.getMessage() != null && !"".equals(cause.getMessage()))
                    s.append("\n");
            }

            // avoid infinite loop if someone did the very stupid thing of setting
            // the cause as the exception itself (I only found this situation once, but...)
            if (ex == cause || cause == null) break;
            else ex = cause;
        } while (true);

        // some exceptions have no description, use the exception name itself then
        if (s.length() == 0 && e != null) {
            s.append(e.getClass().getName());
        }
    }

    /**
     * Copies properties from one object to another.
     *
     * @param source The source object.
     * @param target The target object.
     * @param clazz The class of source and target.
     */
    public static <T> void copy(T source, T target, Class<T> clazz) {
        ClassProperties properties = getClassProperties(clazz);
        for (String p : properties.properties()) {
            Method getter = properties.getter(p, null);
            if (getter == null) {
                continue; // should not really happen
            }

            Class type = getter.getReturnType();
            Method setter = properties.setter(p, type);

            // do a check for read only before calling the getter to avoid an uneccesary call
            if (setter == null
                    && !(Collection.class.isAssignableFrom(type)
                            || Map.class.isAssignableFrom(type))) {
                // read only
                continue;
            }

            try {
                Object newValue = getter.invoke(source, null);
                if (newValue == null) {
                    continue;
                    // TODO: make this a flag whether to overwrite with null values
                }
                if (setter == null) {
                    if (Collection.class.isAssignableFrom(type)) {
                        updateCollectionProperty(target, (Collection) newValue, getter);
                    } else if (Map.class.isAssignableFrom(type)) {
                        updateMapProperty(target, (Map) newValue, getter);
                    }
                    continue;
                }

                setter.invoke(target, newValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Reflectively sets all collections when they are null. */
    public static void resolveCollections(Object object) {
        ClassProperties properties = OwsUtils.getClassProperties(object.getClass());
        for (String property : properties.properties()) {
            Method g = properties.getter(property, null);
            if (g == null) {
                continue;
            }

            Class<?> type = g.getReturnType();
            // only continue if this is a collection or a map
            if (!(Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type))) {
                continue;
            }

            // only continue if there is also a setter as well
            Method s = properties.setter(property, null);
            if (s == null) {
                continue;
            }

            // if the getter returns null, call the setter
            try {
                Object value = g.invoke(object, null);
                if (value == null) {
                    // first attempt to instantiate the type directly in case the method declares
                    // a non interface or abstract class
                    if (!type.isInterface()) {
                        try {
                            value = type.getConstructor().newInstance();
                        } catch (Exception e) {
                            // fall through to defaults
                        }
                    }
                    if (value == null) {
                        if (Map.class.isAssignableFrom(type)) {
                            value = new HashMap();
                        } else if (List.class.isAssignableFrom(type)) {
                            value = new ArrayList();
                        } else if (Set.class.isAssignableFrom(type)) {
                            value = new HashSet();
                        } else {
                            throw new RuntimeException("Unknown collection type:" + type.getName());
                        }
                    }

                    // initialize
                    s.invoke(object, value);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Helper method for updating a collection based property. Only used if setter is null. */
    @SuppressWarnings("unchecked")
    static void updateCollectionProperty(Object object, Collection newValue, Method getter)
            throws Exception {
        Collection<Object> oldValue = (Collection) getter.invoke(object, null);
        if (oldValue != null) {
            oldValue.clear();
            oldValue.addAll(newValue);
        }
    }

    /** Helper method for updating a map based property. Only used if setter is null. */
    @SuppressWarnings("unchecked")
    static void updateMapProperty(Object object, Map newValue, Method getter) throws Exception {
        Map<Object, Object> oldValue = (Map) getter.invoke(object, null);
        if (oldValue != null) {
            oldValue.clear();
            oldValue.putAll(newValue);
        }
    }
}
