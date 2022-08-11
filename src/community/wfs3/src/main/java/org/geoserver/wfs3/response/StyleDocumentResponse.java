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

package org.geoserver.wfs3.response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs3.GetStyleRequest;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.springframework.http.HttpStatus;

public class StyleDocumentResponse extends Response {

    private final Catalog catalog;

    public StyleDocumentResponse(Catalog catalog) {
        super(StyleInfo.class, getStyleFormats());
        this.catalog = catalog;
    }

    private static Set<String> getStyleFormats() {
        Set<String> result = new HashSet<>();
        for (StyleHandler handler : Styles.handlers()) {
            for (Version version : handler.getVersions()) {
                result.add(handler.mimeType(version));
                result.add(handler.getName().toLowerCase());
            }
        }

        return result;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetStyleRequest request = (GetStyleRequest) operation.getParameters()[0];
        final StyleInfo style = (StyleInfo) value;
        String requestedFormat = getRequestedFormat(request, style);
        final StyleHandler handler = Styles.handler(requestedFormat);
        return handler.mimeType(style.getFormatVersion());
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GetStyleRequest request = (GetStyleRequest) operation.getParameters()[0];
        StyleInfo style = (StyleInfo) value;
        String requestedFormat = getRequestedFormat(request, style);

        final StyleHandler handler = Styles.handler(requestedFormat);
        if (handler == null) {
            throw new HttpErrorCodeException(
                    HttpStatus.BAD_REQUEST.value(), "Cannot encode style in " + requestedFormat);
        }

        // if no conversion is needed, push out raw style
        if (Objects.equals(handler.getFormat(), style.getFormat())) {
            try (final BufferedReader reader = catalog.getResourcePool().readStyle(style)) {
                OutputStreamWriter writer = new OutputStreamWriter(output);
                IOUtils.copy(reader, writer);
                writer.flush();
            }
        } else {
            // otherwise convert if possible
            final StyledLayerDescriptor sld = style.getSLD();
            if (sld.getName() == null || sld.getName().isEmpty()) {
                sld.setName(style.getName());
            }
            handler.encode(sld, null, true, output);
        }
    }

    public String getRequestedFormat(GetStyleRequest request, StyleInfo style) {
        String requestedFormat = request.getOutputFormat();
        if (requestedFormat == null) {
            requestedFormat = style.getFormat();
        }
        if (requestedFormat == null) {
            requestedFormat = SLDHandler.FORMAT;
        }
        return requestedFormat;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        StyleInfo style = (StyleInfo) value;
        return style.getName() + "." + (style.getFormat() == null ? ".style" : style.getFormat());
    }
}
