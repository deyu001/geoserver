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

package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Provides access to OpenSearch for EO collections and products as an extension of {@link
 * DataAccess} with well known feature types
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OpenSearchAccess extends DataAccess<FeatureType, Feature> {

    public static String EO_NAMESPACE = "http://a9.com/-/opensearch/extensions/eo/1.0/";

    public static String GEO_NAMESPACE = "http://a9.com/-/opensearch/extensions/geo/1.0/";

    /** Internal attribute pointing to the original package location */
    public static String ORIGINAL_PACKAGE_LOCATION = "originalPackageLocation";

    /** Internal attribute stating he original package mime type */
    public static String ORIGINAL_PACKAGE_TYPE = "originalPackageType";

    /** The optional property in collection and product containing the metadata (ISO or O&M) */
    public static Name METADATA_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "metadata");

    /**
     * The optional property in collection and product containing the OGC links (it's a collection)
     */
    public static Name OGC_LINKS_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "ogcLinks");

    /** The optional property in product containing the quicklook */
    public static Name QUICKLOOK_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "quicklook");

    /**
     * Local part of the optional collection property containing the layers publishing information
     */
    public static String LAYERS = "layers";

    /** The optional property in collection containing the published layers */
    public static Name LAYERS_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, LAYERS);

    /**
     * Local part of the HTML description property. The namespace is the one assigned to the store,
     * this is not an EO property
     */
    public static String DESCRIPTION = "htmlDescription";

    /**
     * Local part of the product granules property. The namespace is the one assigned to the store,
     * this is not an EO property
     */
    public static String GRANULES = "granules";

    /**
     * Just like in WCS 2.0, setting up a separator that's unlikely to be found in the wild, since
     * there is no option that's absolutely unique
     */
    String BAND_LAYER_SEPARATOR = "__";

    /**
     * Returns the feature source backing collections (dynamic, as the store has to respect the
     * namespace URI given by GeoServer)
     */
    FeatureSource<FeatureType, Feature> getCollectionSource() throws IOException;

    /**
     * Returns the feature source backing products (dynamic, as the store has to respect the
     * namespace URI given by GeoServer)
     */
    FeatureSource<FeatureType, Feature> getProductSource() throws IOException;

    /** Returns a feature source to access the granules of a particular product */
    SimpleFeatureSource getGranules(String collectionId, String productId) throws IOException;

    SimpleFeatureType getCollectionLayerSchema() throws IOException;

    SimpleFeatureType getOGCLinksSchema() throws IOException;
}
