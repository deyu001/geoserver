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

package org.geoserver.wfs.v1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.util.ErrorHandler;
import org.geoserver.util.ReaderUtils;
import org.geoserver.wfs.CapabilitiesTransformer;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesTransformerTest extends WFSTestSupport {

    static Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs.test");

    GetCapabilitiesType request() {
        GetCapabilitiesType type = WfsFactory.eINSTANCE.createGetCapabilitiesType();
        type.setBaseUrl("http://localhost:8080/geoserver");
        return type;
    }

    @Test
    public void test() throws Exception {
        GetCapabilitiesType request = request();
        CapabilitiesTransformer tx =
                new CapabilitiesTransformer.WFS1_1(
                        getWFS(), request.getBaseUrl(), getCatalog(), Collections.emptyList());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request, output);

        InputStreamReader reader =
                new InputStreamReader(new ByteArrayInputStream(output.toByteArray()));

        File f = new File("../web/src/main/webapp/schemas/wfs/1.1.0/wfs.xsd");
        if (!f.exists()) {
            return;
        }

        ErrorHandler handler = new ErrorHandler(logger, Level.WARNING);
        // use the schema embedded in the web module
        ReaderUtils.validate(
                reader, handler, WFS.NAMESPACE, "../web/src/main/webapp/schemas/wfs/1.1.0/wfs.xsd");

        assertTrue(handler.errors.isEmpty());
    }

    /** see GEOS-2461 */
    @Test
    public void testDefaultOutputFormat() throws Exception {
        GetCapabilitiesType request = request();
        CapabilitiesTransformer tx =
                new CapabilitiesTransformer.WFS1_1(
                        getWFS(), request.getBaseUrl(), getCatalog(), Collections.emptyList());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request, output);

        Document dom = super.dom(new ByteArrayInputStream(output.toByteArray()));

        // XpathEngine xpath = XMLUnit.newXpathEngine();

        final String expected = "text/xml; subtype=gml/3.1.1";
        String xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='DescribeFeatureType']"
                        + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='GetFeature']"
                        + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='GetFeatureWithLock']"
                        + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='Transaction']"
                        + "/ows:Parameter[@name='inputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);
    }

    @Test
    public void testContactInfo() throws Exception {
        GetCapabilitiesType request = request();
        CapabilitiesTransformer tx =
                new CapabilitiesTransformer.WFS1_1(
                        getWFS(), request.getBaseUrl(), getCatalog(), Collections.emptyList());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request, output);

        Document dom = super.dom(new ByteArrayInputStream(output.toByteArray()));

        String xpathExpr =
                "//wfs:WFS_Capabilities/ows:ServiceProvider/ows:ServiceContact/ows:IndividualName";
        assertXpathExists(xpathExpr, dom);
        assertXpathEvaluatesTo("Andrea Aime", xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:ServiceProvider/ows:ServiceContact/ows:ContactInfo/ows:Address/ows:DeliveryPoint";
        assertXpathExists(xpathExpr, dom);
        assertXpathEvaluatesTo(
                "1600 Pennsylvania Ave NW, Washington DC 20500, United States", xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:ServiceProvider/ows:ServiceContact/ows:ContactInfo/ows:Address/ows:ElectronicMailAddress";
        assertXpathExists(xpathExpr, dom);
        assertXpathEvaluatesTo("andrea@geoserver.org", xpathExpr, dom);
    }
}
