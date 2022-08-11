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

package org.geoserver.opensearch.eo;

import java.util.HashSet;
import java.util.Map;
import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.springframework.util.StringUtils;

/**
 * Temporary trick to force GeoServer KVP parsing of description when there is no KVP param at all
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEODispatcherCallback extends AbstractDispatcherCallback {

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        final Map kvp = request.getKvp();
        final Map rawKvp = request.getRawKvp();
        if ("oseo".equalsIgnoreCase(request.getService())) {
            if (kvp.isEmpty()) {
                if ("description".equals(request.getRequest())) {
                    kvp.put("service", "oseo");
                    // the raw kvp is normally not even initialized
                    request.setRawKvp(kvp);
                } else if ("search".equals(request.getRequest())) {
                    kvp.put("service", "oseo");
                    kvp.put("httpAccept", AtomSearchResponse.MIME);
                }
                // make sure the raw kvp is not empty, ever (the current code
                // leaves it empty if the request has no search params)
                request.setRawKvp(kvp);
            } else {
                // skip everything that has an empty value, in OpenSearch it should be ignored
                // (clients following the template to the letter will create keys with empty value)
                for (String key : new HashSet<String>(request.getRawKvp().keySet())) {
                    Object value = rawKvp.get(key);
                    if ((!(value instanceof String) || StringUtils.isEmpty((String) value))
                            && !(value instanceof String[])) {
                        rawKvp.remove(key);
                        kvp.remove(key);
                    }
                }
            }
        }
        return service;
    }
}
