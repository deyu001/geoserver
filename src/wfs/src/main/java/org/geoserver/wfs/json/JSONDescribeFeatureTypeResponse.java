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

package org.geoserver.wfs.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSDescribeFeatureTypeOutputFormat;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * A DescribeFeatureType output format that generates a JSON schema instead of a XML one
 *
 * @author Andrea Aime - GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 */
public class JSONDescribeFeatureTypeResponse extends WFSDescribeFeatureTypeOutputFormat {

    // private final static Logger LOGGER =
    // Logging.getLogger(JSONDescribeFeatureTypeResponse.class);

    public JSONDescribeFeatureTypeResponse(GeoServer gs, final String mime) {
        super(gs, mime);
    }

    @Override
    protected void write(
            FeatureTypeInfo[] featureTypeInfos, OutputStream output, Operation describeFeatureType)
            throws IOException {

        if (featureTypeInfos.length == 0) {
            throw new IOException("Unable to write an empty feature info array.");
        }
        // prepare to write out
        try (OutputStreamWriter osw =
                        new OutputStreamWriter(output, gs.getSettings().getCharset());
                Writer outWriter = new BufferedWriter(osw)) {
            // jsonp?
            final boolean jsonp =
                    JSONType.useJsonp(getMimeType(featureTypeInfos, describeFeatureType));
            if (jsonp) {
                outWriter.write(getCallbackFunction() + "(");
            }

            // starting with JSon
            GeoJSONBuilder jw = new GeoJSONBuilder(outWriter);
            jw.object();
            jw.key("elementFormDefault");
            jw.value("qualified");
            jw.key("targetNamespace");
            NamespaceInfo nsInfo = featureTypeInfos[0].getNamespace();
            jw.value(nsInfo.getURI());
            jw.key("targetPrefix");
            jw.value(nsInfo.getName());
            jw.key("featureTypes");
            // in general one can describe more than one feature type
            jw.array();
            for (FeatureTypeInfo ft : featureTypeInfos) {
                jw.object();
                jw.key("typeName").value(ft.getName());
                SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
                jw.key("properties");
                jw.array();
                for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
                    if (ad == schema.getGeometryDescriptor()) {
                        // this one we already described
                        describeProperty(ad.getLocalName(), ad, jw, true);
                    } else {
                        describeProperty(ad.getLocalName(), ad, jw, false);
                    }
                }
                jw.endArray();
                jw.endObject(); // end of the feature type schema
            }
            jw.endArray();
            jw.endObject();

            // jsonp?
            if (jsonp) {
                outWriter.write(")");
            }
            outWriter.flush();
        }
    }

    private String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        }
        return JSONType.getCallbackFunction(request.getKvp());
    }

    private static void describeProperty(
            String name, AttributeDescriptor ad, GeoJSONBuilder jw, boolean isGeometry) {
        jw.object();
        jw.key("name").value(name);
        jw.key("maxOccurs").value(ad.getMaxOccurs());
        jw.key("minOccurs").value(ad.getMinOccurs());
        jw.key("nillable").value((ad.getMinOccurs() > 0) ? false : true);
        Class<?> binding = ad.getType().getBinding();
        if (isGeometry) {
            jw.key("type").value("gml:" + mapToJsonType(binding));
        } else {
            jw.key("type").value("xsd:" + mapToJsonType(binding));
        }
        jw.key("localType").value(mapToJsonType(binding));

        jw.endObject(); // end of attribute description
    }

    private static String mapToJsonType(Class<?> binding) {
        if (Long.class.isAssignableFrom(binding)
                || Integer.class.isAssignableFrom(binding)
                || Short.class.isAssignableFrom(binding)
                || Byte.class.isAssignableFrom(binding)) {
            return "int";
        } else if (Number.class.isAssignableFrom(binding)) {
            return "number";
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return "boolean";
        } else if (Geometry.class.isAssignableFrom(binding)) {
            return binding.getSimpleName();
        } else if (java.sql.Date.class.isAssignableFrom(binding)) {
            return "date";
        } else if (java.sql.Time.class.isAssignableFrom(binding)) {
            return "time";
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            return "date-time";
        } else {
            return "string";
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next();
    }
}
