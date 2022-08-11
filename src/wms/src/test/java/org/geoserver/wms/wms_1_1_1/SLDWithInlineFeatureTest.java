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

package org.geoserver.wms.wms_1_1_1;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class SLDWithInlineFeatureTest extends GeoServerSystemTestSupport {

    @Test
    public void testSLDWithInlineFeatureWMS() throws Exception {
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                getClass().getResourceAsStream("SLDWithInlineFeature.xml")))) {
            String line;
            StringBuilder builder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            assertStatusCodeForPost(200, "wms", builder.toString(), "text/xml");

            // this is the test; an exception will be thrown if no image was rendered
            BufferedImage image =
                    ImageIO.read(
                            getBinaryInputStream(postAsServletResponse("wms", builder.toString())));

            assertNotNull(image);
        }
    }

    @Test
    public void testGetMapPostEntityExpansion() throws Exception {
        String body =
                IOUtils.toString(
                        getClass().getResourceAsStream("GetMapExternalEntity.xml"), "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wms", body);
        // should fail with an error message pointing at entity resolution
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());
        final String content = response.getContentAsString();
        assertThat(content, containsString("Entity resolution disallowed"));
        assertThat(content, containsString("/this/file/does/not/exist"));
    }

    @Test
    public void testSLDBody() throws Exception {
        String request =
                "wms?FORMAT=image/png&TRANSPARENT=TRUE&HEIGHT=406&WIDTH=810&REQUEST=GetMap&SRS=EPSG:4326&VERSION=1.1.1&BBOX=-120,-120,120,120&SLD_BODY=%3C%3Fxml%20version%3D%221.0%22%20encoding%3D%22UTF-8%22%3F%3E%3CStyledLayerDescriptor%20version%3D%221.0.0%22%20xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22%20xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%20xmlns%3D%22http%3A%2F%2Fwww.opengis.net%2Fsld%22%3E%3CUserLayer%3E%3CName%3Ejunk%3C%2FName%3E%3CInlineFeature%3E%3CFeatureCollection%3E%3CfeatureMember%3E%3CBodyPart%3E%3CType%3EFace%3C%2FType%3E%3CpolygonProperty%3E%3Cgml%3APolygon%3E%3Cgml%3AouterBoundaryIs%3E%3Cgml%3ALinearRing%3E%3Cgml%3Acoordinates%3E-10%2C10%2010%2C10%2010%2C-10%20-10%2C-10%20-10%2C10%3C%2Fgml%3Acoordinates%3E%3C%2Fgml%3ALinearRing%3E%3C%2Fgml%3AouterBoundaryIs%3E%3C%2Fgml%3APolygon%3E%3C%2FpolygonProperty%3E%3C%2FBodyPart%3E%3C%2FfeatureMember%3E%3C%2FFeatureCollection%3E%3C%2FInlineFeature%3E%3CLayerFeatureConstraints%3E%3CFeatureTypeConstraint%3E%3C%2FFeatureTypeConstraint%3E%3C%2FLayerFeatureConstraints%3E%3CUserStyle%3E%3CFeatureTypeStyle%3E%3CRule%3E%3CPolygonSymbolizer%3E%3CFill%3E%3CCssParameter%20name%3D%22fill%22%3E%3Cogc%3ALiteral%3E%23F00620%3C%2Fogc%3ALiteral%3E%3C%2FCssParameter%3E%3CCssParameter%20name%3D%22fill-opacity%22%3E%3Cogc%3ALiteral%3E1.0%3C%2Fogc%3ALiteral%3E%3C%2FCssParameter%3E%3C%2FFill%3E%3CStroke%3E%3CCssParameter%20name%3D%22stroke%22%3E%3Cogc%3ALiteral%3E%23FF0000%3C%2Fogc%3ALiteral%3E%3C%2FCssParameter%3E%3C%2FStroke%3E%3C%2FPolygonSymbolizer%3E%3C%2FRule%3E%3C%2FFeatureTypeStyle%3E%3C%2FUserStyle%3E%3C%2FUserLayer%3E%3C%2FStyledLayerDescriptor%3E";
        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals("image/png", response.getContentType());

        // this is the test; an exception will be thrown if no image was rendered
        BufferedImage image = ImageIO.read(getBinaryInputStream(getAsServletResponse(request)));

        assertNotNull(image);
    }
}
