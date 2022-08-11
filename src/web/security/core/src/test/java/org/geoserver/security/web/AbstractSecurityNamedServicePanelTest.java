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

package org.geoserver.security.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestHandler;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.wicket.GeoServerDialog;
import org.junit.Before;

public abstract class AbstractSecurityNamedServicePanelTest
        extends AbstractSecurityWicketTestSupport {

    public static final String FIRST_COLUM_PATH = "itemProperties:0:component:link";
    public static final String CHECKBOX_PATH = "selectItemContainer:selectItem";

    protected AbstractSecurityPage basePage;
    protected String basePanelId;
    protected FormTester formTester;

    GeoServerSecurityManager manager;

    protected void newFormTester() {
        newFormTester("form");
    }

    protected void newFormTester(String path) {
        // formTester = tester.newFormTester(getDetailsFormComponentId());
        formTester = tester.newFormTester(path);
    }

    @Before
    public void init() throws Exception {
        manager = getSecurityManager();
    }

    protected abstract AbstractSecurityPage getBasePage();

    protected abstract String getBasePanelId();

    protected abstract Integer getTabIndex();

    protected abstract Class<? extends Component> getNamedServicesClass();

    protected abstract String getDetailsFormComponentId();

    protected void activatePanel() {
        basePage = getBasePage();
        basePanelId = getBasePanelId();
        tester.startPage(basePage);
        tester.assertRenderedPage(basePage.getPageClass());

        // String linkId = getTabbedPanel().getId()+":tabs-container:tabs:"+getTabIndex()+":link";
        // tester.clickLink(linkId,true);
        // assertEquals(getNamedServicesClass(), getNamedServicesPanel().getClass());
    }

    //    protected AjaxTabbedPanel getTabbedPanel() {
    //        return (AjaxTabbedPanel) tabbedPage.get(AbstractSecurityPage.TabbedPanelId);
    //    }

    //    protected NamedServicesPanel getNamedServicesPanel() {
    //        return (NamedServicesPanel) tabbedPage.get(getTabbedPanel().getId()+":panel");
    //
    //    }

    protected void clickAddNew() {
        tester.clickLink(basePanelId + ":add");
    }

    protected void clickRemove() {
        tester.clickLink(basePanelId + ":remove");
    }

    protected Component getRemoveLink() {
        Component result = tester.getLastRenderedPage().get("tabbedPanel:panel:removeSelected");
        assertNotNull(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    protected DataView<SecurityNamedServiceConfig> getDataView() {
        return (DataView<SecurityNamedServiceConfig>)
                basePage.get(basePanelId + ":table:listContainer:items");
    }

    protected long countItems() {
        tester.debugComponentTrees();
        return getDataView().getItemCount();
    }

    protected SecurityNamedServiceConfig getSecurityNamedServiceConfig(String name) {
        // <SecurityNamedServiceConfig>
        Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
        while (it.hasNext()) {
            Item<SecurityNamedServiceConfig> item = it.next();
            if (name.equals(item.getModelObject().getName())) return item.getModelObject();
        }
        return null;
    }

    protected void clickNamedServiceConfig(String name) {
        // <SecurityNamedServiceConfig>
        Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
        while (it.hasNext()) {
            Item<SecurityNamedServiceConfig> item = it.next();
            if (name.equals(item.getModelObject().getName()))
                tester.clickLink(item.getPageRelativePath() + ":" + FIRST_COLUM_PATH);
        }
    }

    protected void checkNamedServiceConfig(String name) {
        // <SecurityNamedServiceConfig>
        Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
        while (it.hasNext()) {
            Item<SecurityNamedServiceConfig> item = it.next();
            if (name.equals(item.getModelObject().getName()))
                tester.executeAjaxEvent(item.getPageRelativePath() + ":" + CHECKBOX_PATH, "click");
        }
    }

    protected void doRemove(String pathForLink, String... serviceNames) throws Exception {
        AbstractSecurityPage testPage = (AbstractSecurityPage) tester.getLastRenderedPage();

        if (serviceNames.length == 0) {
            String selectAllPath =
                    basePanelId + ":table:listContainer:selectAllContainer:selectAll";
            tester.assertComponent(selectAllPath, CheckBox.class);

            FormComponent selectAllPathComponent =
                    (FormComponent) tester.getComponentFromLastRenderedPage(selectAllPath);
            setFormComponentValue(selectAllPathComponent, "true");
            tester.executeAjaxEvent(selectAllPath, "click");
        } else {
            List<String> nameList = Arrays.asList(serviceNames);

            Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
            while (it.hasNext()) {
                Item<SecurityNamedServiceConfig> item = it.next();
                if (nameList.contains(item.getModelObject().getName())) {
                    String checkBoxPath = item.getPageRelativePath() + ":" + CHECKBOX_PATH;

                    tester.assertComponent(checkBoxPath, CheckBox.class);

                    FormComponent checkBoxPathComponent =
                            (FormComponent) tester.getComponentFromLastRenderedPage(checkBoxPath);
                    setFormComponentValue(checkBoxPathComponent, "true");

                    testPage.get(checkBoxPath).setDefaultModelObject(true);
                    tester.executeAjaxEvent(checkBoxPath, "click");
                }
            }
        }

        tester.assertNoErrorMessage();

        tester.assertComponent(basePanelId + ":dialog:dialog", ModalWindow.class);
        ModalWindow w = (ModalWindow) testPage.get(basePanelId + ":dialog:dialog");
        /*(ModalWindow) testPage.get(
        testPage.getWicketPath() + ":dialog:dialog");*/

        assertFalse(w.isShown());
        tester.clickLink(basePanelId + ":remove", true);
        assertTrue(w.isShown());

        ((GeoServerDialog) w.getParent())
                .submit(new AjaxRequestHandler(tester.getLastRenderedPage()));
        // simulateDeleteSubmit();
        // executeModalWindowCloseButtonCallback(w);
    }

    protected void simulateDeleteSubmit() throws Exception {
        // AjaxLink link = (AjaxLInk) tester.getLastRenderedPage().get(basePanelId + ":remove");
        // link.on
    }

    protected void setSecurityConfigName(String aName) {
        formTester.setValue("panel:content:name", aName);
    }

    protected String getSecurityConfigName() {
        return formTester.getForm().get("config.name").getDefaultModelObjectAsString();
    }

    protected String getSecurityConfigClassName() {
        return formTester.getForm().get("config.className").getDefaultModelObjectAsString();
    }

    @SuppressWarnings("deprecation")
    protected <T extends SecurityNamedServicePanelInfo> void setSecurityConfigClassName(
            Class<T> clazz) {
        ListView list = (ListView) tester.getLastRenderedPage().get("servicesContainer:services");
        int toClick = -1;
        for (int i = 0; i < list.getList().size(); i++) {
            if (clazz.isInstance(list.getList().get(i))) {
                toClick = i;
                break;
            }
        }
        AjaxLink link = (AjaxLink) list.get(toClick).get("link");
        if (link.isEnabled()) {
            tester.executeAjaxEvent(link, "click");
        }
        //        formTester.select("config.className", index);
        //
        // tester.executeAjaxEvent(formTester.getForm().getPageRelativePath()+":config.className",
        // "change");
    }

    protected void clickSave() {
        formTester.submit("save");
    }

    protected void clickCancel() {
        formTester.submitLink("cancel", false);
    }
}
