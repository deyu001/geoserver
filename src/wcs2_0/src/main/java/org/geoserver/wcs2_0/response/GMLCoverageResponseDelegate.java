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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.BaseCoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Encoding a {@link GridCoverage2D} as per WCS 2.0 GML format.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GMLCoverageResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate {

    /** FILE_EXTENSION */
    private static final String FILE_EXTENSION = "gml";

    /** MIME_TYPE */
    private static final String MIME_TYPE = "application/gml+xml";

    /** Can be used to map dimensions name to indexes */
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    @SuppressWarnings("serial")
    public GMLCoverageResponseDelegate(
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper, GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList(FILE_EXTENSION, MIME_TYPE), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put(MIME_TYPE, FILE_EXTENSION);
                        put(FILE_EXTENSION, FILE_EXTENSION);
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put(MIME_TYPE, MIME_TYPE);
                        put(FILE_EXTENSION, MIME_TYPE);
                    }
                });
        this.envelopeDimensionsMapper = envelopeDimensionsMapper;
    }

    @Override
    public void encode(
            GridCoverage2D coverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        final GMLTransformer transformer = new GMLTransformer(envelopeDimensionsMapper);
        transformer.setIndentation(4);
        try {
            transformer.transform(coverage, output);
        } catch (TransformerException e) {
            throw new WcsException(e);
        }
    }
}
