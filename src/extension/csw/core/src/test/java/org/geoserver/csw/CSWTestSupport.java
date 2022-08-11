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

package org.geoserver.csw;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.dom.DOMSource;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.csw.CSW;
import org.geotools.csw.CSWConfiguration;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.geotools.xsd.ows.OWS;
import org.junit.BeforeClass;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public abstract class CSWTestSupport extends GeoServerSystemTestSupport {
    protected static final String BASEPATH = "csw";

    @BeforeClass
    public static void configureXMLUnit() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("dc", DC.NAMESPACE);
        namespaces.put("dct", DCT.NAMESPACE);
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("ows", OWS.NAMESPACE);
        namespaces.put("ogc", OGC.NAMESPACE);
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", XLINK.NAMESPACE);
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    };

    protected String root() {
        return "csw?";
    }

    /** Validates a document based on the CSW schemas */
    protected void checkValidationErrors(Document dom) throws Exception {
        checkValidationErrors(dom, new CSWConfiguration());
    }

    /** Validates a document against the */
    protected void checkValidationErrors(Document dom, Configuration configuration)
            throws Exception {
        Parser p = new Parser(configuration);
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Exception exception : p.getValidationErrors()) {
                SAXParseException ex = (SAXParseException) exception;
                LOGGER.severe(
                        ex.getLineNumber() + "," + ex.getColumnNumber() + " -" + ex.toString());
            }
            fail("Document did not validate.");
        }
    }

    /** Loads the specified resource into a string */
    protected String getResourceAsString(String resourceLocation) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourceLocation)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }

    /** Loads the specified resource into a reader */
    protected Reader getResourceAsReader(String resourceLocation) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceLocation);
        return new InputStreamReader(is);
    }
}
