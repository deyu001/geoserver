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

package org.geoserver.wcs.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs.WCSInfo;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Base support class for wcs tests.
 *
 * @author Andrea Aime, TOPP
 */
public abstract class WCSTestSupport extends CoverageTestSupport {
    protected static XpathEngine xpath;

    protected static final boolean IS_WINDOWS;

    protected static final Schema WCS11_SCHEMA;

    static {
        File wcsTestFile = new File("./schemas/wcs/1.1.1/wcsAll.xsd");

        // Specific check for building from an IDE such as IntelliJ IDEA.
        if (!wcsTestFile.exists()) {
            wcsTestFile = new File("wcs1_1/schemas/wcs/1.1.1/wcsAll.xsd");
        }

        try {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS11_SCHEMA = factory.newSchema(wcsTestFile);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.1.1 schemas", e);
        }
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").matches(".*Windows.*");
        } catch (Exception e) {
            // no os.name? oh well, never mind
        }
        IS_WINDOWS = windows;
    }

    /** @return The global wfs instance from the application context. */
    protected WCSInfo getWCS() {
        return getGeoServer().getService(WCSInfo.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }

    /** Parses a multipart message from the response */
    protected Multipart getMultipart(MockHttpServletResponse response)
            throws MessagingException, IOException {
        MimeMessage body = new MimeMessage(null, getBinaryInputStream(response));
        Multipart multipart = (Multipart) body.getContent();
        return multipart;
    }
}
