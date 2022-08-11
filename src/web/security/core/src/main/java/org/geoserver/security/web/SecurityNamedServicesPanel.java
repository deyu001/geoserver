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

import static org.geoserver.security.web.SecurityNamedServiceProvider.NAME;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.web.SecurityNamedServiceProvider.ResourceBeanProperty;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/**
 * Base class for listing all instances of a class of named security service type.
 *
 * <p>This base panel provides a table of the configuration instances, along with "add new" and
 * "remove selected" functionality. Subclasses need to provide the specific type of {@link
 * SecurityNamedServiceProvider} for the service class, as well as implement some additional methods
 * for validating the removal of configuration instances.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class SecurityNamedServicesPanel<T extends SecurityNamedServiceConfig>
        extends Panel {

    SecurityNamedServiceTablePanel<T> tablePanel;
    AjaxLink removeLink;
    FeedbackPanel feedbackPanel;
    GeoServerDialog dialog;

    public SecurityNamedServicesPanel(String id, SecurityNamedServiceProvider<T> dataProvider) {
        super(id);

        final boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole();
        add(
                new AjaxLink("add") {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void onClick(AjaxRequestTarget target) {
                        // create a new config class and instantiate the page
                        SecurityNamedServiceNewPage newPage =
                                new SecurityNamedServiceNewPage(getServiceClass());
                        newPage.setReturnPage(getPage());
                        setResponsePage(newPage);
                    }
                }.setEnabled(isAdmin));

        add(
                removeLink =
                        new AjaxLink("remove") {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                boolean ok = true;
                                for (T config : tablePanel.getSelection()) {
                                    // first determine if the config can be removed
                                    try {
                                        validateRemoveConfig(config);
                                    } catch (Exception e) {
                                        handleException(e, null);
                                        ok = false;
                                    }
                                }

                                if (ok) {
                                    // proceed with the removal, confirming first
                                    dialog.showOkCancel(
                                            target,
                                            new GeoServerDialog.DialogDelegate() {
                                                @Override
                                                protected Component getContents(String id) {
                                                    return new ConfirmRemovalNamedServicePanel<>(
                                                            id, tablePanel.getSelection());
                                                }

                                                @Override
                                                protected boolean onSubmit(
                                                        AjaxRequestTarget target,
                                                        Component contents) {
                                                    for (T config : tablePanel.getSelection()) {
                                                        try {
                                                            removeConfig(config);
                                                            feedbackPanel.info(
                                                                    config.getName() + " removed");
                                                            tablePanel.clearSelection();
                                                        } catch (Exception e) {
                                                            handleException(e, feedbackPanel);
                                                        }
                                                    }
                                                    return true;
                                                }

                                                @Override
                                                public void onClose(AjaxRequestTarget target) {
                                                    target.add(tablePanel);
                                                    target.add(feedbackPanel);
                                                }
                                            });
                                }

                                // render any feedback
                                target.add(feedbackPanel);
                            }
                        });
        removeLink.setEnabled(false);

        add(
                tablePanel =
                        new SecurityNamedServiceTablePanel<T>("table", dataProvider) {
                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                if (isAdmin) {
                                    target.add(removeLink.setEnabled(!getSelection().isEmpty()));
                                }
                            }
                        });
        tablePanel.setOutputMarkupId(true);
        tablePanel.getTopPager().setVisible(false);

        add(tablePanel);

        add(feedbackPanel = new FeedbackPanel("feedback"));
        feedbackPanel.setOutputMarkupId(true);

        add(dialog = new GeoServerDialog("dialog"));
    }

    /** accessors for security manager */
    public GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    /*
     * helper for handling an exception by reporting it as an error on the feedback panel
     */
    void handleException(Exception e, Component target) {
        (target != null ? target : getPage()).error(e);
    }

    /** Create a new configuration object. */
    protected abstract Class getServiceClass();

    /** Do pre validation before a configuration object is removed. */
    protected abstract void validateRemoveConfig(T config) throws SecurityConfigException;

    /** Remove configuration. */
    protected abstract void removeConfig(T config) throws Exception;

    SecurityNamedServicePanelInfo lookupPageInfo(SecurityNamedServiceConfig config) {
        Class<?> serviceClass = null;
        try {
            serviceClass = Class.forName(config.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<SecurityNamedServicePanelInfo> pageInfos = new ArrayList<>();
        for (SecurityNamedServicePanelInfo pageInfo :
                GeoServerApplication.get().getBeansOfType(SecurityNamedServicePanelInfo.class)) {
            Class<?> pageClass = pageInfo.getServiceClass();
            if (pageClass.isAssignableFrom(serviceClass)) {
                pageInfos.add(pageInfo);
            }
        }

        if (pageInfos.isEmpty()) {
            throw new RuntimeException(
                    "Unable to find page info for service config: "
                            + config
                            + ", service class: "
                            + serviceClass);
        }
        if (pageInfos.size() > 1) {
            // filter by strict equals
            List<SecurityNamedServicePanelInfo> l = new ArrayList<>(pageInfos);
            for (Iterator<SecurityNamedServicePanelInfo> it = l.iterator(); it.hasNext(); ) {
                if (!it.next().getServiceClass().equals(serviceClass)) {
                    it.remove();
                }
            }
            if (l.size() == 1) {
                // filter down to one match
                return l.get(0);
            }
            throw new RuntimeException(
                    "Found multiple page infos for service config: "
                            + config
                            + ", service class: "
                            + serviceClass);
        }

        // found just one
        return pageInfos.get(0);
    }

    void goToPage(SecurityNamedServicePanelInfo pageInfo, IModel model) {
        // instantiate the page
        try {
            Class<?> cc = pageInfo.getComponentClass();
            AbstractSecurityPage editPage =
                    (AbstractSecurityPage) cc.getConstructor(IModel.class).newInstance(model);
            editPage.setReturnPage(getPage());
            setResponsePage(editPage);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create page for page info: " + pageInfo, e);
        }
    }

    class SecurityNamedServiceTablePanel<T extends SecurityNamedServiceConfig>
            extends GeoServerTablePanel<T> {

        public SecurityNamedServiceTablePanel(
                String id, SecurityNamedServiceProvider<T> dataProvider) {
            super(id, dataProvider, true);
        }

        @Override
        protected Component getComponentForProperty(
                String id, IModel<T> itemModel, Property<T> property) {
            if (property == NAME) {
                return createEditLink(id, itemModel, property);
            }

            if (property instanceof ResourceBeanProperty) {
                Object val = property.getModel(itemModel).getObject();
                if (val != null) {
                    return new Label(id, new ResourceModel(val.toString(), val.toString()));
                }
            }

            // backback to just a label
            return new Label(id, property.getModel(itemModel));
        }

        Component createEditLink(String id, final IModel<T> model, Property<T> property) {
            @SuppressWarnings("unchecked")
            IModel<Object> cast = (IModel<Object>) property.getModel(model);
            return new SimpleAjaxLink<Object>(id, cast) {

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    SecurityNamedServiceEditPage<T> editPage =
                            new SecurityNamedServiceEditPage<>(model);

                    editPage.setReturnPage(getPage());
                    setResponsePage(editPage);
                }
            };
        }
    }

    protected void onBeforeRender() {
        tablePanel.clearSelection();
        removeLink.setEnabled(false);
        super.onBeforeRender();
    };
}
