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

package org.geoserver.wms;

import java.util.Map;
import org.geoserver.ows.Dispatcher;

/**
 * Defines a general Request type and provides accessor methods for universal request information.
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Gabriel Roldan
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 * @version $Id$
 */
public abstract class WMSRequest {

    private String baseUrl;

    private Map<String, String> rawKvp;

    /** flag indicating if the request is get */
    protected boolean get;

    protected String request;

    protected String version;

    private String requestCharset;

    /**
     * Creates the new request with the given operation name
     *
     * @param request name of the request, (Example, GetCapabiliites)
     */
    protected WMSRequest(final String request) {
        setRequest(request);
    }

    /**
     * Tells whether the originating request used HTTP GET method or not; may be useful, for
     * example, to determine if client can do HTTP caching and then set the corresponding response
     * headers.
     *
     * @return {@code true} if the originating HTTP request used HTTP GET method, {@code false}
     *     otherwise
     */
    public boolean isGet() {
        return get;
    }

    public void setGet(boolean get) {
        this.get = get;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setRawKvp(Map<String, String> rawKvp) {
        this.rawKvp = rawKvp;
    }

    /** Set by {@link Dispatcher} */
    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    /** Gets the raw kvp parameters which were used to create the request. */
    public Map<String, String> getRawKvp() {
        return rawKvp;
    }

    /** Setter for the 'WMTVER' parameter, which is an alias for 'VERSION'. */
    public void setWmtVer(String version) {
        setVersion(version);
    }

    /** @return the HTTP request charset, may be {@code null} */
    public String getRequestCharset() {
        return requestCharset;
    }

    public void setRequestCharset(String requestCharset) {
        this.requestCharset = requestCharset;
    }
}
