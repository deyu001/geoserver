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

package org.geoserver.catalog;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.geotools.util.Converters;

/**
 * A map used to store metadata on catalog and configuration objects.
 *
 * <p>This map is used to convert values from the map to a specified type, via the {@link
 * #get(String, Class)} method. Usage:
 *
 * <pre>
 * MetadataMap map = new MetadataMap();
 * map.put( "one", "1");
 *
 * map.get( "one" ) ==> "1"
 * map.get( "one", Integer.class) ==> 1
 * map.get( "one", Double.class ) ==> 1.0
 * </pre>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MetadataMap implements Map<String, Serializable>, Serializable {

    private static final long serialVersionUID = -3267986531863264568L;
    protected String id;

    protected Map<String, Serializable> map;

    public MetadataMap() {
        this(new HashMap<>());
    }

    public MetadataMap(Map<String, Serializable> map) {
        if (!(map instanceof Serializable)) {
            throw new IllegalArgumentException("map is not serializable");
        }
        this.map = map;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Serializable> getMap() {
        return map;
    }

    public void setMap(Map<String, Serializable> map) {
        this.map = map;
    }

    public <T> T get(String key, Class<T> clazz) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }

        return Converters.convert(obj, clazz);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Serializable get(Object key) {
        return map.get(key);
    }

    public Serializable put(String key, Serializable value) {
        return map.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Serializable> t) {
        map.putAll(t);
    }

    public Serializable remove(Object key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<Serializable> values() {
        return map.values();
    }

    public Set<java.util.Map.Entry<String, Serializable>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
