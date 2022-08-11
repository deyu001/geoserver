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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.QueryablesDocument;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
@JacksonXmlRootElement(localName = "Collection", namespace = "http://www.opengis.net/wfs/3.0")
public class CollectionDocument extends AbstractCollectionDocument<FeatureTypeInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    FeatureTypeInfo featureType;
    String mapPreviewURL;
    List<String> crs;

    public CollectionDocument(GeoServer geoServer, FeatureTypeInfo featureType, List<String> crs) {
        super(featureType);
        // basic info
        String collectionId = featureType.prefixedName();
        this.id = collectionId;
        this.title = featureType.getTitle();
        this.description = featureType.getAbstract();
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        setExtent(new CollectionExtents(bbox));
        this.featureType = featureType;
        this.crs = crs;

        // links
        Collection<MediaType> formats =
                APIRequestInfo.get().getProducibleMediaTypes(FeaturesResponse.class, true);
        String baseUrl = APIRequestInfo.get().getBaseURL();
        for (MediaType format : formats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "ogc/features/collections/" + collectionId + "/items",
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            Link.REL_ITEMS,
                            format.toString(),
                            collectionId + " items as " + format.toString(),
                            "items"));
        }
        addSelfLinks("ogc/features/collections/" + id);

        // describedBy as GML schema
        String describedByHref =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs",
                        new HashMap<String, String>() {
                            {
                                put("service", "WFS");
                                put("version", "2.0");
                                put("request", "DescribeFeatureType");
                                put("typenames", featureType.prefixedName());
                            }
                        },
                        URLMangler.URLType.SERVICE);
        Link describedBy =
                new Link(describedByHref, "describedBy", "application/xml", "Schema for " + id);
        addLink(describedBy);

        // queryables
        addLinksFor(
                "ogc/features/collections/"
                        + ResponseUtils.urlEncode(featureType.prefixedName())
                        + "/queryables",
                QueryablesDocument.class,
                "Queryable attributes as ",
                "queryables",
                null,
                "queryables");

        // map preview
        if (isWMSAvailable(geoServer)) {
            Map<String, String> kvp = new HashMap<>();
            kvp.put("LAYERS", featureType.prefixedName());
            kvp.put("FORMAT", "application/openlayers");
            this.mapPreviewURL =
                    ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp, URLMangler.URLType.SERVICE);
        }
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

    public List<String> getCrs() {
        return crs;
    }
}
