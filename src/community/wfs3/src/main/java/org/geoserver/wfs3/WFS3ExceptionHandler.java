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

package org.geoserver.wfs3;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;

/**
 * Returns exceptions as a JSON document according to the WFS 3 draft spec
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WFS3ExceptionHandler extends ServiceExceptionHandler {

    private GeoServer geoServer;

    public WFS3ExceptionHandler(List services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {
        HttpServletResponse response = request.getHttpResponse();
        response.setContentType(BaseRequest.JSON_MIME);

        if (exception instanceof OWS20Exception) {
            OWS20Exception ex = (OWS20Exception) exception;
            if (ex.getHttpCode() != null) {
                response.setStatus(ex.getHttpCode());
            } else {
                response.setStatus(500);
            }
        } else {
            OWS20Exception.OWSExceptionCode code =
                    OWS20Exception.OWSExceptionCode.getByCode(exception.getCode());
            if (code != null) {
                response.setStatus(code.getHttpCode());
            } else {
                response.setStatus(500);
            }
        }

        Map<String, String> error = new LinkedHashMap<>();
        error.put("code", exception.getCode());
        error.put("description", getDescription(geoServer.getGlobal(), exception));
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), error);
        } catch (Exception ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        } finally {
            try {
                request.getHttpResponse().getOutputStream().flush();
            } catch (IOException ignored) {
            }
        }
    }

    private String getDescription(GeoServerInfo geoServer, ServiceException e) {
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(e, sb, true);

        if (geoServer.getSettings().isVerboseExceptions()) {
            ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(stackTrace));

            sb.append("\nDetails:\n");
            sb.append(new String(stackTrace.toByteArray()));
        }

        return sb.toString();
    }
}
