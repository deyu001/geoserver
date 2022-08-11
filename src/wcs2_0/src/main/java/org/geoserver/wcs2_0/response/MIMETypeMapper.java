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

package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitorAdapter;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Simple mapping utility to map native formats to Mime Types using ImageIO reader capabilities.
 *
 * <p>It does perform caching of the mappings. Tha cache should be very small, hence it uses hard
 * references.
 *
 * @author Simone Giannechini, GeoSolutions
 */
public class MIMETypeMapper implements ApplicationContextAware {

    private static final String NO_MIME_TYPE = "NoMimeType";

    public static final String DEFAULT_FORMAT =
            GeoTIFFCoverageResponseDelegate.GEOTIFF_CONTENT_TYPE;

    private Logger LOGGER = Logging.getLogger(MIMETypeMapper.class);

    private final SoftValueHashMap<String, String> mimeTypeCache = new SoftValueHashMap<>(100);

    private final Set<String> outputMimeTypes = new HashSet<>();

    private List<CoverageMimeTypeMapper> mappers;

    /** Constructor. */
    private MIMETypeMapper(CoverageResponseDelegateFinder finder, Catalog catalog) {
        // collect all of the output mime types
        for (String of : finder.getOutputFormats()) {
            CoverageResponseDelegate delegate = finder.encoderFor(of);
            String mime = delegate.getMimeType(of);
            outputMimeTypes.add(mime);
        }
        catalog.addListener(new MimeTypeCacheClearingListener());
    }

    /**
     * Returns a mime types for the provided {@link CoverageInfo} using the {@link
     * CoverageInfo#getNativeFormat()} as its key. In case none was found, the DEFAULT_FORMAT format
     * is returned.
     *
     * @param cInfo the {@link CoverageInfo} to find a mime type for
     * @return a mime types or null for the provided {@link CoverageInfo} using the {@link
     *     CoverageInfo#getNativeFormat()} as its key.
     * @throws IOException in case we don't manage to open the underlying file
     */
    public String mapNativeFormat(final CoverageInfo cInfo) throws IOException {
        // checks
        Utilities.ensureNonNull("cInfo", cInfo);

        String mime = mimeTypeCache.get(cInfo.getId());
        if (mime != null) {
            if (NO_MIME_TYPE.equals(mime)) {
                return DEFAULT_FORMAT;
            } else {
                return mime;
            }
        }

        for (CoverageMimeTypeMapper mapper : mappers) {
            mime = mapper.getMimeType(cInfo);
            if (mime != null) {
                break;
            }
        }

        // the native format must be encodable
        if (mime != null && outputMimeTypes.contains(mime)) {
            mimeTypeCache.put(cInfo.getId(), mime);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Added mapping for mime: " + mime);
            }
            return mime;
        } else {
            // we either don't have a clue about the mime, or we don't have an encoder,
            // save the response as null
            mimeTypeCache.put(cInfo.getId(), NO_MIME_TYPE);
            return DEFAULT_FORMAT;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        mappers = GeoServerExtensions.extensions(CoverageMimeTypeMapper.class, applicationContext);
    }

    /**
     * Cleans the mime type cache contents on reload
     *
     * @author Andrea Aime - GeoSolutions
     */
    public class MimeTypeCacheClearingListener extends CatalogVisitorAdapter
            implements CatalogListener {

        public void handleAddEvent(CatalogAddEvent event) {}

        public void handleModifyEvent(CatalogModifyEvent event) {}

        public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            event.getSource().accept(this);
        }

        public void handleRemoveEvent(CatalogRemoveEvent event) {
            event.getSource().accept(this);
        }

        public void reloaded() {
            outputMimeTypes.clear();
        }

        @Override
        public void visit(CoverageInfo coverage) {
            outputMimeTypes.remove(coverage.getId());
        }
    }
}
