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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.checkerframework.checker.units.qual.K;

/**
 * Map decorator which makes String keys case-insensitive.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
// Implementation note, the "K extends String" bit is weird, but fixing String would have meant
// to have a single parameter map, I've found it to be even more strange to go and declare
// CaseInsensitiveMap<Object> in the user code
public class CaseInsensitiveMap<K extends String, V> implements Map<K, V> {
    Map<K, V> delegate = new TreeMap<>();

    public CaseInsensitiveMap(Map<K, V> delegate) {
        putAll(delegate);
    }

    public void clear() {
        delegate.clear();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(upper(key));
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public V get(Object key) {
        return delegate.get(upper(key));
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Set<K> keySet() {
        return delegate.keySet();
    }

    public V put(K key, V value) {
        return delegate.put(upper(key), value);
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        // make sure to upcase all keys
        for (Entry<? extends K, ? extends V> entry : t.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            put(key, value);
        }
    }

    public V remove(Object key) {
        return delegate.remove(upper(key));
    }

    public int size() {
        return delegate.size();
    }

    public Collection<V> values() {
        return delegate.values();
    }

    @SuppressWarnings("unchecked")
    <T> T upper(T key) {
        if ((key != null) && key instanceof String) {
            return (T) ((String) key).toUpperCase();
        }

        return key;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    /**
     * Wraps a map in case insensitive one.
     *
     * <p>If the instance is already a case insensitive map it is returned as is.
     */
    public static <V> Map<String, V> wrap(Map<String, V> other) {
        if (other instanceof CaseInsensitiveMap) {
            return other;
        }
        return new CaseInsensitiveMap<>(other);
    }
}
