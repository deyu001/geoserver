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

package org.geoserver.security.web.auth;

import static org.geoserver.security.web.auth.SecurityFilterChainProvider.NAME;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.HtmlLoginFilterChain;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.ServiceLoginFilterChain;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/**
 * Panel for modifiy security filter chains
 *
 * @author christian
 */
public class SecurityFilterChainsPanel extends Panel {

    SecurityFilterChainTablePanel tablePanel;
    FeedbackPanel feedbackPanel;
    GeoServerDialog dialog;
    SecurityManagerConfig secMgrConfig;

    public SecurityFilterChainsPanel(String id, SecurityManagerConfig secMgrConfig) {
        super(id);
        this.secMgrConfig = secMgrConfig;

        final boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole();
        add(
                new AjaxLink("addServiceChain") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // create a new config class and instantiate the page
                        // TODO, switch between ServiceLoginFilter/HtmlLogin
                        SecurityFilterChainPage newPage =
                                new SecurityVariableFilterChainPage(
                                        new ServiceLoginFilterChain(),
                                        SecurityFilterChainsPanel.this.secMgrConfig,
                                        true);
                        newPage.setReturnPage(getPage());
                        setResponsePage(newPage);
                    }
                }.setEnabled(isAdmin));

        add(
                new AjaxLink("addHtmlChain") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // create a new config class and instantiate the page
                        SecurityFilterChainPage newPage =
                                new SecurityVariableFilterChainPage(
                                        new HtmlLoginFilterChain(),
                                        SecurityFilterChainsPanel.this.secMgrConfig,
                                        true);
                        newPage.setReturnPage(getPage());
                        setResponsePage(newPage);
                    }
                }.setEnabled(isAdmin));

        SecurityFilterChainProvider dataProvider = new SecurityFilterChainProvider(secMgrConfig);
        add(
                tablePanel =
                        new SecurityFilterChainTablePanel("table", dataProvider) {
                            private static final long serialVersionUID = 1L;

                            //            @Override
                            //            protected void onSelectionUpdate(AjaxRequestTarget target)
                            // {
                            //                if (isAdmin) {
                            //
                            // target.add(removeLink.setEnabled(!getSelection().isEmpty()));
                            //                }
                            //            }
                        });
        tablePanel.setOutputMarkupId(true);
        tablePanel.setFilterable(false);
        tablePanel.setSortable(false);
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
        Serializable msg = null;
        if (e instanceof SecurityConfigException) {
            SecurityConfigException sce = (SecurityConfigException) e;
            msg =
                    new StringResourceModel("security." + sce.getId())
                            .setParameters(sce.getArgs())
                            .getObject();
        } else {
            msg = e;
        }

        (target != null ? target : getPage()).error(msg);
    }

    class SecurityFilterChainTablePanel extends GeoServerTablePanel<RequestFilterChain> {

        public SecurityFilterChainTablePanel(String id, SecurityFilterChainProvider dataProvider) {
            super(id, dataProvider, false);
        }

        Component createEditLink(
                String id,
                final IModel<RequestFilterChain> model,
                final Property<RequestFilterChain> property) {
            @SuppressWarnings("unchecked")
            IModel<String> cast = (IModel<String>) property.getModel(model);
            return new SimpleAjaxLink<String>(id, cast) {

                String chainName = (String) property.getModel(model).getObject();

                @Override
                protected void onClick(AjaxRequestTarget target) {

                    RequestFilterChain chain =
                            SecurityFilterChainsPanel.this
                                    .secMgrConfig
                                    .getFilterChain()
                                    .getRequestChainByName(chainName);

                    SecurityFilterChainPage editPage = null;
                    if (chain instanceof VariableFilterChain)
                        editPage =
                                new SecurityVariableFilterChainPage(
                                        ((VariableFilterChain) chain),
                                        SecurityFilterChainsPanel.this.secMgrConfig,
                                        false);
                    else
                        editPage =
                                new SecurityFilterChainPage(
                                        chain, SecurityFilterChainsPanel.this.secMgrConfig, false);

                    editPage.setReturnPage(getPage());
                    setResponsePage(editPage);
                }
            };
        }

        @Override
        protected Component getComponentForProperty(
                String id,
                IModel<RequestFilterChain> itemModel,
                Property<RequestFilterChain> property) {

            if (property == NAME) {
                return createEditLink(id, itemModel, property);
            }

            if (property == SecurityFilterChainProvider.POSITION) {
                return positionPanel(id, itemModel);
            }

            if (property == SecurityFilterChainProvider.REMOVE) {
                return removeLink(id, itemModel);
            }

            if (Boolean.TRUE.equals(property.getModel(itemModel).getObject()))
                return new Icon(id, CatalogIconFactory.ENABLED_ICON);

            if (Boolean.FALSE.equals(property.getModel(itemModel).getObject()))
                return new Label(id, "");

            return new Label(id, property.getModel(itemModel));
        }
    }

    Component removeLink(String id, IModel itemModel) {
        final RequestFilterChain chain = (RequestFilterChain) itemModel.getObject();

        if (chain.canBeRemoved() == false) {
            ImageAjaxLink blankLink =
                    new ImageAjaxLink(
                            id,
                            new PackageResourceReference(getClass(), "../img/icons/blank.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {}
                    };
            blankLink.getImage().add(new AttributeModifier("alt", new Model<>("")));
            add(blankLink);
            return blankLink;
        }

        ImageAjaxLink link =
                new ImageAjaxLink(
                        id,
                        new PackageResourceReference(getClass(), "../img/icons/silk/delete.png")) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        secMgrConfig.getFilterChain().getRequestChains().remove(chain);
                        target.add(tablePanel);
                    }
                };
        link.getImage()
                .add(
                        new AttributeModifier(
                                "alt",
                                new ParamResourceModel("LayerGroupEditPage.th.remove", link)));
        return link;
    }

    Component positionPanel(String id, IModel itemModel) {
        return new PositionPanel(id, (RequestFilterChain) itemModel.getObject());
    }

    class PositionPanel extends Panel {

        List<RequestFilterChain> getChains() {
            return secMgrConfig.getFilterChain().getRequestChains();
        }

        RequestFilterChain theChain;
        private ImageAjaxLink upLink;
        private ImageAjaxLink downLink;

        public PositionPanel(String id, final RequestFilterChain chain) {
            super(id);
            this.theChain = chain;
            this.setOutputMarkupId(true);

            upLink =
                    new ImageAjaxLink(
                            "up",
                            new PackageResourceReference(
                                    getClass(), "../img/icons/silk/arrow_up.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            int index = getChains().indexOf(PositionPanel.this.theChain);
                            getChains().remove(index);
                            getChains().add(Math.max(0, index - 1), PositionPanel.this.theChain);
                            target.add(tablePanel);
                            target.add(this);
                            target.add(downLink);
                            target.add(upLink);
                        }

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (getChains().indexOf(theChain) == 0) {
                                tag.put("style", "visibility:hidden");
                            } else {
                                tag.put("style", "visibility:visible");
                            }
                        }
                    };
            upLink.getImage()
                    .add(
                            new AttributeModifier(
                                    "alt",
                                    new ParamResourceModel(
                                            "SecurityFilterChainsPanel.th.up", upLink)));
            upLink.setOutputMarkupId(true);
            add(upLink);

            downLink =
                    new ImageAjaxLink(
                            "down",
                            new PackageResourceReference(
                                    getClass(), "../img/icons/silk/arrow_down.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            int index = getChains().indexOf(PositionPanel.this.theChain);
                            getChains().remove(index);
                            getChains()
                                    .add(
                                            Math.min(getChains().size(), index + 1),
                                            PositionPanel.this.theChain);
                            target.add(tablePanel);
                            target.add(this);
                            target.add(downLink);
                            target.add(upLink);
                        }

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (getChains().indexOf(theChain) == getChains().size() - 1) {
                                tag.put("style", "visibility:hidden");
                            } else {
                                tag.put("style", "visibility:visible");
                            }
                        }
                    };
            downLink.getImage()
                    .add(
                            new AttributeModifier(
                                    "alt",
                                    new ParamResourceModel(
                                            "SecurityFilterChainsPanel.th.down", downLink)));
            downLink.setOutputMarkupId(true);
            add(downLink);
        }
    }
}
