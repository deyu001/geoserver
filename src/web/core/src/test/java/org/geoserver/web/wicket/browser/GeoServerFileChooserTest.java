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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestHandler;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.geoserver.data.test.MockData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.junit.Before;
import org.junit.Test;

public class GeoServerFileChooserTest extends GeoServerWicketTestSupport {

    private File root;
    private File one;
    private File two;
    private File child;

    @Before
    public void init() throws IOException {

        root = new File("target/test-filechooser");
        if (root.exists()) FileUtils.deleteDirectory(root);
        child = new File(root, "child");
        child.mkdirs();
        one = new File(child, "one.txt");
        one.createNewFile();
        two = new File(child, "two.sld");
        two.createNewFile();
    }

    public void setupChooser(final File file) {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new GeoServerFileChooser(id, new Model<>(file));
                            }
                        }));

        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        setupChooser(root);

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel(
                "form:panel:fileTable:fileTable:fileContent:files:1:nameLink:name", "child/");
        assertEquals(
                1,
                ((DataView)
                                tester.getComponentFromLastRenderedPage(
                                        "form:panel:fileTable:fileTable:fileContent:files"))
                        .size());
    }

    @Test
    public void testNullRoot() {
        setupChooser(null);

        // make sure it does not now blow out because of the null
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel(
                "form:panel:breadcrumbs:path:0:pathItemLink:pathItem",
                getTestData().getDataDirectoryRoot().getName() + "/");
    }

    @Test
    public void testInDialog() throws Exception {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new GeoServerDialog(id);
                            }
                        }));

        tester.assertRenderedPage(FormTestPage.class);

        tester.debugComponentTrees();

        GeoServerDialog dialog =
                (GeoServerDialog) tester.getComponentFromLastRenderedPage("form:panel");
        assertNotNull(dialog);

        dialog.showOkCancel(
                new AjaxRequestHandler(tester.getLastRenderedPage()),
                new DialogDelegate() {
                    @Override
                    protected Component getContents(String id) {
                        return new GeoServerFileChooser(id, new Model<>(root));
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        assertNotNull(contents);
                        assertTrue(contents instanceof GeoServerFileChooser);
                        return true;
                    }
                });

        dialog.submit(new AjaxRequestHandler(tester.getLastRenderedPage()));
    }

    @Test
    public void testHideFileSystem() throws Exception {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            public Component buildComponent(String id) {
                                return new GeoServerFileChooser(id, new Model<>(), true);
                            }
                        }));

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DropDownChoice<File> rootChoice =
                (DropDownChoice<File>) tester.getComponentFromLastRenderedPage("form:panel:roots");
        assertEquals(1, rootChoice.getChoices().size());
        assertEquals(getDataDirectory().root(), rootChoice.getChoices().get(0));
    }

    @Test
    public void testAutocompleteDataDirectory() throws Exception {
        FileRootsFinder rootsFinder = new FileRootsFinder(true);
        getDataDirectory().get("/").dir();

        // looking for a match on basic polygons
        List<String> values =
                rootsFinder
                        .getMatches(MockData.CITE_PREFIX + "/poly", null)
                        .collect(Collectors.toList());
        assertEquals(1, values.size());
        assertEquals("file:cite/BasicPolygons.properties", values.get(0));

        // for the sake of checking, find a specific style with a file extension filter (the dd
        // contains both
        // raster.sld and raster.xml
        values =
                rootsFinder
                        .getMatches("/styles/raster", new ExtensionFileFilter(".sld"))
                        .collect(Collectors.toList());
        assertEquals(1, values.size());
        assertEquals("file:styles/raster.sld", values.get(0));
    }

    @Test
    public void testAutocompleteDirectories() throws Exception {
        FileRootsFinder rootsFinder = new FileRootsFinder(true);
        File dir = getDataDirectory().get("/").dir();

        // look for a property file in the data dir full path (so, not a relative path match).
        // we should still get directories
        String rootPath = dir.getCanonicalFile().getAbsolutePath() + File.separator;
        ExtensionFileFilter fileFilter = new ExtensionFileFilter(".properties");
        List<String> values =
                rootsFinder.getMatches(rootPath, fileFilter).collect(Collectors.toList());
        assertThat(values.size(), greaterThan(0));
        assertEquals(new HashSet<>(values).size(), values.size());
        assertThat(
                values,
                hasItem("file://" + new File(rootPath, MockData.CITE_PREFIX).getAbsolutePath()));
        assertThat(
                values,
                hasItem("file://" + new File(rootPath, MockData.SF_PREFIX).getAbsolutePath()));
    }
}
