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

package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Locale;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.file.File;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.metadata.web.resource.WicketResourceResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the ExternalResourceLoader.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class ExternalResourceLoaderTest extends AbstractWicketMetadataTest {

    private Locale originalLocale;

    @Before
    public void before() throws IOException {
        login();

        originalLocale = Session.get().getLocale();

        // Load the page
        MetadataTemplatesPage page = new MetadataTemplatesPage();
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatesPage.class);
    }

    @After
    public void after() throws Exception {
        logout();

        Session.get().setLocale(originalLocale);
    }

    @Test
    public void testExternalResourceLoader() throws IOException {
        File metadata = new File(DATA_DIRECTORY.getDataDirectoryRoot(), "metadata");
        WicketResourceResourceLoader loader =
                new WicketResourceResourceLoader(Files.asResource(metadata), "metadata.properties");

        String actual =
                loader.loadStringResource(
                        tester.getLastRenderedPage(), "metadata.generated.form.identifier-single");
        Assert.assertEquals("identifier single field", actual);

        Session.get().setLocale(new Locale("nl"));
        actual =
                loader.loadStringResource(
                        tester.getLastRenderedPage(), "metadata.generated.form.identifier-single");
        Assert.assertEquals("identifier single field", actual);
    }

    @Test
    public void testExternalResourceLoaderDutch() throws IOException {
        Session.get().setLocale(new Locale("nl"));
        File metadata = new File(DATA_DIRECTORY.getDataDirectoryRoot(), "metadata");
        WicketResourceResourceLoader loader =
                new WicketResourceResourceLoader(Files.asResource(metadata), "metadata.properties");

        String actual =
                loader.loadStringResource(
                        tester.getLastRenderedPage(), "metadata.generated.form.number-field");
        Assert.assertEquals("Getal veld", actual);
    }

    @Test
    public void testLocalizationLabels() {
        Session.get().setLocale(new Locale("nl"));

        LayerInfo layer = geoServer.getCatalog().getLayerByName("mylayer");
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);
        tester.startPage(page);
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs"))
                .setSelectedTab(4);
        tester.submitForm("publishedinfo");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel", MetadataPanel.class);

        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:2:itemProperties:0:component",
                "identifier single field");
        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:3:itemProperties:0:component",
                "Getal veld");
        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:0:component",
                "the refsystem as list field");
        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:6:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component",
                "Het code veld");

        @SuppressWarnings("unchecked")
        DropDownChoice<String> choice =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:4:itemProperties:1:component:dropdown");
        assertEquals(
                "The Final Choice", choice.getChoiceRenderer().getDisplayValue("the-final-choice"));
    }
}
