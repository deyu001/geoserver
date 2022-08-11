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

package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.system.status.SystemStatusMonitorPanel;

public abstract class AbstractStatusPage extends ServerAdminPage {

    /** serialVersionUID */
    private static final long serialVersionUID = -6228795354577370186L;

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    public AbstractStatusPage() {
        initUI();
    }

    protected void initUI() {

        List<ITab> tabs = new ArrayList<>();

        PanelCachingTab statusTab =
                new PanelCachingTab(
                        new AbstractTab(new Model<>("Status")) {
                            private static final long serialVersionUID = 9062803783143908814L;

                            public Panel getPanel(String id) {
                                return new StatusPanel(id, AbstractStatusPage.this);
                            }
                        });
        PanelCachingTab moduleStatusTab =
                new PanelCachingTab(
                        new AbstractTab(new Model<>("Modules")) {
                            private static final long serialVersionUID = -5301288750339244612L;

                            public Panel getPanel(String id) {
                                return new ModuleStatusPanel(id, AbstractStatusPage.this);
                            }
                        });
        PanelCachingTab systemStatusTab =
                new PanelCachingTab(
                        new AbstractTab(new StringResourceModel("MonitoringPanel.title")) {
                            private static final long serialVersionUID = -5301288750339244612L;

                            public Panel getPanel(String id) {
                                return new SystemStatusMonitorPanel(id);
                            }
                        });

        tabs.add(statusTab);
        tabs.add(moduleStatusTab);
        tabs.add(systemStatusTab);

        // extension point for adding extra tabs that will be ordered using the extension priority
        GeoServerExtensions.extensions(StatusPage.TabDefinition.class)
                .forEach(
                        tabDefinition -> {
                            // create the new extra panel using the tab definition title
                            String title =
                                    new ResourceModel(tabDefinition.getTitleKey()).getObject();
                            PanelCachingTab tab =
                                    new PanelCachingTab(
                                            new AbstractTab(new Model<>(title)) {
                                                private static final long serialVersionUID =
                                                        -5301288750339244612L;
                                                // create the extra tab panel passing down the
                                                // container id
                                                public Panel getPanel(String panelId) {
                                                    return tabDefinition.createPanel(
                                                            panelId, AbstractStatusPage.this);
                                                }
                                            });
                            tabs.add(tab);
                        });
        AjaxTabbedPanel tabbedPanel = new AjaxTabbedPanel<>("tabs", tabs);
        tabbedPanel
                .get("panel")
                .add(
                        new Behavior() {

                            @Override
                            public boolean getStatelessHint(Component component) {
                                // this will force canCallListenerInterfaceAfterExpiry to be false
                                // when a pending Ajax request
                                // is processed for expired tabs, we can't predict the Ajax events
                                // that will be used
                                return false;
                            }
                        });
        add(tabbedPanel);
    }
    // Make sure child tabs can see this
    @Override
    protected boolean isAuthenticatedAsAdmin() {
        return super.isAuthenticatedAsAdmin();
    }

    @Override
    protected Catalog getCatalog() {
        return super.getCatalog();
    }

    @Override
    protected GeoServerApplication getGeoServerApplication() {
        return super.getGeoServerApplication();
    }

    @Override
    protected GeoServer getGeoServer() {
        return super.getGeoServerApplication().getGeoServer();
    }

    /**
     * Extensions that implement this interface will be able to contribute a new tabs to GeoServer
     * status page, interface {@link org.geoserver.platform.ExtensionPriority} should be used to
     * define the tab priority.
     */
    public interface TabDefinition {

        // title of the tab
        String getTitleKey();

        // content of the tab, the created panel should use the provided id
        Panel createPanel(String panelId, Page containerPage);
    }
}
