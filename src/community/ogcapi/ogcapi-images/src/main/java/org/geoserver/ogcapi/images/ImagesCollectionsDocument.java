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

package org.geoserver.ogcapi.images;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractDocument;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.http.HttpStatus;

/**
 * A class representing the image server "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class ImagesCollectionsDocument extends AbstractDocument {

    static final Logger LOGGER = Logging.getLogger(ImagesCollectionsDocument.class);

    private final GeoServer gs;

    public ImagesCollectionsDocument(GeoServer gs) {
        this.gs = gs;

        // build the self links
        addSelfLinks("ogc/images/collections/");
    }

    public Iterator<ImagesCollectionDocument> getCollections() {

        CloseableIterator<CoverageInfo> coverages =
                gs.getCatalog().list(CoverageInfo.class, Filter.INCLUDE);
        boolean skipInvalid =
                gs.getGlobal().getResourceErrorHandling()
                        == ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS;
        return new Iterator<ImagesCollectionDocument>() {

            ImagesCollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                while (coverages.hasNext()) {
                    CoverageInfo coverage = coverages.next();
                    try {
                        if (coverage.getGridCoverageReader(null, null)
                                instanceof StructuredGridCoverage2DReader) {
                            ImagesCollectionDocument collection =
                                    new ImagesCollectionDocument(coverage, true);
                            next = collection;
                            return true;
                        }
                    } catch (Exception e) {
                        if (skipInvalid) {
                            LOGGER.log(Level.WARNING, "Skipping coverage  " + coverage);
                        } else {
                            throw new APIException(
                                    "InternalError",
                                    "Failed to iterate over the coverages in the catalog",
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    e);
                        }
                    }
                }

                return next != null;
            }

            @Override
            public ImagesCollectionDocument next() {
                ImagesCollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
    }
}
