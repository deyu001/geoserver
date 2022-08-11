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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.DefaultWebFeatureService30;
import org.geoserver.wfs3.NCNameResourceCodec;
import org.geoserver.wfs3.WFS3Extension;
import org.opengis.filter.Filter;

/**
 * A class representing the WFS3 server "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JacksonXmlRootElement(localName = "Collections")
@JsonPropertyOrder({"links", "links", "collections"})
public class CollectionsDocument extends AbstractDocument {

    private final BaseRequest request;
    private final FeatureTypeInfo featureType;
    private final GeoServer geoServer;
    private final List<WFS3Extension> extensions;

    public CollectionsDocument(
            BaseRequest request, GeoServer geoServer, List<WFS3Extension> extensions) {
        this(request, geoServer, null, extensions);
    }

    public CollectionsDocument(
            BaseRequest request,
            GeoServer geoServer,
            FeatureTypeInfo featureType,
            List<WFS3Extension> extensions) {
        this.geoServer = geoServer;
        this.request = request;
        this.featureType = featureType;
        this.extensions = extensions;

        // build the links
        List<String> formats =
                DefaultWebFeatureService30.getAvailableFormats(CollectionsDocument.class);
        String baseUrl = request.getBaseUrl();
        for (String format : formats) {
            String path =
                    "wfs3/collections/"
                            + (featureType != null ? NCNameResourceCodec.encode(featureType) : "");
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            path,
                            Collections.singletonMap("f", format),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document " + " as " + format;
            String outputFormat = request.getOutputFormat();
            if (format.equals(outputFormat)
                    || (outputFormat == null && format.equals(BaseRequest.JSON_MIME))) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            links.add(new Link(apiUrl, linkType, format, linkTitle));
        }
    }

    @JacksonXmlProperty(localName = "Links")
    public List<Link> getLinks() {
        return links;
    }

    @JacksonXmlProperty(localName = "Collection")
    public Iterator<CollectionDocument> getCollections() {
        // single collection case
        if (featureType != null) {
            CollectionDocument document = new CollectionDocument(geoServer, request, featureType);
            decorateWithExtensions(document);
            return Collections.singleton(document).iterator();
        }

        // full scan case
        CloseableIterator<FeatureTypeInfo> featureTypes =
                geoServer.getCatalog().list(FeatureTypeInfo.class, Filter.INCLUDE);
        return new Iterator<CollectionDocument>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                boolean hasNext = featureTypes.hasNext();
                if (!hasNext) {
                    featureTypes.close();
                    return false;
                } else {
                    try {
                        FeatureTypeInfo featureType = featureTypes.next();
                        CollectionDocument collection =
                                new CollectionDocument(geoServer, request, featureType);
                        decorateWithExtensions(collection);

                        next = collection;
                        return true;
                    } catch (Exception e) {
                        featureTypes.close();
                        throw new ServiceException(
                                "Failed to iterate over the feature types in the catalog", e);
                    }
                }
            }

            @Override
            public CollectionDocument next() {
                CollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
    }

    private void decorateWithExtensions(CollectionDocument collection) {
        if (extensions != null) {
            for (WFS3Extension extension : extensions) {
                extension.extendCollection(collection, request);
            }
        }
    }
}
