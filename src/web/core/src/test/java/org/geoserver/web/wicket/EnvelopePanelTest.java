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

package org.geoserver.web.wicket;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class EnvelopePanelTest extends GeoServerWicketTestSupport {

    @Test
    public void testEditPlain() throws Exception {
        final ReferencedEnvelope e =
                new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new EnvelopePanel(id, e);
                            }
                        }));

        tester.assertComponent("form", Form.class);

        FormTester ft = tester.newFormTester("form");
        assertEquals("-180", ft.getTextComponentValue("panel:minX"));
        assertEquals("-90", ft.getTextComponentValue("panel:minY"));
        assertEquals("180", ft.getTextComponentValue("panel:maxX"));
        assertEquals("90", ft.getTextComponentValue("panel:maxY"));

        EnvelopePanel ep = (EnvelopePanel) tester.getComponentFromLastRenderedPage("form:panel");
        assertEquals(e, ep.getModelObject());

        ft.setValue("panel:minX", "-2");
        ft.setValue("panel:minY", "-2");
        ft.setValue("panel:maxX", "2");
        ft.setValue("panel:maxY", "2");

        ft.submit();

        assertEquals(new Envelope(-2, 2, -2, 2), ep.getModelObject());
        assertEquals(
                DefaultGeographicCRS.WGS84, ep.getModelObject().getCoordinateReferenceSystem());
    }

    @Test
    public void testEditCRS() throws Exception {
        CoordinateReferenceSystem epsg4326 = CRS.decode("EPSG:4326", true);
        CoordinateReferenceSystem epsg4140 = CRS.decode("EPSG:4140", true);
        final ReferencedEnvelope e = new ReferencedEnvelope(-180, 180, -90, 90, epsg4326);
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                EnvelopePanel panel = new EnvelopePanel(id, e);
                                panel.setCRSFieldVisible(true);
                                return panel;
                            }
                        }));

        tester.assertComponent("form", Form.class);

        FormTester ft = tester.newFormTester("form");
        // print(tester.getLastRenderedPage(), true, true);
        assertEquals("-180", ft.getTextComponentValue("panel:minX"));
        assertEquals("-90", ft.getTextComponentValue("panel:minY"));
        assertEquals("180", ft.getTextComponentValue("panel:maxX"));
        assertEquals("90", ft.getTextComponentValue("panel:maxY"));
        assertEquals("EPSG:4326", ft.getTextComponentValue("panel:crsContainer:crs:srs"));

        EnvelopePanel ep = (EnvelopePanel) tester.getComponentFromLastRenderedPage("form:panel");
        assertEquals(e, ep.getModelObject());

        ft.setValue("panel:minX", "-2");
        ft.setValue("panel:minY", "-2");
        ft.setValue("panel:maxX", "2");
        ft.setValue("panel:maxY", "2");
        ft.setValue("panel:crsContainer:crs:srs", "EPSG:4140");

        ft.submit();

        assertEquals(new Envelope(-2, 2, -2, 2), ep.getModelObject());
        assertEquals(epsg4140, ep.getModelObject().getCoordinateReferenceSystem());
    }

    @Test
    public void testDecimalsPreserved() throws Exception {
        final ReferencedEnvelope e =
                new ReferencedEnvelope(-0.1E-10, 1.0E-9, -9E-11, 9E-11, DefaultGeographicCRS.WGS84);
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new EnvelopePanel(id, e);
                            }
                        }));

        tester.assertComponent("form", Form.class);

        FormTester ft = tester.newFormTester("form");
        assertEquals("-0.00000000001", ft.getTextComponentValue("panel:minX"));
        assertEquals("-0.00000000009", ft.getTextComponentValue("panel:minY"));
        assertEquals("0.000000001", ft.getTextComponentValue("panel:maxX"));
        assertEquals("0.00000000009", ft.getTextComponentValue("panel:maxY"));
    }
}
