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

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.GeoServerSecurityService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.GeoServerApplication;

/**
 * New page for specific class of named security service.
 *
 * <p>Most of the work is delegated to {@link SecurityNamedServicePanelInfo} and {@link
 * SecurityNamedServicePanel}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SecurityNamedServiceNewPage<
                S extends GeoServerSecurityService, T extends SecurityNamedServiceConfig>
        extends SecurityNamedServicePage<T> {

    Form<T> form;
    WebMarkupContainer panelContainer;

    public SecurityNamedServiceNewPage(Class<S> serviceClass) {
        // keys that allow us to dynamically set the page title and description based on
        // type of service class / extension point
        add(new Label("title1", createTitleModel(serviceClass).getString()));
        add(new Label("title2", createTitleModel(serviceClass).getString()));

        List<SecurityNamedServicePanelInfo> panelInfos = lookupPanelInfos(serviceClass);

        AjaxLinkGroup<SecurityNamedServicePanelInfo> serviceLinks =
                new AjaxLinkGroup<SecurityNamedServicePanelInfo>("services", panelInfos) {

                    @Override
                    protected void populateItem(ListItem<SecurityNamedServicePanelInfo> item) {
                        SecurityNamedServicePanelInfo panelInfo = item.getModelObject();
                        item.add(
                                newLink("link", item.getModel())
                                        .add(new Label("title", createShortTitleModel(panelInfo)))
                                        .setEnabled(item.getIndex() > 0));
                        item.add(new Label("description", createDescriptionModel(panelInfo)));
                    }

                    @Override
                    protected void onClick(
                            AjaxLink<SecurityNamedServicePanelInfo> link,
                            AjaxRequestTarget target) {
                        updatePanel(link.getModelObject(), target);
                    }
                };

        add(new WebMarkupContainer("servicesContainer").add(serviceLinks).setOutputMarkupId(true));

        add(form = new Form<>("form"));

        // add a container for the actual panel, since we will dynamically update it
        form.add(panelContainer = new WebMarkupContainer("panel"));
        panelContainer.setOutputMarkupId(true);

        form.add(
                new SubmitLink("save", form) {
                    @Override
                    public void onSubmit() {
                        handleSubmit(getForm());
                    }
                });
        form.add(
                new Link("cancel") {
                    @Override
                    public void onClick() {
                        doReturn();
                    }
                });

        updatePanel(panelInfos.get(0), null);
    }

    void updatePanel(SecurityNamedServicePanelInfo panelInfo, AjaxRequestTarget target) {
        // create a new config object
        T config = getConfig(panelInfo);

        config.setClassName(panelInfo.getServiceClass().getCanonicalName());

        // update the form model
        form.setModel(new CompoundPropertyModel<>(config));

        // create the new panel
        panel = createPanel("content", panelInfo, new Model<>(config));

        // remove the old panel if it is there
        if (panelContainer.get("content") != null) {
            panelContainer.remove("content");
        }
        panelContainer.add(panel);

        if (target != null) {
            target.add(panelContainer);
        }
    }

    @SuppressWarnings("unchecked")
    private T getConfig(SecurityNamedServicePanelInfo panelInfo) {
        try {
            return (T) panelInfo.getServiceConfigClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new WicketRuntimeException(
                    "Unable to create config class: " + panelInfo.getServiceConfigClass(), e);
        }
    }

    List<SecurityNamedServicePanelInfo> lookupPanelInfos(Class<S> serviceClass) {

        List<SecurityNamedServicePanelInfo> panelInfos = new ArrayList<>();
        for (SecurityNamedServicePanelInfo pageInfo :
                GeoServerApplication.get().getBeansOfType(SecurityNamedServicePanelInfo.class)) {
            if (serviceClass.isAssignableFrom(pageInfo.getServiceClass())) {
                panelInfos.add(pageInfo);
            }
        }

        if (panelInfos.isEmpty()) {
            throw new RuntimeException(
                    "Unable to find panel info for service class: " + serviceClass);
        }

        return panelInfos;
    }

    abstract static class AjaxLinkGroup<T> extends ListView<T> {

        public AjaxLinkGroup(String id, List<T> list) {
            super(id, list);
        }

        public AjaxLinkGroup(String id) {
            super(id);
        }

        void init() {
            setOutputMarkupId(true);
        }

        protected AjaxLink<T> newLink(String id, IModel<T> model) {
            AjaxLink<T> result =
                    new AjaxLink<T>(id, model) {
                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            // set all links enabled
                            AjaxLinkGroup.this.visitChildren(
                                    AjaxLink.class,
                                    (component, visit) -> {
                                        component.setEnabled(true);
                                        target.add(component);
                                        visit.dontGoDeeper();
                                    });
                            // set this link disabled
                            setEnabled(false);

                            // update
                            // target.add(AjaxLinkGroup.this.getParent());
                            target.add(this);

                            AjaxLinkGroup.this.onClick(this, target);
                        }
                    };
            result.setOutputMarkupId(true);
            return result;
        }

        protected abstract void onClick(AjaxLink<T> link, AjaxRequestTarget target);
    }
}
