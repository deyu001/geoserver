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

package org.geoserver.cluster;

import static org.geoserver.cluster.JmsEventsListener.getMessagesForHandler;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import javax.jms.Message;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.handlers.catalog.JMSCatalogAddEventHandlerSPI;
import org.geoserver.cluster.impl.handlers.catalog.JMSCatalogModifyEventHandlerSPI;
import org.geoserver.cluster.impl.handlers.catalog.JMSCatalogRemoveEventHandlerSPI;
import org.geoserver.cluster.impl.handlers.catalog.JMSCatalogStylesFileHandlerSPI;
import org.geoserver.cluster.server.events.StyleModifyEvent;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests related with styles events. */
public final class JmsStylesTest extends GeoServerSystemTestSupport {

    private static final String TEST_STYLE_NAME = "test_style";
    private static final String TEST_STYLE_FILE = "/test_style.sld";

    private static final String TEST_WORKSPACE_NAME = MockData.DEFAULT_PREFIX;

    private static final String CATALOG_ADD_EVENT_HANDLER_KEY = "JMSCatalogAddEventHandlerSPI";
    private static final String CATALOG_MODIFY_EVENT_HANDLER_KEY =
            "JMSCatalogModifyEventHandlerSPI";
    private static final String CATALOG_STYLES_FILE_EVENT_HANDLER_KEY =
            "JMSCatalogStylesFileHandlerSPI";
    private static final String CATALOG_REMOVE_EVENT_HANDLER_KEY =
            "JMSCatalogRemoveEventHandlerSPI";

    private WorkspaceInfo testWorkspace;

    private JMSEventHandler<String, DocumentFile> styleFileHandler;
    private JMSEventHandler<String, CatalogEvent> addEventHandler;
    private JMSEventHandler<String, CatalogEvent> modifyEventHandler;
    private JMSEventHandler<String, CatalogEvent> removeEventHandler;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        // adding our test spring context
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void beforeTest() {
        // get the test workspace from the catalog
        testWorkspace = getCatalog().getWorkspaceByName(TEST_WORKSPACE_NAME);
        assertThat(testWorkspace, notNullValue());
        // initiate the handlers related to styles
        styleFileHandler =
                GeoServerExtensions.bean(JMSCatalogStylesFileHandlerSPI.class).createHandler();
        assertThat(styleFileHandler, notNullValue());
        addEventHandler =
                GeoServerExtensions.bean(JMSCatalogAddEventHandlerSPI.class).createHandler();
        assertThat(addEventHandler, notNullValue());
        modifyEventHandler =
                GeoServerExtensions.bean(JMSCatalogModifyEventHandlerSPI.class).createHandler();
        assertThat(modifyEventHandler, notNullValue());
        removeEventHandler =
                GeoServerExtensions.bean(JMSCatalogRemoveEventHandlerSPI.class).createHandler();
        assertThat(removeEventHandler, notNullValue());
    }

    @After
    public void afterTest() {
        // search the test style in the catalog
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(TEST_STYLE_NAME);
        if (style != null) {
            // the test style exists so let's remove it
            catalog.remove(style);
        }
        // search the test style in the catalog by workspace
        style = catalog.getStyleByName(MockData.DEFAULT_PREFIX, TEST_STYLE_NAME);
        if (style != null) {
            // the test style exists so let's remove it
            catalog.remove(style);
        }
        // clear all pending events
        JmsEventsListener.clear();
    }

