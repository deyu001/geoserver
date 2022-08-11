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

package org.geoserver.security.web.service;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** A page listing data access rules, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class ServiceAccessRulePage extends AbstractSecurityPage {

    private GeoServerTablePanel<ServiceAccessRule> rules;

    private SelectionServiceRemovalLink removal;

    public ServiceAccessRulePage() {

        ServiceAccessRuleProvider provider = new ServiceAccessRuleProvider();
        add(
                rules =
                        new GeoServerTablePanel<ServiceAccessRule>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<ServiceAccessRule> itemModel,
                                    Property<ServiceAccessRule> property) {
                                if (property == ServiceAccessRuleProvider.RULEKEY) {
                                    return editRuleLink(id, itemModel, property);
                                }
                                if (property == ServiceAccessRuleProvider.ROLES) {
                                    return new Label(id, property.getModel(itemModel));
                                }
                                throw new RuntimeException("Uknown property " + property);
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                removal.setEnabled(rules.getSelection().size() > 0);
                                target.add(removal);
                            }
                        });
        rules.setOutputMarkupId(true);

        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(
                new BookmarkablePageLink<NewServiceAccessRulePage>(
                        "addNew", NewServiceAccessRulePage.class));

        // the removal button
        header.add(removal = new SelectionServiceRemovalLink("removeSelected", rules, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    Component editRuleLink(
            String id, IModel<ServiceAccessRule> itemModel, Property<ServiceAccessRule> property) {
        return new SimpleAjaxLink<ServiceAccessRule>(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(
                        new EditServiceAccessRulePage((ServiceAccessRule) getDefaultModelObject()));
            }
        };
    }
}
