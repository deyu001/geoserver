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

package org.geoserver.web.wicket.browser;

import static org.geoserver.web.GeoServerWicketTestSupport.initResourceSettings;
import static org.junit.Assert.assertEquals;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Before;
import org.junit.Test;

public class FileDataViewTest {

    private WicketTester tester;

    private File root;

    private File one;

    private File two;

    private File lastClicked;

    FileProvider fileProvider;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
        initResourceSettings(tester);

        root = new File("target/test-dataview");
        if (root.exists()) FileUtils.deleteDirectory(root);
        root.mkdirs();
        one = new File(root, "one.txt");
        one.createNewFile();
        two = new File(root, "two.sld");
        two.createNewFile();

        fileProvider = new FileProvider(root);

        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {

                                return new FileDataView(id, fileProvider) {

                                    @Override
                                    protected void linkNameClicked(
                                            File file, AjaxRequestTarget target) {
                                        lastClicked = file;
                                    }
                                };
                            }
                        }));

        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("form:panel:fileTable:fileContent:files:1:nameLink:name", "one.txt");
        tester.assertLabel("form:panel:fileTable:fileContent:files:2:nameLink:name", "two.sld");
        assertEquals(
                2,
                ((DataView)
                                tester.getComponentFromLastRenderedPage(
                                        "form:panel:fileTable:fileContent:files"))
                        .size());
    }

    @Test
    public void testClick() throws Exception {
        tester.clickLink("form:panel:fileTable:fileContent:files:1:nameLink");
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        assertEquals(one, lastClicked);
    }

    @Test
    public void testFilter() throws Exception {
        fileProvider.setFileFilter(new Model<>(new ExtensionFileFilter(".txt")));
        tester.startPage(tester.getLastRenderedPage());
        tester.assertLabel("form:panel:fileTable:fileContent:files:3:nameLink:name", "one.txt");
        assertEquals(
                1,
                ((DataView)
                                tester.getComponentFromLastRenderedPage(
                                        "form:panel:fileTable:fileContent:files"))
                        .size());
    }

    @Test
    public void testSortByName() throws Exception {

        // order by inverse name
        tester.clickLink("form:panel:fileTable:nameHeader:orderByLink", true);
        tester.clickLink("form:panel:fileTable:nameHeader:orderByLink", true);
        tester.assertRenderedPage(FormTestPage.class);

        tester.assertLabel("form:panel:fileTable:fileContent:files:5:nameLink:name", "two.sld");
        tester.assertLabel("form:panel:fileTable:fileContent:files:6:nameLink:name", "one.txt");
    }
}
