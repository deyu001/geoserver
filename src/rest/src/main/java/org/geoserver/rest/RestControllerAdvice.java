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

package org.geoserver.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Controller advice for geoserver rest
 *
 * <p>A note on the exception handling here:
 *
 * <p>The manual exception handling, using the response output stream directly and the
 * response/request directly is very much NOT RECOMMENDED. Prefer to use ResponseEntity objects to
 * return proper errors.
 *
 * <p>BUT
 *
 * <p>GeoServer test cases do two silly things:
 *
 * <p>- Make requests without any accepts and then look for an exact string in the response. Without
 * the accepts header spring has no idea what the response should be, so it tries to pick the first
 * default based on the producible media types. This is, frequently, HTML
 */
@ControllerAdvice
public class RestControllerAdvice extends ResponseEntityExceptionHandler {

    static final Logger LOGGER = Logging.getLogger(RestControllerAdvice.class);

    private void notifyExceptionToCallbacks(
            WebRequest webRequest, HttpServletResponse response, Exception ex) {
        if (!(webRequest instanceof ServletWebRequest)) {
            return;
        }
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        notifyExceptionToCallbacks(request, response, ex);
    }

    private void notifyExceptionToCallbacks(
            HttpServletRequest request, HttpServletResponse response, Exception ex) {
        List<DispatcherCallback> callbacks =
                GeoServerExtensions.extensions(DispatcherCallback.class);
        for (DispatcherCallback callback : callbacks) {
            callback.exception(request, response, ex);
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public void handleResourceNotFound(
            ResourceNotFoundException e,
            HttpServletResponse response,
            WebRequest request,
            OutputStream os)
            throws IOException {
        notifyExceptionToCallbacks(request, response, e);

        String quietOnNotFound =
                request.getParameter("quietOnNotFound"); // yes this is seriously a thing
        String message = e.getMessage();
        if (Boolean.parseBoolean(quietOnNotFound)) {
            message = "";
        } else {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        response.setStatus(404);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        StreamUtils.copy(message, Charset.forName("UTF-8"), os);
    }

    @ExceptionHandler(RestException.class)
    public void handleRestException(
            RestException e, HttpServletResponse response, WebRequest request, OutputStream os)
            throws IOException {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
        notifyExceptionToCallbacks(request, response, e);

        if (e.getStatus().is4xxClientError()) {
            response.sendError(e.getStatus().value(), e.getMessage());
        } else {
            response.setStatus(e.getStatus().value());
        }
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        StreamUtils.copy(e.getMessage(), Charset.forName("UTF-8"), os);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleGeneralException(
            Exception e, HttpServletRequest request, HttpServletResponse response, OutputStream os)
            throws Exception {
        // if there is a OGC request active, the exception was not meant for this dispatcher,
        // nor it was if it's a security exception, in this case let servlet filters handle it
        // instead
        if (Dispatcher.REQUEST.get() != null
                || e instanceof AuthenticationException
                || e instanceof AccessDeniedException) {
            throw e;
        }
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
        notifyExceptionToCallbacks(request, response, e);

        response.setStatus(500);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        StreamUtils.copy(e.getMessage(), Charset.forName("UTF-8"), os);
    }
}
