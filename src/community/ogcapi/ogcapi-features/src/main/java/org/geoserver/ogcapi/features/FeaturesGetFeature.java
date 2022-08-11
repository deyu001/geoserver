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

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.util.logging.Logging;

/** A subclass of GetFeature that builds proper API Feature nex/prev links */
public class FeaturesGetFeature extends org.geoserver.wfs.GetFeature {

    static final Logger LOGGER = Logging.getLogger(FeaturesGetFeature.class);

    public FeaturesGetFeature(WFSInfo wfs, Catalog catalog) {
        super(wfs, catalog);
    }

    @Override
    protected void buildPrevNextLinks(
            GetFeatureRequest request,
            int offset,
            int maxFeatures,
            int count,
            FeatureCollectionResponse result,
            Map<String, String> kvp) {
        // can we build the links?
        List<Query> queries = request.getQueries();
        if (queries == null
                || queries.size() != 1
                || queries.get(0).getTypeNames() == null
                || queries.get(0).getTypeNames().size() != 1) {
            LOGGER.log(
                    Level.INFO,
                    "Cannot build prev/next links, the the target typename is not known (or multiple type names available)");
            return;
        }
        QName typeName = queries.get(0).getTypeNames().get(0);
        FeatureTypeInfo typeInfo =
                getCatalog()
                        .getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
        if (typeInfo == null) {
            LOGGER.log(
                    Level.INFO,
                    "Cannot build prev/next links, the the target typename was not found: "
                            + typeName);
            return;
        }
        String collectionName = typeInfo.prefixedName();
        String itemsPath = getItemsPath(collectionName);

        // in WFS3 params are normally lowercase (and are case sensitive)...
        // TODO: we might need a list of parameters and their "normalized case" for WFS3, we'll
        // wait for the filtering/crs extensions to show up before deciding exactly what exactly to
        // do
        kvp = APIRequestInfo.get().getSimpleQueryMap();
        // build prev link if needed
        if (offset > 0) {
            // previous offset calculated as the current offset - maxFeatures, or 0 if this is a
            // negative value, while  previous count should be current offset - previousOffset
            int prevOffset = Math.max(offset - maxFeatures, 0);
            kvp.put("startIndex", String.valueOf(prevOffset));
            kvp.put("limit", String.valueOf(offset - prevOffset));
            result.setPrevious(buildURL(itemsPath, kvp));
        }

        // build next link if needed
        if (count > 0 && offset > -1 && maxFeatures <= count) {
            kvp.put("startIndex", String.valueOf(offset > 0 ? offset + count : count));
            kvp.put("limit", String.valueOf(maxFeatures));
            result.setNext(buildURL(itemsPath, kvp));
        }
    }

    protected String getItemsPath(String collectionName) {
        return "ogc/features/collections/" + ResponseUtils.urlEncode(collectionName) + "/items";
    }

    private String buildURL(String itemsPath, Map<String, String> kvp) {
        return ResponseUtils.buildURL(
                APIRequestInfo.get().getBaseURL(), itemsPath, kvp, URLType.SERVICE);
    }
}
