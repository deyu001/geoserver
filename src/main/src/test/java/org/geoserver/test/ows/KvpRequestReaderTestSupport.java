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

package org.geoserver.test.ows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.test.GeoServerTestSupport;

/**
 * Test class for testing instances of {@link KvpRequestReader}.
 *
 * <p>The {@link #parseKvp(Map)} method of this class sets up a kvp map and parses it by processing
 * instances of {@link KvpParser} in the application context.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class KvpRequestReaderTestSupport extends GeoServerTestSupport {
    /**
     * Parses a raw set of kvp's into a parsed set of kvps.
     *
     * @param kvp Map of String,String.
     */
    protected Map<String, Object> parseKvp(Map<String, Object> raw) throws Exception {

        // parse like the dispatcher but make sure we don't change the original map
        Map<String, Object> input = new HashMap<>(raw);
        List<Throwable> errors = KvpUtils.parse(input);
        if (errors != null && !errors.isEmpty()) throw (Exception) errors.get(0);

        return caseInsensitiveKvp(input);
    }

    protected <T> Map<String, Object> caseInsensitiveKvp(Map<String, Object> input) {
        // make it case insensitive like the servlet+dispatcher maps
        Map<String, Object> result = new HashMap<>();
        for (String key : input.keySet()) {
            result.put(key.toUpperCase(), input.get(key));
        }
        return new CaseInsensitiveMap<>(result);
    }
}
