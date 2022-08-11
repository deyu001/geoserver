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

package org.geoserver.wms.map;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.springframework.util.Assert;

/**
 * @author Simone Giannecchini, GeoSolutions
 * @author Gabriel Roldan
 */
public abstract class AbstractMapResponse extends Response {

    protected AbstractMapResponse(
            final Class<? extends WebMap> responseBinding, final String mime) {
        this(responseBinding, new String[] {mime});
    }

    @SuppressWarnings("unchecked")
    protected AbstractMapResponse(
            final Class<? extends WebMap> responseBinding, final String[] outputFormats) {
        this(
                responseBinding,
                outputFormats == null
                        ? Collections.emptySet()
                        : new HashSet<>(Arrays.asList(outputFormats)));
    }

    protected AbstractMapResponse(
            final Class<? extends WebMap> responseBinding, Set<String> outputFormats) {
        // Call Response superclass constructor with the kind of request we can handle
        // Make sure the output format comparison in canHandle is case insensitive
        super(responseBinding, caseInsensitiveOutputFormats(outputFormats));
    }

    private static Set<String> caseInsensitiveOutputFormats(Set<String> outputFormats) {
        if (outputFormats == null) {
            return Collections.emptySet();
        }
        Set<String> caseInsensitiveFormats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveFormats.addAll(outputFormats);
        return caseInsensitiveFormats;
    }

    protected AbstractMapResponse() {
        this(null, (String[]) null);
    }

    /**
     * @return {@code ((WebMap)value).getMimeType()}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(WebMap.class, value);
        return ((WebMap) value).getMimeType();
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        Assert.isInstanceOf(WebMap.class, value);
        // defer to WebMap - it has the extension and other information
        return ((WebMap) value).getAttachmentFileName();
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        Assert.isInstanceOf(WebMap.class, value);
        // defer to WebMap - it has the extension and other information
        return ((WebMap) value).getDisposition();
    }

    /**
     * Evaluates whether this response can handle the given operation by checking if the operation's
     * request is a {@link GetMapRequest} and the requested output format is contained in {@link
     * #getOutputFormatNames()}.
     *
     * <p>NOTE: requested MIME Types may come with parameters, like, for example: {@code
     * image/png;param1=value1}. This default canHandle implementation performs and exact match
     * check against the requested and supported format names. Subclasses may feel free to override
     * if needed.
     *
     * @see org.geoserver.ows.Response#canHandle(org.geoserver.platform.Operation)
     */
    @Override
    public boolean canHandle(final Operation operation) {
        GetMapRequest request;
        Object[] parameters = operation.getParameters();
        request = OwsUtils.parameter(parameters, GetMapRequest.class);
        if (request == null) {
            return false;
        }
        Set<String> outputFormats = getOutputFormats();
        if (outputFormats.isEmpty()) {
            // rely only on response binding
            return true;
        }
        String outputFormat = request.getFormat();
        boolean match = outputFormats.contains(outputFormat);
        return match;
    }

    /**
     * Returns a 2xn array of Strings, each of which is an HTTP header pair to be set on the HTTP
     * Response. Can return null if there are no headers to be set on the response.
     *
     * @param value must be a {@link WebMap}
     * @param operation The operation being performed.
     * @return {@link WebMap#getResponseHeaders()}: 2xn string array containing string-pairs of HTTP
     *     headers/values
     * @see Response#getHeaders(Object, Operation)
     * @see WebMap#getResponseHeaders()
     */
    @Override
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(WebMap.class, value);
        WebMap map = (WebMap) value;
        String[][] responseHeaders = map.getResponseHeaders();
        return responseHeaders;
    }
}
