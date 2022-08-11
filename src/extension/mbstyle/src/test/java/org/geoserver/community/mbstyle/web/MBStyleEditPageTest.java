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

package org.geoserver.community.mbstyle.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.data.OpenLayersPreviewPanel;
import org.geoserver.wms.web.data.StyleEditPage;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Style;
import org.geotools.styling.TextSymbolizer2;
import org.junit.Before;
import org.junit.Test;
import org.opengis.style.Symbolizer;

public class MBStyleEditPageTest extends GeoServerWicketTestSupport {

    StyleInfo mbstyle;
    StyleEditPage edit;

    @Before
    public void setUp() throws Exception {
        Catalog catalog = getCatalog();
        login();

        mbstyle = new StyleInfoImpl(null);
        mbstyle.setName("mbstyle");
        mbstyle.setFilename("mbstyle.json");
        mbstyle.setFormat(MBStyleHandler.FORMAT);
        catalog.add(mbstyle);
        mbstyle = catalog.getStyleByName("mbstyle");
        catalog.save(mbstyle);

        edit = new StyleEditPage(mbstyle);
        tester.startPage(edit);
    }

    @Test
    public void testMbstyleChange() throws Exception {

        String json =
                "{\n"
                        + "  \"version\": 8, \n"
                        + "  \"name\": \"places\",\n"
                        + "  \"sprite\": \"http://localhost:8080/geoserver/styles/mbsprites\",\n"
                        + "  \"layers\": [\n"
                        + "    {\n"
                        + "      \"id\": \"circle\",\n"
                        + "      \"source-layer\": \"Buildings\",\n"
                        + "      \"type\": \"symbol\",\n"
                        + "      \"layout\": {\n"
                        + "        \"icon-image\": \"circle\",\n"
                        + "        \"icon-size\": {\n"
                        + "          \"property\": \"POP_MAX\",\n"
                        + "          \"type\": \"exponential\",\n"
                        + "          \"stops\": [\n"
                        + "            [0, 0.7],\n"
                        + "            [40000000, 3.7]\n"
                        + "          ]\n"
                        + "        }\n"
                        + "      }\n"
                        + "    }\n"
                        + "  ]\n"
                        + " }\n";

        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:format", MBStyleHandler.FORMAT);
        form.setValue("context:panel:name", "mbstyleTest");
        tester.executeAjaxEvent("apply", "click");
        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:2:link", "click");
        tester.assertComponent("styleForm:context:panel", OpenLayersPreviewPanel.class);
        tester.assertModelValue("styleForm:context:panel:previewStyleGroup", false);
        form.setValue("context:panel:previewStyleGroup", true);
        form.setValue("styleEditor:editorContainer:editorParent:editor", json);
        tester.executeAjaxEvent("apply", "click");
        tester.assertModelValue("styleForm:context:panel:previewStyleGroup", true);
        assertNotNull(getCatalog().getStyleByName("mbstyle").getSLD());
        Style style = getCatalog().getStyleByName("mbstyle").getStyle();
        Symbolizer sym = style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        TextSymbolizer2 label = (TextSymbolizer2) sym;
        ExternalGraphic eg = (ExternalGraphic) label.getGraphic().graphicalSymbols().get(0);
        assertEquals(
                eg.getURI(),
                "http://localhost:8080/geoserver/styles/mbsprites#icon=${strURLEncode('circle')}&size=${strURLEncode(Interpolate(POP_MAX,0,0.7,40000000,3.7,'numeric'))}");
    }
}
