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

package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.OWS20Exception;
import org.geotools.data.DataAccess;

/**
 * Helper object returning the configured {@link OpenSearchAccess}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OpenSearchAccessProvider {

    private GeoServer geoServer;

    public OpenSearchAccessProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public OSEOInfo getService() {
        return this.geoServer.getService(OSEOInfo.class);
    }

    /**
     * Returns the OpenSearchAccess configured in {@link OSEOInfo}, or throws a service exception in
     * case of mis-configuration
     */
    public OpenSearchAccess getOpenSearchAccess() throws IOException {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        DataStoreInfo dataStore = getDataStoreInfo();

        DataAccess result = dataStore.getDataStore(null);
        if (result == null) {
            throw new OWS20Exception(
                    "Failed to locate OpenSearch data access with identifier "
                            + openSearchAccessStoreId
                            + ", please correct the configuration in the OpenSearch for EO panel");
        } else if (!(result instanceof OpenSearchAccess)) {
            throw new OWS20Exception(
                    "Data access with identifier "
                            + openSearchAccessStoreId
                            + " does not point to a valid OpenSearchDataAccess, "
                            + "please correct the configuration in the OpenSearch for EO panel, "
                            + "but got instead an istance of "
                            + result.getClass()
                            + "\n. ToString follows: "
                            + result);
        }

        return (OpenSearchAccess) result;
    }

    /** Returns the configuration of the store backing the OpenSearch subsystem */
    public DataStoreInfo getDataStoreInfo() {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        if (openSearchAccessStoreId == null) {
            throw new OWS20Exception(
                    "OpenSearchAccess is not configured in the"
                            + " OpenSearch for EO panel, please do so");
        }
        DataStoreInfo dataStore = this.geoServer.getCatalog().getDataStore(openSearchAccessStoreId);

        return dataStore;
    }

    /** List the configured product classes */
    public List<ProductClass> getProductClasses() {
        return ProductClass.getProductClasses(getService());
    }
}