    @Test
    public void testAddStyle() throws Exception {
        // add the test to the style catalog
        getTestData().addStyle(TEST_STYLE_NAME, TEST_STYLE_FILE, this.getClass(), getCatalog());
        // waiting for a catalog add event and a style file event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000,
                        (selected) -> selected.size() >= 2,
                        CATALOG_ADD_EVENT_HANDLER_KEY,
                        CATALOG_STYLES_FILE_EVENT_HANDLER_KEY);
        // let's check if the new added style was correctly published
        assertThat(messages.size(), is(2));
        // checking that the correct style file was published
        List<DocumentFile> styleFile =
                getMessagesForHandler(
                        messages, CATALOG_STYLES_FILE_EVENT_HANDLER_KEY, styleFileHandler);
        assertThat(styleFile.size(), is(1));
        assertThat(styleFile.get(0).getResourceName(), is("test_style.sld"));
        // checking that the correct style was published
        List<CatalogEvent> styleAddEvent =
                getMessagesForHandler(messages, CATALOG_ADD_EVENT_HANDLER_KEY, addEventHandler);
        assertThat(styleAddEvent.size(), is(1));
        assertThat(styleAddEvent.get(0).getSource(), instanceOf(StyleInfo.class));
        StyleInfo styleInfo = (StyleInfo) styleAddEvent.get(0).getSource();
        assertThat(styleInfo.getName(), is(TEST_STYLE_NAME));
        assertThat(styleInfo.getWorkspace(), nullValue());
    }

    @Test
    public void testAddStyleToWorkspace() throws Exception {
        // add the test to the style catalog
        getTestData()
                .addStyle(
                        testWorkspace,
                        TEST_STYLE_NAME,
                        TEST_STYLE_FILE,
                        this.getClass(),
                        getCatalog());
        // waiting for a catalog add event and a style file event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000,
                        (selected) -> selected.size() >= 2,
                        CATALOG_ADD_EVENT_HANDLER_KEY,
                        CATALOG_STYLES_FILE_EVENT_HANDLER_KEY);
        // let's check if the new added style was correctly published
        assertThat(messages.size(), is(2));
        // checking that the correct style file was published
        List<DocumentFile> styleFile =
                getMessagesForHandler(
                        messages, CATALOG_STYLES_FILE_EVENT_HANDLER_KEY, styleFileHandler);
        assertThat(styleFile.size(), is(1));
        assertThat(styleFile.get(0).getResourceName(), is("test_style.sld"));
        // checking that the correct style was published
        List<CatalogEvent> styleAddEvent =
                getMessagesForHandler(messages, CATALOG_ADD_EVENT_HANDLER_KEY, addEventHandler);
        assertThat(styleAddEvent.size(), is(1));
        assertThat(styleAddEvent.get(0).getSource(), instanceOf(StyleInfo.class));
        StyleInfo styleInfo = (StyleInfo) styleAddEvent.get(0).getSource();
        assertThat(styleInfo.getName(), is(TEST_STYLE_NAME));
        assertThat(styleInfo.getWorkspace(), is(testWorkspace));
    }

    @Test
    public void testModifyStyleWorkspace() throws Exception {
        // add the test to the style catalog
        addTestStyle();
        // modify the style associated file
        StyleInfo styleInfo = getCatalog().getStyleByName(TEST_STYLE_NAME);
        assertThat(styleInfo, notNullValue());
        getCatalog()
                .getResourcePool()
                .writeStyle(
                        styleInfo,
                        JmsStylesTest.class.getResourceAsStream("/test_style_modified.sld"));
        // modify the style workspace
        styleInfo.setWorkspace(testWorkspace);
        getCatalog().save(styleInfo);
        // waiting for a catalog modify event and a style file event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000, (selected) -> selected.size() >= 1, CATALOG_MODIFY_EVENT_HANDLER_KEY);
        assertThat(messages.size(), is(1));
        // checking that the correct catalog style was published
        List<CatalogEvent> styleModifiedEvent =
                getMessagesForHandler(messages, CATALOG_MODIFY_EVENT_HANDLER_KEY, addEventHandler);
        assertThat(styleModifiedEvent.size(), is(1));
        assertThat(styleModifiedEvent.get(0).getSource(), instanceOf(StyleInfo.class));
        StyleInfo modifiedStyle = (StyleInfo) styleModifiedEvent.get(0).getSource();
        assertThat(modifiedStyle.getName(), is(TEST_STYLE_NAME));
        // check that the catalog modify event contains the correct workspace
        WorkspaceInfo workspace =
                searchPropertyNewValue(
                        (CatalogModifyEvent) styleModifiedEvent.get(0),
                        "workspace",
                        WorkspaceInfo.class);
        assertThat(workspace, is(testWorkspace));
        // check that the correct file style was published
        assertThat(styleModifiedEvent.get(0), instanceOf(StyleModifyEvent.class));
        byte[] fileContent = ((StyleModifyEvent) styleModifiedEvent.get(0)).getFile();
        assertThat(fileContent, notNullValue());
        assertThat(fileContent.length, not(0));
        // parse the published style file and check the opacity value
        Style style = parseStyleFile(styleInfo, new ByteArrayInputStream(fileContent));
        RasterSymbolizer symbolizer =
                (RasterSymbolizer)
                        style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertThat(symbolizer.getOpacity().evaluate(null), is("0.5"));
    }

    @Test
    public void testRemoveStyle() throws Exception {
        // add the test to the style catalog
        addTestStyle();
        // remove style from catalog
        StyleInfo style = getCatalog().getStyleByName(TEST_STYLE_NAME);
        assertThat(style, notNullValue());
        getCatalog().remove(style);
        // waiting for a catalog remove event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000, (selected) -> selected.size() >= 1, CATALOG_REMOVE_EVENT_HANDLER_KEY);
        assertThat(messages.size(), is(1));
        // checking that the correct style was published
        List<CatalogEvent> styleRemoveEvent =
                getMessagesForHandler(messages, CATALOG_REMOVE_EVENT_HANDLER_KEY, addEventHandler);
        assertThat(styleRemoveEvent.size(), is(1));
        assertThat(styleRemoveEvent.get(0).getSource(), instanceOf(StyleInfo.class));
        StyleInfo removedStyle = (StyleInfo) styleRemoveEvent.get(0).getSource();
        assertThat(removedStyle.getName(), is(TEST_STYLE_NAME));
    }

    /** Helper method that adds the test style to the catalog and consume the produced events. */
    private void addTestStyle() throws Exception {
        // add the test to the style catalog
        getTestData().addStyle(TEST_STYLE_NAME, TEST_STYLE_FILE, this.getClass(), getCatalog());
        // waiting for a catalog add event and a style file event
        // waiting for a catalog add event and a style file event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000,
                        (selected) -> selected.size() >= 2,
                        CATALOG_ADD_EVENT_HANDLER_KEY,
                        CATALOG_STYLES_FILE_EVENT_HANDLER_KEY);
        assertThat(messages.size(), is(2));
    }

    /** Helper method that parses the file associated with a style. */
    private Style parseStyleFile(StyleInfo styleInfo, InputStream input) throws Exception {
        StyleHandler styleHandler = Styles.handler(styleInfo.getFormat());
        StyledLayerDescriptor styleDescriptor =
                styleHandler.parse(input, styleInfo.getFormatVersion(), null, null);
        return Styles.style(styleDescriptor);
    }

    /** Helper method that searches a modified property in a catalog modify event. */
    private <T> T searchPropertyNewValue(
            CatalogModifyEvent event, String propertyName, Class<WorkspaceInfo> propertyType) {
        // sanity check of the modify event properties and values
        assertThat(event.getPropertyNames(), notNullValue());
        assertThat(event.getPropertyNames().isEmpty(), not(true));
        assertThat(event.getNewValues(), notNullValue());
        assertThat(event.getNewValues().isEmpty(), not(true));
        assertThat(event.getPropertyNames().size(), is(event.getNewValues().size()));
        // find the property we want
        Object propertyValue = null;
        for (int i = 0; i < event.getPropertyNames().size(); i++) {
            String candidatePropertyName = event.getPropertyNames().get(i);
            if (candidatePropertyName != null
                    && candidatePropertyName.equalsIgnoreCase(propertyName)) {
                propertyValue = event.getNewValues().get(i);
            }
        }
        // return the found value
        assertThat(propertyValue, notNullValue());
        assertThat(propertyValue, instanceOf(propertyType));
        return (T) propertyValue;
    }
}
