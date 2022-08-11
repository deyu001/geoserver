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

package org.geoserver.wfs.xml.v1_1_0;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDResourceImpl;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSDescribeFeatureTypeOutputFormat;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;

public class XmlSchemaEncoder extends WFSDescribeFeatureTypeOutputFormat {

    /** the catalog */
    Catalog catalog;

    /** the geoserver resource loader */
    GeoServerResourceLoader resourceLoader;

    /** schema builder */
    FeatureTypeSchemaBuilder schemaBuilder;

    public XmlSchemaEncoder(String mimeType, GeoServer gs, FeatureTypeSchemaBuilder schemaBuilder) {
        super(gs, mimeType);

        this.catalog = gs.getCatalog();
        this.resourceLoader = catalog.getResourceLoader();
        this.schemaBuilder = schemaBuilder;
    }

    public XmlSchemaEncoder(
            Set<String> mimeTypes, GeoServer gs, FeatureTypeSchemaBuilder schemaBuilder) {
        super(gs, mimeTypes);

        this.catalog = gs.getCatalog();
        this.resourceLoader = catalog.getResourceLoader();
        this.schemaBuilder = schemaBuilder;
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next();
        // return "text/xml; subtype=gml/3.1.1";
    }

    protected String getWFSNamespaceURI() {
        return WFS.NAMESPACE;
    }

    protected void write(
            FeatureTypeInfo[] featureTypeInfos, OutputStream output, Operation describeFeatureType)
            throws IOException {

        // hack for SOAP request, when encoding as SOAP response the schema is actually required
        // to be encoded in base64
        if (Dispatcher.REQUEST.get() != null && Dispatcher.REQUEST.get().isSOAP()) {

            output.write(
                    ("<wfs:DescribeFeatureTypeResponse xmlns:wfs='" + getWFSNamespaceURI() + "'>")
                            .getBytes());

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            doWrite(featureTypeInfos, bout, describeFeatureType);
            output.write(Base64.encodeBase64(bout.toByteArray()));

            output.write("</wfs:DescribeFeatureTypeResponse>".getBytes());
        } else {
            // normal write
            doWrite(featureTypeInfos, output, describeFeatureType);
        }
    }

    protected void doWrite(
            FeatureTypeInfo[] featureTypeInfos, OutputStream output, Operation describeFeatureType)
            throws IOException {

        // create the schema
        Object request = describeFeatureType.getParameters()[0];
        DescribeFeatureTypeRequest req = DescribeFeatureTypeRequest.adapt(request);

        XSDSchema schema = schemaBuilder.build(featureTypeInfos, req.getBaseURL());

        // serialize
        schema.updateElement();
        final String encoding = gs.getSettings().getCharset();
        XSDResourceImpl.serialize(output, schema.getElement(), encoding);
    }

    public static class V20 extends XmlSchemaEncoder {
        static Set<String> MIME_TYPES = new LinkedHashSet<>();

        static {
            MIME_TYPES.add("application/gml+xml; version=3.2");
            MIME_TYPES.add("text/xml; subtype=gml/3.2");
        }

        public V20(GeoServer gs) {
            super(MIME_TYPES, gs, new FeatureTypeSchemaBuilder.GML32NoWfsSchemaImport(gs));
        }

        @Override
        protected String getWFSNamespaceURI() {
            return org.geotools.wfs.v2_0.WFS.NAMESPACE;
        }
    }

    public static class V11 extends XmlSchemaEncoder {

        public V11(GeoServer gs) {
            super("text/xml; subtype=gml/3.1.1", gs, new FeatureTypeSchemaBuilder.GML3(gs));
        }
    }

    public static class V10 extends XmlSchemaEncoder {

        public V10(GeoServer gs) {
            super("XMLSCHEMA", gs, new FeatureTypeSchemaBuilder.GML2(gs));
        }

        @Override
        public String getMimeType(Object arg0, Operation arg1) throws ServiceException {
            return "text/xml";
        }
    }
}
