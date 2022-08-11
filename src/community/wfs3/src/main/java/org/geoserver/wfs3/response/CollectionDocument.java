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

package org.geoserver.wfs3.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.DefaultWebFeatureService30;
import org.geoserver.wfs3.NCNameResourceCodec;
import org.geoserver.wfs3.WFSExtents;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"name", "title", "description", "extent", "links"})
@JacksonXmlRootElement(localName = "Collection")
public class CollectionDocument extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    String name;
    String title;
    String description;
    WFSExtents extent;
    FeatureTypeInfo featureType;
    String mapPreviewURL;

    public CollectionDocument(
            GeoServer geoServer, BaseRequest request, FeatureTypeInfo featureType) {
        // basic info
        String collectionId = NCNameResourceCodec.encode(featureType);
        setName(collectionId);
        setTitle(featureType.getTitle());
        setDescription(featureType.getAbstract());
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        setExtent(new WFSExtents(bbox));
        this.featureType = featureType;

        // links
        List<String> formats =
                DefaultWebFeatureService30.getAvailableFormats(FeatureCollectionResponse.class);
        String baseUrl = request.getBaseUrl();
        for (String format : formats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "wfs3/collections/" + collectionId + "/items",
                            Collections.singletonMap("f", format),
                            URLMangler.URLType.SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            Link.REL_ITEM,
                            format,
                            collectionId + " items as " + format,
                            "items"));
        }

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
                        .filter(s -> "WMS".equals(s.getName()))
                        .findFirst()
                        .orElse(null);
        return si != null;
    }

    @JacksonXmlProperty(localName = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JacksonXmlProperty(localName = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JacksonXmlProperty(localName = "Description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WFSExtents getExtent() {
        return extent;
    }

    public void setExtent(WFSExtents extent) {
        this.extent = extent;
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> getLinks() {
        return links;
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
}
