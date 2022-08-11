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

package org.geoserver.wcs.web.demo;

import java.awt.geom.AffineTransform;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.filter.v1_0.OGC;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.GML;
import org.geotools.ows.v1_1.OWS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class to turn a {@link GetCoverageRequest} into the corresponding WCS 1.1 GetCoverage xml
 *
 * @author Andrea Aime - GeoSolutions
 */
class WCS11GetCoverageTransformer extends TransformerBase {

    static final Logger LOGGER = Logging.getLogger(WCS11GetCoverageTransformer.class);

    private Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;

    public WCS11GetCoverageTransformer(
            Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        this.catalog = catalog;
        this.responseFactory = responseFactory;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ExecuteRequestTranslator(handler);
    }

    public class ExecuteRequestTranslator extends TranslatorSupport {
        protected static final String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

        /** xml schema namespace + prefix */
        protected static final String XSI_PREFIX = "xsi";

        protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

        public ExecuteRequestTranslator(ContentHandler ch) {
            super(ch, null, null);
        }

        public void encode(Object o) throws IllegalArgumentException {
            GetCoverageRequest request = (GetCoverageRequest) o;
            encode(request);
        }

        private void encode(GetCoverageRequest request) {
            AttributesImpl attributes =
                    attributes(
                            "version",
                            "1.1.1",
                            "service",
                            "WCS",
                            "xmlns:xsi",
                            XSI_URI,
                            "xmlns",
                            WCS_URI,
                            "xmlns:ows",
                            OWS.NAMESPACE,
                            "xmlns:gml",
                            GML.NAMESPACE,
                            "xmlns:ogc",
                            OGC.NAMESPACE,
                            "xsi:schemaLocation",
                            WCS_URI + " " + "http://schemas.opengis.net/wcs/1.1.1/wcsAll.xsd");

            start("GetCoverage", attributes);
            element("ows:Identifier", request.coverage);
            CoverageInfo coverage = catalog.getCoverageByName(request.coverage);
            start("DomainSubset");
            handleSpatialSubset(request, coverage);
            end("DomainSubset");
            handleOutput(request);
            end("GetCoverage");
        }

        private void handleOutput(GetCoverageRequest request) {
            String format = request.outputFormat;
            final CoverageResponseDelegate encoder = responseFactory.encoderFor(format);
            String mime = encoder.getMimeType(request.outputFormat);
            start("Output", attributes("store", "true", "format", mime));
            if (request.targetCRS != null) {
                start("GridCRS");
                element("GridBaseCRS", epsgUrnCode(request.targetCRS));
                AffineTransform at = request.targetGridToWorld;
                if (at.getTranslateX() == 0
                        && at.getTranslateY() == 0
                        && at.getShearX() == 0
                        && at.getShearY() == 0) {
                    // simple grid mode
                    element("GridType", "urn:ogc:def:method:WCS:1.1:2dSimpleGrid");
                    element("GridOffsets", at.getScaleX() + " " + at.getScaleY());
                } else {
                    element("GridType", "urn:ogc:def:method:WCS:1.1:2dGridIn2dCrs");
                    element("GridOrigin", at.getTranslateX() + " " + at.getTranslateY());
                    element(
                            "GridOffsets",
                            at.getScaleX()
                                    + " "
                                    + at.getShearX()
                                    + " "
                                    + at.getShearY()
                                    + " "
                                    + at.getScaleY());
                }
                element("GridCS", "urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS");
                end("GridCRS");
            }
            end("Output");
        }

        void handleSpatialSubset(GetCoverageRequest request, CoverageInfo coverage) {
            try {
                ReferencedEnvelope bounds = request.bounds;
                CoordinateReferenceSystem boundsCrs = bounds.getCoordinateReferenceSystem();
                final String epsgCode = epsgUrnCode(boundsCrs);
                bounds = bounds.transform(CRS.decode(epsgCode), true);
                start("ows:BoundingBox", attributes("crs", epsgCode));
                element("ows:LowerCorner", bounds.getMinX() + " " + bounds.getMinY());
                element("ows:UpperCorner", bounds.getMaxX() + " " + bounds.getMaxY());
                end("ows:BoundingBox");
            } catch (Exception e) {
                // should never happen, but anyways
                throw new RuntimeException(e);
            }
        }

        private String epsgUrnCode(CoordinateReferenceSystem boundsCrs) {
            try {
                int epsg = CRS.lookupEpsgCode(boundsCrs, false);
                return "urn:ogc:def:crs:EPSG::" + epsg;
            } catch (Exception e) {
                // should never happen, but anyways
                throw new RuntimeException(e);
            }
        }

        /** Helper to build a set of attributes out of a list of key/value pairs */
        AttributesImpl attributes(String... nameValues) {
            AttributesImpl atts = new AttributesImpl();

            for (int i = 0; i < nameValues.length; i += 2) {
                String name = nameValues[i];
                String valu = nameValues[i + 1];

                atts.addAttribute(null, null, name, null, valu);
            }

            return atts;
        }
    }
}
