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

package org.geoserver.csw.store.internal.iso;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.csw.store.internal.CSWInternalTestSupport;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.filter.v1_1.OGC;
import org.geotools.gml3.v3_2.GML;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.ows.OWS;
import org.junit.BeforeClass;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** @author Niels Charlier */
public class MDTestSupport extends CSWInternalTestSupport {

    @BeforeClass
    public static void configureXMLUnit() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("ows", OWS.NAMESPACE);
        namespaces.put("ogc", OGC.NAMESPACE);
        namespaces.put("xlink", XLINK.NAMESPACE);
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gmd", MetaDataDescriptor.NAMESPACE_GMD);
        namespaces.put("gco", MetaDataDescriptor.NAMESPACE_GCO);
        namespaces.put("dc", DC.NAMESPACE);
        namespaces.put("gmx", MetaDataDescriptor.NAMESPACE_GMX);
        namespaces.put("gfc", MetaDataDescriptor.NAMESPACE_GFC);
        namespaces.put("gml", GML.NAMESPACE);

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    };

    // Lazy Loading.
    private static Validator validator;

    protected static Validator getValidator() {
        if (validator == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema;
            try {
                schema =
                        factory.newSchema(
                                new StreamSource(
                                        MDTestSupport.class
                                                .getResource(
                                                        "/net/opengis/schemas/iso/19139/20070417/gmd/metadataEntity.xsd")
                                                .toString()));
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
            validator = schema.newValidator();
        }
        return validator;
    }

    protected static void validateSchema(NodeList xml) throws SAXException, IOException {

        for (int i = 0; i < xml.getLength(); i++) {
            getValidator().validate(new DOMSource(xml.item(i)));
        }
    }
}
