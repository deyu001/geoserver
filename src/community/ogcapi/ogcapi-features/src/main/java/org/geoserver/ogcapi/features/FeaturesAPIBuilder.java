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

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.wfs.WFSInfo;

/** Builds the OGC Features OpenAPI document */
public class FeaturesAPIBuilder extends org.geoserver.ogcapi.OpenAPIBuilder<WFSInfo> {

    public FeaturesAPIBuilder() {
        super(FeaturesAPIBuilder.class, "openapi.yaml", "Features 1.0 server", "ogc/features");
    }

    /**
     * Build the document based on request, current WFS configuration, and list of available
     * extensions
     *
     * @param wfs The WFS configuration
     */
    public OpenAPI build(WFSInfo wfs) throws IOException {
        OpenAPI api = super.build(wfs);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("WFS specification")
                        .url("https://github.com/opengeospatial/WFS_FES"));

        // adjust path output formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        declareGetResponseFormats(api, "/collections", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}/items", FeaturesResponse.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items/{featureId}", FeaturesResponse.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        Catalog catalog = wfs.getGeoServer().getCatalog();
        List<String> validCollectionIds =
                catalog.getFeatureTypes()
                        .stream()
                        .map(ft -> ft.prefixedName())
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        // provide actual values for limit
        Parameter limit = parameters.get("limit");
        BigDecimal limitMax;
        if (wfs.getMaxFeatures() > 0) {
            limitMax = BigDecimal.valueOf(wfs.getMaxFeatures());
        } else {
            limitMax = BigDecimal.valueOf(Integer.MAX_VALUE);
        }
        limit.getSchema().setMaximum(limitMax);
        // for the moment we don't have a setting for the default, keep it same as max
        limit.getSchema().setDefault(limitMax);

        return api;
    }
}
