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

package org.geoserver.security.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;

/**
 * Implementation for testing. All {@link Authentication} objects are stored serialized to be
 * prepared for clustering.
 *
 * @author mcr
 */
public class TestingAuthenticationCache implements AuthenticationCache {

    Map<String, Map<String, byte[]>> cache = new HashMap<>();

    public static Integer DEFAULT_IDLE_SECS = 60;
    public static Integer DEFAULT_LIVE_SECS = 600;

    Map<String, Map<String, Integer[]>> expireMap = new HashMap<>();

    @Override
    public void removeAll() {
        cache.clear();
        expireMap.clear();
    }

    @Override
    public void removeAll(String filterName) {
        cache.remove(filterName);
        expireMap.remove(filterName);
    }

    @Override
    public void remove(String filterName, String cacheKey) {
        Map<String, byte[]> map = cache.get(filterName);
        if (map != null) map.remove(cacheKey);

        Map<String, Integer[]> map2 = expireMap.get(filterName);
        if (map2 != null) map.remove(cacheKey);
    }

    @Override
    public Authentication get(String filterName, String cacheKey) {
        Map<String, byte[]> map = cache.get(filterName);
        if (map != null) return deserializeAuthentication(map.get(cacheKey));
        else return null;
    }

    public Integer[] getExpireTimes(String filterName, String cacheKey) {
        Integer[] result = null;
        Map<String, Integer[]> map = expireMap.get(filterName);
        if (map != null) result = map.get(cacheKey);
        if (result == null) return new Integer[] {DEFAULT_IDLE_SECS, DEFAULT_LIVE_SECS};
        return result;
    }

    @Override
    public void put(
            String filterName,
            String cacheKey,
            Authentication auth,
            Integer timeToIdleSeconds,
            Integer timeToLiveSeconds) {
        put(filterName, cacheKey, auth);
        if (timeToIdleSeconds != null || timeToLiveSeconds != null) {
            Map<String, Integer[]> map = expireMap.get(filterName);
            if (map == null) {
                map = new HashMap<>();
                expireMap.put(filterName, map);
            }
            map.put(cacheKey, new Integer[] {timeToIdleSeconds, timeToLiveSeconds});
        }
    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth) {
        Map<String, byte[]> map = cache.get(filterName);
        if (map == null) {
            map = new HashMap<>();
            cache.put(filterName, map);
        }
        map.put(cacheKey, serializeAuthentication(auth));
    }

    Authentication deserializeAuthentication(byte[] bytes) {
        if (bytes == null) return null;
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bin);
            Authentication auth = (Authentication) in.readObject();
            in.close();
            return auth;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte[] serializeAuthentication(Authentication auth) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(auth);
            out.close();
            return bout.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
