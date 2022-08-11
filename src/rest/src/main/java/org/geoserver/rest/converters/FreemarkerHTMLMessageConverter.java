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

package org.geoserver.rest.converters;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/** Message converter for Freemarker-generated HTML output */
public class FreemarkerHTMLMessageConverter extends BaseMessageConverter<RestWrapper<?>> {

    /** Encoding (null for default) */
    protected String encoding;

    public FreemarkerHTMLMessageConverter() {
        super(MediaType.TEXT_HTML);
    }

    public FreemarkerHTMLMessageConverter(String encoding) {
        this();
        this.encoding = encoding;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RestWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return false; // reading not supported
    }

    @Override
    protected RestWrapper<?> readInternal(
            Class<? extends RestWrapper<?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    /**
     * Write an given object to the given output message as HTML, invoked from {@link #write}.
     *
     * @param wrapper The wrapped object write to the output message
     * @param outputMessage the HTTP output message to write to
     * @throws IOException in case of I/O errors
     * @throws HttpMessageNotWritableException in case of conversion errors
     */
    @Override
    @SuppressWarnings("PMD.CloseResource") // actual stream managed by servlet
    protected void writeInternal(RestWrapper<?> wrapper, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();

        Writer templateWriter = null;
        wrapper.configureFreemarker(this);
        try {
            Object object = wrapper.getObject();
            Template template = wrapper.getTemplate();
            OutputStream outputStream = outputMessage.getBody();
            Charset charSet =
                    contentType != null ? contentType.getCharset() : getGeoServerDefaultCharset();

            if (charSet != null) {
                templateWriter =
                        new BufferedWriter(new OutputStreamWriter(outputStream, charSet.name()));
            } else {
                templateWriter =
                        new BufferedWriter(
                                new OutputStreamWriter(outputStream, template.getEncoding()));
            }

            template.process(object, templateWriter);
            templateWriter.flush();
        } catch (TemplateException te) {
            throw new IOException("Template processing error " + te.getMessage());
        }
    }

    private Charset getGeoServerDefaultCharset() {
        return Charset.forName(
                GeoServerExtensions.bean(GeoServer.class).getGlobal().getSettings().getCharset());
    }

    public int getPriority() {
        // If no extension or content-type provided, return HTML;
        return ExtensionPriority.LOWEST - 1;
    }

    public List<URL> createCollectionLink(String link) {
        // TODO Auto-generated method stub
        try {
            String href = href(link);
            URL url2 = new URL(href);
            return Collections.singletonList(url2);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    protected String href(String link) {

        final RequestInfo pg = RequestInfo.get();
        link += ".html";

        // encode as relative or absolute depending on the link type
        if (link.startsWith("/")) {
            // absolute, encode from "root"
            return pg.servletURI(link);
        } else {
            // encode as relative
            return pg.pageURI(link);
        }
    }
}
