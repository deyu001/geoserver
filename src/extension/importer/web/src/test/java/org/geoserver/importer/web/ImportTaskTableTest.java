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

package org.geoserver.importer.web;

import java.io.File;
import java.io.InputStream;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.importer.Archive;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.util.IOUtils;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** @author Kevin Smith, Boundless */
public class ImportTaskTableTest extends GeoServerWicketTestSupport {
    private ImportData data;
    private ImportContext context;
    private GeoServerDataProvider<ImportTask> provider;
    private FeedbackPanel feedback;

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        // Create a test file.
        File file = temp.newFile("twoShapefilesNoPrj.zip");
        try (InputStream rin =
                ImportTaskTableTest.class.getResourceAsStream("twoShapefilesNoPrj.zip"); ) {
            IOUtils.copy(rin, file);
        }

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);

        data = new Archive(file);

        context = ImporterWebUtils.importer().createContext(data);

        provider = new ImportTaskProvider(context);

        ImportTaskTable table = new ImportTaskTable("taskTable", provider, true);
        table.setFeedbackPanel(feedback);
        table.setOutputMarkupId(true);

        tester.startComponentInPage(table);
    }

    @Test
    public void testTwoCRSSetByFindThenApply() {
        tester.assertComponent("taskTable", ImportTaskTable.class);

        // Click the Find CRS button for the first layer to import
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:find", true);
        // Select the first CRS
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:popup:content:table:listContainer:items:1:itemProperties:0:component:link",
                true);
        // Click the Find CRS button for the second layer to import
        tester.clickLink(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:find", true);
        // Select the first CRS
        tester.clickLink(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:popup:content:table:listContainer:items:2:itemProperties:0:component:link",
                true);

        // The EPSG codes should be set
        tester.assertModelValue(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:srs",
                "EPSG:2000");
        tester.assertModelValue(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:srs",
                "EPSG:2001");

        // Check that the WKT links set
        tester.assertModelValue(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:Anguilla 1957 / British West Indies Grid");
        tester.assertModelValue(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:Antigua 1943 / British West Indies Grid");

        // Apply the first
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:apply", true);
        // The first entry should be replaced with an "Advanced" link, the numbering continues from
        // those used before so the second item is 3
        tester.assertComponent(
                "taskTable:listContainer:items:3:itemProperties:2:component",
                ImportTaskTable.AdvancedOptionPanel.class);
        // The second (4) should still be set
        tester.assertModelValue(
                "taskTable:listContainer:items:4:itemProperties:2:component:form:crs:srs",
                "EPSG:2001");
    }

    void fill(String formPath, String fieldPath, String value) {
        FormTester form = tester.newFormTester(formPath);
        form.setValue(fieldPath, value);
        tester.executeAjaxEvent(String.format("%s:%s", formPath, fieldPath), "blur");
    }

    @Test
    public void testTwoCRSSetManuallyThenApply() {
        tester.assertComponent("taskTable", ImportTaskTable.class);

        // "Type" in the EPSG codes
        fill(
                "taskTable:listContainer:items:1:itemProperties:2:component:form",
                "crs:srs",
                "EPSG:3857");
        fill(
                "taskTable:listContainer:items:2:itemProperties:2:component:form",
                "crs:srs",
                "EPSG:4326");

        // Check that the WKT links set
        tester.assertModelValue(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:WGS 84 / Pseudo-Mercator");
        tester.assertModelValue(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:WGS 84");

        // Apply the first
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:apply", true);
        // The first entry should be replaced with an "Advanced" link, the numbering continues from
        // those used before so the second item is 3
        tester.assertComponent(
                "taskTable:listContainer:items:3:itemProperties:2:component",
                ImportTaskTable.AdvancedOptionPanel.class);
        // The second (4) should still be set
        tester.assertModelValue(
                "taskTable:listContainer:items:4:itemProperties:2:component:form:crs:srs",
                "EPSG:4326");
    }
}
