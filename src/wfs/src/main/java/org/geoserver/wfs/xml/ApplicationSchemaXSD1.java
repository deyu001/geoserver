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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDImport;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.impl.XSDSchemaImpl;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.gml3.GML;
import org.geotools.xsd.XSD;
import org.opengis.feature.simple.SimpleFeatureType;

public class ApplicationSchemaXSD1 extends XSD {

    FeatureTypeSchemaBuilder schemaBuilder;

    Map<String, Set<FeatureTypeInfo>> featureTypes;
    String baseURL;

    public ApplicationSchemaXSD1(FeatureTypeSchemaBuilder schemaBuilder) {
        this(schemaBuilder, Collections.emptyMap());
    }

    public ApplicationSchemaXSD1(
            FeatureTypeSchemaBuilder schemaBuilder,
            Map<String, Set<FeatureTypeInfo>> featureTypes) {
        this.schemaBuilder = schemaBuilder;
        this.featureTypes = featureTypes;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setResources(Map<String, Set<ResourceInfo>> resources) {
        Map<String, Set<FeatureTypeInfo>> featureTypes = new HashMap<>();
        for (Map.Entry<String, Set<ResourceInfo>> entry : resources.entrySet()) {
            Set<FeatureTypeInfo> fts = new HashSet<>();
            for (ResourceInfo ri : entry.getValue()) {
                if (ri instanceof FeatureTypeInfo) {
                    fts.add((FeatureTypeInfo) ri);
                }
            }

            if (!fts.isEmpty()) {
                featureTypes.put(entry.getKey(), fts);
            }
        }
        this.featureTypes = featureTypes;
    }

    public Map<String, Set<FeatureTypeInfo>> getFeatureTypes() {
        return featureTypes;
    }

    @Override
    public String getNamespaceURI() {
        if (featureTypes.size() == 1) {
            return featureTypes.keySet().iterator().next();
        }

        // TODO: return xsd namespace?
        return null;
    }

    @Override
    public String getSchemaLocation() {
        StringBuilder sb = new StringBuilder();
        for (Set<FeatureTypeInfo> fts : featureTypes.values()) {
            for (FeatureTypeInfo ft : fts) {
                sb.append(ft.prefixedName()).append(",");
            }
        }
        sb.setLength(sb.length() - 1);

        HashMap<String, String> kvp = new HashMap<>();
        kvp.putAll(schemaBuilder.getDescribeFeatureTypeParams());
        kvp.put("typename", sb.toString());

        return ResponseUtils.buildURL(baseURL, "wfs", kvp, URLType.SERVICE);
    }

    @Override
    protected XSDSchema buildSchema() throws IOException {
        FeatureTypeInfo[] types =
                this.featureTypes
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .toArray(FeatureTypeInfo[]::new);
        XSDSchema schema;
        if (containsComplexTypes(types)) {
            // we have complex features so we add all the available catalog feature types
            schema = schemaBuilder.build(new FeatureTypeInfo[0], baseURL, true, true);
            schemaBuilder.addApplicationTypes(schema);
        } else {
            // simple feature so we add only the feature types we need
            schema = schemaBuilder.build(types, baseURL, true, true);
        }
        // add an explicit dependency on WFS 1.0.0 schema
        return importWfsSchema(schema);
    }

    /** Checks if the provided feature types contains complex types. */
    private static boolean containsComplexTypes(FeatureTypeInfo[] featureTypes) {
        for (FeatureTypeInfo featureType : featureTypes) {
            try {
                if (!(featureType.getFeatureType() instanceof SimpleFeatureType)) {
                    return true;
                }
            } catch (Exception exception) {
                // ignore the broken feature type
            }
        }
        return false;
    }

    /** Imports the WFS 1.0.0 schema as a dependency. */
    private static XSDSchema importWfsSchema(XSDSchema schema) throws IOException {
        XSDSchema wfsSchema = org.geotools.wfs.v1_1.WFS.getInstance().getSchema();
        if (wfsSchema == null || !(wfsSchema instanceof XSDSchemaImpl)) {
            return schema;
        }
        XSDImport wfsImport = XSDFactory.eINSTANCE.createXSDImport();
        wfsImport.setNamespace(org.geotools.wfs.v1_1.WFS.NAMESPACE);
        wfsImport.setResolvedSchema(wfsSchema);
        schema.getContents().add(wfsImport);
        schema.getQNamePrefixToNamespaceMap().put("wfs", org.geotools.wfs.v1_1.WFS.NAMESPACE);
        synchronized (wfsSchema.eAdapters()) {
            ((XSDSchemaImpl) wfsSchema).imported(wfsImport);
        }
        // make sure that GML 3.1 namespace is used
        schema.getQNamePrefixToNamespaceMap().put("gml", GML.NAMESPACE);
        return schema;
    }
}
