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

package org.geoserver.params.extractor;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public class UrlMangler implements URLMangler {

    private static final Logger LOGGER = Logging.getLogger(UrlMangler.class);

    private List<EchoParameter> echoParameters;

    public UrlMangler(GeoServerDataDirectory dataDirectory) {
        Resource resource = dataDirectory.get(EchoParametersDao.getEchoParametersPath());
        echoParameters = EchoParametersDao.getEchoParameters(resource.in());
        resource.addListener(
                notify -> echoParameters = EchoParametersDao.getEchoParameters(resource.in()));
    }

    private HttpServletRequest getHttpRequest(Request request) {
        HttpServletRequest httpRequest = request.getHttpRequest();
        while (httpRequest instanceof HttpServletRequestWrapper
                && !(httpRequest instanceof RequestWrapper)) {
            ServletRequest servlet = ((HttpServletRequestWrapper) httpRequest).getRequest();
            if (servlet instanceof HttpServletRequest) {
                httpRequest = (HttpServletRequest) servlet;
            } else {
                throw new RuntimeException("Only HttpRequest is supported");
            }
        }
        return httpRequest;
    }

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        Request request = Dispatcher.REQUEST.get();
        if (request == null || !"GetCapabilities".equalsIgnoreCase(request.getRequest())) {
            Utils.debug(
                    LOGGER,
                    "Not a capabilities request, ignored by the parameters extractor URL mangler.");
            return;
        }
        forwardOriginalUri(request, path);
        Map<String, Object> requestRawKvp = request.getRawKvp();
        HttpServletRequest httpRequest = getHttpRequest(request);
        if (httpRequest instanceof RequestWrapper) {
            RequestWrapper requestWrapper = (RequestWrapper) httpRequest;
            Map<String, String[]> parameters = requestWrapper.getOriginalParameters();
            requestRawKvp = new KvpMap<>(KvpUtils.normalize(parameters));
        }
        forwardParameters(requestRawKvp, kvp);
        Utils.debug(LOGGER, "Parameters extractor URL mangler applied.");
    }

    private void forwardOriginalUri(Request request, StringBuilder path) {
        HttpServletRequest httpRequest = getHttpRequest(request);
        String requestUri = httpRequest.getRequestURI();
        if (httpRequest instanceof RequestWrapper) {
            requestUri = ((RequestWrapper) httpRequest).getOriginalRequestURI();
        }
        int i = httpRequest.getContextPath().length() + 1;
        String pathInfo = requestUri.substring(i);
        path.delete(0, path.length());
        path.append(pathInfo);
    }

    private void forwardParameters(Map<String, Object> requestRawKvp, Map<String, String> kvp) {
        for (EchoParameter echoParameter : echoParameters) {
            if (!echoParameter.getActivated()) {
                continue;
            }
            Map.Entry<String, Object> rawParameter =
                    Utils.caseInsensitiveSearch(echoParameter.getParameter(), requestRawKvp);
            if (rawParameter != null
                    && Utils.caseInsensitiveSearch(echoParameter.getParameter(), kvp) == null) {
                if (rawParameter.getValue() instanceof String) {
                    kvp.put(rawParameter.getKey(), (String) rawParameter.getValue());
                }
            }
        }
    }
}
