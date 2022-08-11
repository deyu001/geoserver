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

package org.geoserver.ogcapi.dggs;

import static org.geoserver.ows.URLMangler.URLType.SERVICE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.features.FeaturesResponse;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.data.Query;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "dggs-id", "resolutions", "links"})
public class CollectionDocument extends AbstractCollectionDocument<FeatureTypeInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);
    private final DGGSFeatureSource fs;

    FeatureTypeInfo featureType;
    String mapPreviewURL;

    public CollectionDocument(GeoServer geoServer, FeatureTypeInfo featureType) throws IOException {
        super(featureType);
        // basic info
        String collectionId = featureType.prefixedName();
        this.id = collectionId;
        this.title = featureType.getTitle();
        this.description = featureType.getAbstract();
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        setExtent(new CollectionExtents(bbox));
        this.featureType = featureType;

        String baseUrl = APIRequestInfo.get().getBaseURL();

        // zones links
        Collection<MediaType> zoneFormats =
                APIRequestInfo.get().getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : zoneFormats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "ogc/dggs/collections/" + collectionId + "/zones",
                            Collections.singletonMap("f", format.toString()),
                            SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            "zones",
                            format.toString(),
                            collectionId + " items as " + format.toString(),
                            "zones"));
        }

        // DAPA links, if time is available
        DimensionInfo time = featureType.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time != null) {
            Collection<MediaType> dapaFormats =
                    APIRequestInfo.get().getProducibleMediaTypes(CollectionDAPA.class, true);
            for (MediaType format : dapaFormats) {
                String dapaURL =
                        ResponseUtils.buildURL(
                                baseUrl,
                                "ogc/dggs/collections/" + collectionId + "/processes",
                                Collections.singletonMap("f", format.toString()),
                                SERVICE);
                addLink(
                        new Link(
                                dapaURL,
                                "ogc-dapa-processes",
                                format.toString(),
                                "DAPA for " + collectionId + " as " + format.toString(),
                                "ogc-dapa-processes"));
            }
            Collection<MediaType> variablesFormats =
                    APIRequestInfo.get().getProducibleMediaTypes(DAPAVariables.class, true);
            for (MediaType format : variablesFormats) {
                String variablesURL =
                        ResponseUtils.buildURL(
                                baseUrl,
                                "ogc/dggs/collections/" + collectionId + "/variables",
                                Collections.singletonMap("f", format.toString()),
                                SERVICE);
                addLink(
                        new Link(
                                variablesURL,
                                "ogc-dapa-variables",
                                format.toString(),
                                "DAPA variables for " + collectionId + " as " + format.toString(),
                                "ogc-dapa-variables"));
            }
        }

        addSelfLinks("ogc/dggs/collections/" + id);

        // map preview if available
        if (isWMSAvailable(geoServer)) {
            Map<String, String> kvp = new HashMap<>();
            kvp.put("LAYERS", featureType.prefixedName());
            kvp.put("FORMAT", "application/openlayers");
            this.mapPreviewURL = ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp, SERVICE);
        }

        // setup resolutions
        DGGSStore dggsStore = (DGGSStore) featureType.getStore().getDataStore(null);
        this.fs = dggsStore.getDGGSFeatureSource(featureType.getNativeName());
    }

    private boolean isWMSAvailable(GeoServer geoServer) {
        ServiceInfo si =
                geoServer
                        .getServices()
                        .stream()
                        .filter(s -> "WMS".equals(s.getId()))
                        .findFirst()
                        .orElse(null);
        return si != null;
    }

    @JsonIgnore
    public FeatureType getSchema() {
        try {
            return featureType.getFeatureType();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Failed to compute feature type", e);
            return null;
        }
    }

    @JsonIgnore
    public String getMapPreviewURL() {
        return mapPreviewURL;
    }

    public int[] getResolutions() throws IOException {
        UniqueVisitor visitor = new UniqueVisitor(DGGSStore.RESOLUTION);
        fs.getFeatures(Query.ALL).accepts(visitor, null);
        int[] resolutions =
                visitor.getResult().toList().stream().mapToInt(v -> (Integer) v).toArray();
        return resolutions;
    }

    @JsonProperty("dggs-id")
    public String getDggsId() {
        return fs.getDGGS().getIdentifier();
    }
}
