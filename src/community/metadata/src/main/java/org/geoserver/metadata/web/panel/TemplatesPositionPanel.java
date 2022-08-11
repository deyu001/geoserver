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

package org.geoserver.metadata.web.panel;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.web.MetadataTemplateTracker;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

public class TemplatesPositionPanel extends Panel {
    private static final long serialVersionUID = -4645368967597125299L;

    public TemplatesPositionPanel(
            String id,
            IModel<List<MetadataTemplate>> templates,
            MetadataTemplateTracker tracker,
            IModel<MetadataTemplate> model,
            GeoServerTablePanel<MetadataTemplate> tablePanel) {
        super(id, model);
        ImageAjaxLink<Object> upLink =
                new ImageAjaxLink<Object>(
                        "up",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/arrow_up.png")) {
                    private static final long serialVersionUID = -4165434301439054175L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = templates.getObject().indexOf(model.getObject());
                        tracker.switchTemplates(
                                model.getObject(), templates.getObject().get(index - 1));
                        templates.getObject().add(index - 1, templates.getObject().remove(index));
                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (templates.getObject().indexOf(model.getObject()) == 0) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        upLink.getImage()
                .add(
                        new AttributeModifier(
                                "alt", new ParamResourceModel("up", TemplatesPositionPanel.this)));
        add(upLink);

        ImageAjaxLink<Object> downLink =
                new ImageAjaxLink<Object>(
                        "down",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/arrow_down.png")) {
                    private static final long serialVersionUID = -8005026702401617344L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = templates.getObject().indexOf(model.getObject());
                        tracker.switchTemplates(
                                model.getObject(), templates.getObject().get(index + 1));
                        templates.getObject().add(index + 1, templates.getObject().remove(index));

                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (templates.getObject().indexOf(model.getObject())
                                == templates.getObject().size() - 1) {
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
                                new ParamResourceModel("down", TemplatesPositionPanel.this)));
        add(downLink);
    }
}
