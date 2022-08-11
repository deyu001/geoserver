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

package org.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.style.GraphicalSymbol;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class GeoServerDataDirectoryTest {

    ClassPathXmlApplicationContext ctx;

    GeoServerDataDirectory dataDir;
    CatalogFactory factory = new CatalogFactoryImpl(new CatalogImpl());

    @Before
    public void setUp() throws Exception {

        ctx =
                new ClassPathXmlApplicationContext(
                        "GeoServerDataDirectoryTest-applicationContext.xml", getClass());
        ctx.refresh();

        dataDir = new GeoServerDataDirectory(Files.createTempDirectory("data").toFile());
        dataDir.root().deleteOnExit();
    }

    @After
    public void tearDown() throws Exception {
        ctx.close();
    }

    @Test
    public void testNullWorkspace() {
        assertEquals(dataDir.get((WorkspaceInfo) null, "test").path(), dataDir.get("test").path());
        assertEquals(
                dataDir.getStyles((WorkspaceInfo) null, "test").path(),
                dataDir.getStyles("test").path());
        assertEquals(
                dataDir.getLayerGroups((WorkspaceInfo) null, "test").path(),
                dataDir.getLayerGroups("test").path());
    }

    @Test
    public void testParsedStyle() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();

        // Copy the sld to the temp style dir
        File styleFile = new File(styleDir, "external.sld");
        Files.copy(this.getClass().getResourceAsStream("external.sld"), styleFile.toPath());

        File iconFile = new File(styleDir, "icon.png");
        assertFalse(iconFile.exists());

        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("sld");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());

        Style s = dataDir.parsedStyle(si);
        // Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic =
                ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(((ExternalGraphic) graphic).getLocation(), iconFile.toURI().toURL());

        // GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }

    @Test
    public void testParsedStyleExternalWithParams() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();

        // Copy the sld to the temp style dir
        File styleFile = new File(styleDir, "external_with_params.sld");
        Files.copy(
                this.getClass().getResourceAsStream("external_with_params.sld"),
                styleFile.toPath());

        File iconFile = new File(styleDir, "icon.png");
        assertFalse(iconFile.exists());

        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("sld");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());

        Style s = dataDir.parsedStyle(si);
        // Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic =
                ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(
                ((ExternalGraphic) graphic).getLocation().getPath(),
                iconFile.toURI().toURL().getPath());

        assertEquals("param1=1", ((ExternalGraphic) graphic).getLocation().getQuery());

        // GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }

    /**
     * Test loading a parsed style with an external graphic URL that contains both ?queryParams and
     * a URL #fragment, and assert that those URL components are preserved.
     */
    @Test
    public void testParsedStyleExternalWithParamsAndFragment() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();

        // Copy the sld to the temp style dir
        File styleFile = new File(styleDir, "external_with_params_and_fragment.sld");
        Files.copy(
                this.getClass().getResourceAsStream("external_with_params_and_fragment.sld"),
                styleFile.toPath());

        File iconFile = new File(styleDir, "icon.png");
        assertFalse(iconFile.exists());

        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("sld");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());

        Style s = dataDir.parsedStyle(si);
        // Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic =
                ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(
                ((ExternalGraphic) graphic).getLocation().getPath(),
                iconFile.toURI().toURL().getPath());

        assertEquals("param1=1", ((ExternalGraphic) graphic).getLocation().getQuery());
        assertEquals("textAfterHash", ((ExternalGraphic) graphic).getLocation().getRef());

        // GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }
}
