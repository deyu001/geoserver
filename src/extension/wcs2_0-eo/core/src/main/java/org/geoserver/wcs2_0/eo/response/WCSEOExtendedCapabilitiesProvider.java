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

package org.geoserver.wcs2_0.eo.response;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.util.List;
import net.opengis.wcs20.GetCapabilitiesType;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.eo.EOCoverageResourceCodec;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.wcs2_0.response.WCSDimensionsHelper;
import org.geoserver.wcs2_0.response.WCSExtendedCapabilitiesProvider;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Encodes the extensions to the WCS capabilities document
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCSEOExtendedCapabilitiesProvider extends WCSExtendedCapabilitiesProvider {
    EOCoverageResourceCodec codec;
    GeoServer gs;

    public WCSEOExtendedCapabilitiesProvider(GeoServer gs, EOCoverageResourceCodec codec) {
        this.codec = codec;
        this.gs = gs;
    }

    private boolean isEarthObservationEnabled() {
        WCSInfo wcs = gs.getService(WCSInfo.class);
        Boolean enabled = wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    /** IGN : Do we still need to host this xsd ? */
    public String[] getSchemaLocations(String schemaBaseURL) {
        if (!isEarthObservationEnabled()) {
            return new String[0];
        }
        String schemaLocation =
                ResponseUtils.buildURL(
                        schemaBaseURL,
                        "schemas/wcseo/1.0/wcsEOGetCapabilites.xsd",
                        null,
                        URLType.RESOURCE);
        return new String[] {WCSEOMetadata.NAMESPACE, schemaLocation};
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        if (isEarthObservationEnabled()) {
            namespaces.declarePrefix("wcseo", WCSEOMetadata.NAMESPACE);
        }
    }

    @Override
    public void encodeExtendedOperations(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs,
            GetCapabilitiesType request)
            throws IOException {
        if (!isEarthObservationEnabled()) {
            return;
        }

        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "name", "name", null, "DescribeEOCoverageSet");
        tx.start("ows:Operation", attributes);

        final String url =
                appendQueryString(
                        buildURL(request.getBaseUrl(), "wcs", null, URLMangler.URLType.SERVICE),
                        "");

        tx.start("ows:DCP");
        tx.start("ows:HTTP");
        attributes = new AttributesImpl();
        attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
        element(tx, "ows:Get", null, attributes);
        tx.end("ows:HTTP");
        tx.end("ows:DCP");

        attributes = new AttributesImpl();
        attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
        tx.start("ows:DCP");
        tx.start("ows:HTTP");
        element(tx, "ows:Post", null, attributes);
        tx.end("ows:HTTP");
        tx.end("ows:DCP");

        tx.end("ows:Operation");

        Integer defaultCount =
                wcs.getMetadata().get(WCSEOMetadata.COUNT_DEFAULT.key, Integer.class);
        if (defaultCount != null) {
            tx.start("ows:Constraint", atts("name", "CountDefault"));
            element(tx, "ows:NoValues", null, null);
            element(tx, "ows:DefaultValue", String.valueOf(defaultCount), null);
            tx.end("ows:Constraint");
        }
    }

    @Override
    public void encodeExtendedContents(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs,
            List<CoverageInfo> coverages,
            GetCapabilitiesType request)
            throws IOException {
        Boolean enabled = wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class);
        if (enabled == null || !enabled) {
            return;
        }

        for (CoverageInfo ci : coverages) {
            Boolean dataset = ci.getMetadata().get(WCSEOMetadata.DATASET.key, Boolean.class);
            DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if (dataset != null && dataset && time != null && time.isEnabled()) {
                tx.start("wcseo:DatasetSeriesSummary");
                ReferencedEnvelope bbox = ci.getLatLonBoundingBox();
                tx.start("ows:WGS84BoundingBox");
                element(tx, "ows:LowerCorner", bbox.getMinX() + " " + bbox.getMinY(), null);
                element(tx, "ows:UpperCorner", bbox.getMaxX() + " " + bbox.getMaxY(), null);
                tx.end("ows:WGS84BoundingBox");
                String datasetId = codec.getDatasetName(ci);
                element(tx, "wcseo:DatasetSeriesId", datasetId, null);

                GridCoverage2DReader reader =
                        (GridCoverage2DReader) ci.getGridCoverageReader(null, null);

                WCSDimensionsHelper dimensionsHelper = new WCSDimensionsHelper(time, reader, null);
                tx.start("gml:TimePeriod", atts("gml:id", datasetId + "__timeperiod"));
                element(tx, "gml:beginPosition", dimensionsHelper.getBeginTime(), null);
                element(tx, "gml:endPosition", dimensionsHelper.getEndTime(), null);
                tx.end("gml:TimePeriod");
                tx.end("wcseo:DatasetSeriesSummary");
            }
        }
    }

    private void element(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            String element,
            String content,
            AttributesImpl attributes) {
        tx.start(element, attributes);
        if (content != null) {
            tx.chars(content);
        }
        tx.end(element);
    }

    Attributes atts(String... atts) {
        AttributesImpl attributes = new AttributesImpl();
        for (int i = 0; i < atts.length; i += 2) {
            attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
        }
        return attributes;
    }
}
