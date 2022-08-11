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

package org.geoserver.web.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.ParamResourceModel;
import org.junit.Test;

public class ReprojectPageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // we don't need data configured in the catalog
        testData.setUpSecurity();
    }

    @Test
    public void testReprojectPoint() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "12 45");
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(0, tester.getMessages(FeedbackMessage.ERROR).size());
        String tx =
                tester.getComponentFromLastRenderedPage("form:targetGeom")
                        .getDefaultModelObjectAsString();
        String[] ordinateStrings = tx.split("\\s+");
        assertEquals(736446.0261038465, Double.parseDouble(ordinateStrings[0]), 1e-6);
        assertEquals(4987329.504699742, Double.parseDouble(ordinateStrings[1]), 1e-6);
    }

    @Test
    public void testInvalidPoint() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "12 a45a");
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        String message =
                ((ValidationErrorFeedback) tester.getMessages(FeedbackMessage.ERROR).get(0))
                        .getMessage()
                        .toString();
        String expected = new ParamResourceModel("GeometryTextArea.parseError", null).getString();
        assertEquals(expected, message);
    }

    @Test
    public void testReprojectLinestring() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "LINESTRING(12 45, 13 45)");
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(0, tester.getMessages(FeedbackMessage.ERROR).size());
        String tx =
                tester.getComponentFromLastRenderedPage("form:targetGeom")
                        .getDefaultModelObjectAsString();
        Matcher matcher =
                Pattern.compile("LINESTRING \\(([\\d\\.]+) ([\\d\\.]+), ([\\d\\.]+) ([\\d\\.]+)\\)")
                        .matcher(tx);
        assertTrue(tx, matcher.matches());
        assertEquals(736446.0261038465, Double.parseDouble(matcher.group(1)), 1e-6);
        assertEquals(4987329.504699742, Double.parseDouble(matcher.group(2)), 1e-6);
        assertEquals(815261.4271666661, Double.parseDouble(matcher.group(3)), 1e-6);
        assertEquals(4990738.261612577, Double.parseDouble(matcher.group(4)), 1e-6);
    }

    @Test
    public void testInvalidGeometry() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "LINESTRING(12 45, 13 45"); // missing ) at the end
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        String message =
                ((ValidationErrorFeedback) tester.getMessages(FeedbackMessage.ERROR).get(0))
                        .getMessage()
                        .toString();
        String expected = new ParamResourceModel("GeometryTextArea.parseError", null).getString();
        assertEquals(expected, message);
    }

    @Test
    public void testPageParams() {
        tester.startPage(
                ReprojectPage.class,
                new PageParameters().add("fromSRS", "EPSG:4326").add("toSRS", "EPSG:32632"));
        String source =
                tester.getComponentFromLastRenderedPage("form:sourceCRS:srs")
                        .getDefaultModelObjectAsString();
        String target =
                tester.getComponentFromLastRenderedPage("form:targetCRS:srs")
                        .getDefaultModelObjectAsString();
        assertEquals("EPSG:4326", source);
        assertEquals("EPSG:32632", target);
    }
}
