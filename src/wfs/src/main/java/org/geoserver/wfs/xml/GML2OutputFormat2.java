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

package org.geoserver.wfs.xml;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.response.ComplexFeatureAwareFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;

public class GML2OutputFormat2 extends WFSGetFeatureOutputFormat
        implements ComplexFeatureAwareFormat {

    Catalog catalog;
    GeoServerResourceLoader resourceLoader;

    public GML2OutputFormat2(GeoServer gs) {
        super(gs, new HashSet<>(Arrays.asList("gml2", "text/xml; subtype=gml/2.1.2")));

        this.catalog = gs.getCatalog();
        this.resourceLoader = catalog.getResourceLoader();
    }

    public String getMimeType(Object value, Operation operation) {
        return "text/xml; subtype=gml/2.1.2";
    }

    public String getCapabilitiesElementName() {
        return "GML2";
    }

    protected void write(
            FeatureCollectionResponse results, OutputStream output, Operation getFeature)
            throws ServiceException, IOException {

        // declare wfs schema location
        GetFeatureRequest gft = GetFeatureRequest.adapt(getFeature.getParameters()[0]);

        List featureCollections = results.getFeature();

        // round up the info objects for each feature collection
        MultiValuedMap<NamespaceInfo, FeatureTypeInfo> ns2metas = new HashSetValuedHashMap<>();

        for (Object featureCollection : featureCollections) {
            SimpleFeatureCollection features = (SimpleFeatureCollection) featureCollection;
            SimpleFeatureType featureType = features.getSchema();

            // load the metadata for the feature type
            String namespaceURI = featureType.getName().getNamespaceURI();
            FeatureTypeInfo meta =
                    catalog.getFeatureTypeByName(namespaceURI, featureType.getTypeName());
            if (meta == null)
                throw new WFSException(
                        gft,
                        "Could not find feature type "
                                + namespaceURI
                                + ":"
                                + featureType.getTypeName()
                                + " in the GeoServer catalog");

            NamespaceInfo ns = catalog.getNamespaceByURI(namespaceURI);
            ns2metas.put(ns, meta);
        }

        Collection<FeatureTypeInfo> featureTypes = ns2metas.values();

        // create the encoder
        ApplicationSchemaXSD xsd =
                new ApplicationSchemaXSD(
                        null,
                        catalog,
                        gft.getBaseUrl(),
                        org.geotools.wfs.v1_0.WFS.getInstance(),
                        featureTypes);
        Configuration configuration =
                new ApplicationSchemaConfiguration(
                        xsd, new org.geotools.wfs.v1_0.WFSConfiguration_1_0());

        Encoder encoder = new Encoder(configuration);
        // encoder.setEncoding(wfs.getCharSet());

        encoder.setSchemaLocation(
                org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                buildSchemaURL(gft.getBaseUrl(), "wfs/1.0.0/WFS-basic.xsd"));

        // declare application schema namespaces
        Map<String, String> params =
                params("service", "WFS", "version", "1.0.0", "request", "DescribeFeatureType");
        for (MapIterator i = ns2metas.mapIterator(); i.hasNext(); ) {
            NamespaceInfo ns = (NamespaceInfo) i.next();
            String namespaceURI = ns.getURI();
            Collection metas = (Collection) i.getValue();

            StringBuffer typeNames = new StringBuffer();

            for (Iterator m = metas.iterator(); m.hasNext(); ) {
                FeatureTypeInfo meta = (FeatureTypeInfo) m.next();
                typeNames.append(meta.prefixedName());

                if (m.hasNext()) {
                    typeNames.append(",");
                }
            }

            // set the schema location
            params.put("typeName", typeNames.toString());
            encoder.setSchemaLocation(
                    namespaceURI, buildURL(gft.getBaseUrl(), "wfs", params, URLType.RESOURCE));
        }

        encoder.encode(results.getAdaptee(), org.geotools.wfs.v1_0.WFS.FeatureCollection, output);
    }

    @Override
    public boolean supportsComplexFeatures(Object value, Operation operation) {
        return true;
    }
}
