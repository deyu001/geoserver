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

import java.io.IOException;
import java.nio.charset.Charset;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Base message converter behavior for XStream XML or JSON converters.
 *
 * <p>Local fields have been provided for {@link #catalog} and {@link #geoServer} access.
 */
public abstract class BaseMessageConverter<T> extends AbstractHttpMessageConverter<T>
        implements HttpMessageConverter<T>, ExtensionPriority {

    protected final Catalog catalog;

    protected final XStreamPersisterFactory xpf;

    protected final GeoServer geoServer;

    //    /**
    //     * Construct an {@code BaseMessageConverter} with no supported media types.
    //     * @see #setSupportedMediaTypes
    //     */
    //    public BaseMessageConverter() {
    //        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
    //        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
    //        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    //    }

    /**
     * Construct an {@code BaseMessageConverter} with supported media types.
     *
     * @param supportedMediaTypes the supported media types
     */
    protected BaseMessageConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }

    /**
     * Construct an {@code BaseMessageConverter} with a default charset and supported media types.
     *
     * @param defaultCharset the default character set
     * @param supportedMediaTypes the supported media types
     */
    protected BaseMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
        super(defaultCharset, supportedMediaTypes);
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }

    /** Returns the priority of the {@link BaseMessageConverter}. */
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    //    /**
    //     * Checks if the media type provided is "included" by one of the media types declared
    //     * in {@link #getSupportedMediaTypes()}
    //     */
    //    protected boolean isSupportedMediaType(MediaType mediaType) {
    //        for (MediaType supported : getSupportedMediaTypes()) {
    //            if(supported.includes(mediaType)) {
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

    //    @Override
    //    protected T readInternal(Class<? extends T> clazz,
    //            HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException
    // {
    //        throw new HttpMessageNotReadableException(getClass().getName()+" does not support
    // deserialization");
    //    }
    //
    //    @Override
    //    protected void writeInternal(T t, HttpOutputMessage outputMessage)
    //            throws IOException, HttpMessageNotWritableException {
    //        throw new HttpMessageNotReadableException(getClass().getName()+" does not support
    // serialization");
    //    }

    /* Default implementation provided for consistent not-implemented message */
    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException(
                getClass().getName() + " does not support deserialization", inputMessage);
    }

    /* Default implementation provided for consistent not-implemented message */
    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        throw new HttpMessageNotWritableException(
                getClass().getName() + " does not support serialization");
    }
}
