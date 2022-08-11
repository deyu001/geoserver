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

package org.geoserver.ogcapi.features;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.Link;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** A subclass of GeoJSONGetFeatureResponse that encodes a RFC compliant document */
@Component
public class RFCGeoJSONFeaturesResponse extends GeoJSONGetFeatureResponse {

    /** The MIME type requested by WFS3 for GeoJSON Responses */
    public static final String MIME = "application/geo+json";

    public RFCGeoJSONFeaturesResponse(GeoServer gs) {
        super(gs, MIME);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // was it a single feature request?
        if (getItemId() != null) {
            writeSingleFeature((FeatureCollectionResponse) value, output, operation);
        } else {
            super.write(value, output, operation);
        }
    }

    /** Returns the WFS3 featureId, or null if it's missing or the request is not a WFS3 one */
    private String getItemId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(
                        att ->
                                (String)
                                        att.getAttribute(
                                                FeatureService.ITEM_ID,
                                                RequestAttributes.SCOPE_REQUEST))
                .orElse(null);
    }

    /** Writes a single feature using the facilities provided by the base class */
    private void writeSingleFeature(
            FeatureCollectionResponse value, OutputStream output, Operation operation)
            throws IOException {
        OutputStreamWriter osw =
                new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset());
        try (BufferedWriter outWriter = new BufferedWriter(osw)) {
            FeatureCollectionResponse featureCollection = value;
            boolean isComplex = isComplexFeature(featureCollection);
            GeoJSONBuilder jsonWriter = getGeoJSONBuilder(featureCollection, outWriter);
            writeFeatures(featureCollection.getFeatures(), operation, isComplex, jsonWriter);
            outWriter.flush();
        }
    }

    @Override
    protected void writeExtraFeatureProperties(
            Feature feature, Operation operation, GeoJSONBuilder jw) {
        String featureId = getItemId();
        if (featureId != null) {
            writeLinks(null, operation, jw, featureId);
        }
    }

    @Override
    protected void writePagingLinks(
            FeatureCollectionResponse response, Operation operation, GeoJSONBuilder jw) {
        // we have more than just paging links here
        writeLinks(response, operation, jw, null);
    }

    /**
     * Subclasses can override to adapt links to another service
     *
     * @param response
     * @param operation
     * @param jw
     * @param featureId
     */
    protected void writeLinks(
            FeatureCollectionResponse response,
            Operation operation,
            GeoJSONBuilder jw,
            String featureId) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        FeatureTypeInfo featureType = getFeatureType(request);
        String baseUrl = request.getBaseUrl();
        jw.key("links");
        jw.array();
        // paging links
        if (response != null) {
            if (response.getPrevious() != null) {
                writeLink(jw, "Previous page", MIME, "prev", response.getPrevious());
            }
            if (response.getNext() != null) {
                writeLink(jw, "Next page", MIME, "next", response.getNext());
            }
        }
        // alternate/self links
        String basePath =
                "ogc/features/collections/" + ResponseUtils.urlEncode(featureType.prefixedName());
        Collection<MediaType> formats =
                requestInfo.getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : formats) {
            String path = basePath + "/items";
            if (featureId != null) {
                path += "/" + ResponseUtils.urlEncode(featureId);
            }
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            path,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document as " + format;
            if (format.toString().equals(MIME)) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        // backpointer to the collection
        for (MediaType format :
                requestInfo.getProducibleMediaTypes(CollectionDocument.class, true)) {
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            basePath,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        jw.endArray();
    }

    protected FeatureTypeInfo getFeatureType(GetFeatureRequest request) {
        // a WFS3 always has a collection reference, so one query
        return Optional.ofNullable(request.getQueries())
                .filter(qs -> qs.size() > 0)
                .map(qs -> qs.get(0))
                .map(q -> q.getTypeNames())
                .filter(tns -> tns.size() > 0)
                .map(tns -> tns.get(0))
                .map(tn -> new NameImpl(tn.getNamespaceURI(), tn.getLocalPart()))
                .map(tn -> gs.getCatalog().getFeatureTypeByName(tn))
                .orElse(null);
    }

    @Override
    protected void writeCollectionCRS(GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs)
            throws IOException {
        // write the CRS block only if needed
        if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            super.writeCollectionCRS(jsonWriter, crs);
        }
    }

    protected void writeCollectionCounts(
            BigInteger featureCount, long numberReturned, GeoJSONBuilder jsonWriter) {
        // counts
        if (featureCount != null) {
            jsonWriter.key("numberMatched").value(featureCount);
        }
        jsonWriter.key("numberReturned").value(numberReturned);
    }

    @Override
    protected void writeCollectionBounds(
            boolean featureBounding,
            GeoJSONBuilder jsonWriter,
            List<FeatureCollection> resultsList,
            boolean hasGeom) {
        // not needed in WFS3
    }

    /** capabilities output format string. */
    public String getCapabilitiesElementName() {
        return "GeoJSON-RFC";
    }

    @Override
    public boolean canHandle(Operation operation) {
        String operationId = operation.getId();
        if ("GetFeatures".equalsIgnoreCase(operationId)
                || "GetFeature".equalsIgnoreCase(operationId)
                || "GetFeatureWithLock".equalsIgnoreCase(operationId)
                || "getTile".equalsIgnoreCase(operationId)) {
            return operation.getService() != null
                    && "Features".equals(operation.getService().getId());
        }
        return false;
    }
}
