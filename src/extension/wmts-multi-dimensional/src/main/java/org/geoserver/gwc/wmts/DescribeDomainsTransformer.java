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

package org.geoserver.gwc.wmts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.wms.WMS;
import org.geotools.data.Query;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/** XML transformer for the describe domains operation. */
class DescribeDomainsTransformer extends TransformerBase {

    public DescribeDomainsTransformer(WMS wms) {
        setIndentation(2);
        setEncoding(wms.getCharSet());
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new TranslatorSupport(handler);
    }

    class TranslatorSupport extends TransformerBase.TranslatorSupport {

        public TranslatorSupport(ContentHandler handler) {
            super(handler, null, null);
        }

        @Override
        public void encode(Object object) throws IllegalArgumentException {
            if (!(object instanceof Domains)) {
                throw new IllegalArgumentException(
                        "Expected domains info but instead got: "
                                + object.getClass().getCanonicalName());
            }
            Domains domains = (Domains) object;
            Attributes attributes =
                    createAttributes(
                            new String[] {
                                "xmlns",
                                "http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd",
                                "xmlns:ows",
                                "http://www.opengis.net/ows/1.1",
                                "version",
                                "1.1"
                            });
            start("Domains", attributes);
            Map<String, Tuple<Integer, List<String>>> domainsValues = new HashMap<>();
            domains.getDimensions()
                    .forEach(
                            dimension -> {
                                Tuple<Integer, List<String>> dimensionValues =
                                        dimension.getDomainValuesAsStrings(
                                                new Query(null, domains.getFilter()),
                                                domains.getExpandLimit());
                                domainsValues.put(dimension.getDimensionName(), dimensionValues);
                            });
            if (domains.getSpatialDomain() != null && !domains.getSpatialDomain().isEmpty()) {
                handleBoundingBox(domains.getSpatialDomain());
            }
            domainsValues
                    .entrySet()
                    .forEach(
                            dimensionValues ->
                                    handleDimension(
                                            dimensionValues.getKey(), dimensionValues.getValue()));
            end("Domains");
        }

        private void handleBoundingBox(ReferencedEnvelope boundingBox) {
            if (boundingBox == null) {
                return;
            }
            start("SpaceDomain");
            CoordinateReferenceSystem crs = boundingBox.getCoordinateReferenceSystem();
            Attributes attributes =
                    createAttributes(
                            new String[] {
                                "CRS", crs == null ? "EPSG:4326" : CRS.toSRS(crs),
                                "minx", String.valueOf(boundingBox.getMinX()),
                                "miny", String.valueOf(boundingBox.getMinY()),
                                "maxx", String.valueOf(boundingBox.getMaxX()),
                                "maxy", String.valueOf(boundingBox.getMaxY()),
                            });
            element("BoundingBox", "", attributes);
            end("SpaceDomain");
        }

        private void handleDimension(
                String dimensionName, Tuple<Integer, List<String>> domainsValuesAsStrings) {
            start("DimensionDomain");
            element("ows:Identifier", dimensionName);
            element(
                    "Domain",
                    domainsValuesAsStrings.second.stream().collect(Collectors.joining(",")));
            element("Size", String.valueOf(domainsValuesAsStrings.first));
            end("DimensionDomain");
        }
    }
}
