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

package org.geoserver.ogcapi.stac;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.visitor.UniqueVisitor;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.PropertyName;

/** Builds the OGC Features OpenAPI document */
public class STACAPIBuilder extends org.geoserver.ogcapi.OpenAPIBuilder<OSEOInfo> {

    private final OpenSearchAccessProvider accessProvider;

    public STACAPIBuilder(OpenSearchAccessProvider accessProvider) {
        super(STACAPIBuilder.class, "openapi.yaml", "STAC server", "ogc/stac");
        this.accessProvider = accessProvider;
    }

    /**
     * Build the document based on request, current WFS configuration, and list of available
     * extensions
     *
     * @param service The Opensearch for EO configuration
     */
    public OpenAPI build(OSEOInfo service) throws IOException {
        OpenAPI api = super.build(service);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("STAC API specification")
                        .url("https://github.com/radiantearth/stac-api-spec"));

        // adjust path output formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        // TODO: these needs to be adjusted once we have
        declareGetResponseFormats(api, "/collections", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionsDocument.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items", FeatureCollectionType.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items/{featureId}", FeatureCollectionType.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        Catalog catalog = service.getGeoServer().getCatalog();
        List<String> validCollectionIds = getCollectionIds();
        collectionId.getSchema().setEnum(validCollectionIds);

        // provide actual values for limit
        Parameter limit = parameters.get("limit");
        BigDecimal limitMax;
        if (service.getMaximumRecordsPerPage() > 0) {
            limitMax = BigDecimal.valueOf(service.getMaximumRecordsPerPage());
        } else {
            limitMax = BigDecimal.valueOf(OSEOInfo.DEFAULT_MAXIMUM_RECORDS);
        }
        limit.getSchema().setMaximum(limitMax);
        int recordsPerpage = service.getRecordsPerPage();
        if (recordsPerpage <= 0) recordsPerpage = OSEOInfo.DEFAULT_RECORDS_PER_PAGE;
        limit.getSchema().setDefault(recordsPerpage);

        return api;
    }

    @SuppressWarnings("unchecked")
    private List<String> getCollectionIds() throws IOException {
        FeatureSource<FeatureType, Feature> fs =
                accessProvider.getOpenSearchAccess().getCollectionSource();
        PropertyName name =
                CommonFactoryFinder.getFilterFactory2()
                        .property(new NameImpl(fs.getSchema().getName().getNamespaceURI(), "name"));
        UniqueVisitor visitor = new UniqueVisitor(name);
        fs.getFeatures().accepts(visitor, null);
        Set<Attribute> uniqueValues = visitor.getUnique();
        return uniqueValues
                .stream()
                .map(o -> (String) o.getValue())
                .sorted()
                .collect(Collectors.toList());
    }
}
