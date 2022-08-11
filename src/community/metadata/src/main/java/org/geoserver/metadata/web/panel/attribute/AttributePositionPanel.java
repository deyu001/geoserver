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

package org.geoserver.metadata.web.panel.attribute;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class AttributePositionPanel extends Panel {
    private static final long serialVersionUID = -4645368967597125299L;

    public AttributePositionPanel(
            String id,
            IModel<ComplexMetadataMap> mapModel,
            AttributeConfiguration attConfig,
            int index,
            List<Integer> derivedAtts,
            GeoServerTablePanel<?> tablePanel) {
        super(id, mapModel);
        AjaxSubmitLink upLink =
                new AjaxSubmitLink("up") {
                    private static final long serialVersionUID = -4165434301439054175L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        moveUpOrDown(mapModel, attConfig, index, -1, tablePanel);
                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (index == 0
                                || derivedAtts != null
                                        && (derivedAtts.contains(index)
                                                || derivedAtts.contains(index - 1))) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        upLink.add(
                new Image(
                                "upImage",
                                new PackageResourceReference(
                                        GeoServerBasePage.class, "img/icons/silk/arrow_up.png"))
                        .add(
                                new AttributeModifier(
                                        "alt",
                                        new ParamResourceModel(
                                                "up", AttributePositionPanel.this))));
        add(upLink);

        AjaxSubmitLink downLink =
                new AjaxSubmitLink("down") {
                    private static final long serialVersionUID = -8005026702401617344L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        moveUpOrDown(mapModel, attConfig, index, 1, tablePanel);

                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (index == mapModel.getObject().size(attConfig.getKey()) - 1
                                || derivedAtts != null
                                        && (derivedAtts.contains(index)
                                                || derivedAtts.contains(index + 1))) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        downLink.add(
                new Image(
                                "downImage",
                                new PackageResourceReference(
                                        GeoServerBasePage.class, "img/icons/silk/arrow_down.png"))
                        .add(
                                new AttributeModifier(
                                        "alt",
                                        new ParamResourceModel(
                                                "down", AttributePositionPanel.this))));
        add(downLink);
    }

    public void moveUpOrDown(
            IModel<ComplexMetadataMap> mapModel,
            AttributeConfiguration attConfig,
            int index,
            int diff,
            GeoServerTablePanel<?> tablePanel) {

        if (attConfig.getFieldType() == FieldTypeEnum.COMPLEX) {
            ComplexMetadataService service =
                    GeoServerApplication.get()
                            .getApplicationContext()
                            .getBean(ComplexMetadataService.class);

            ComplexMetadataMap other =
                    mapModel.getObject().subMap(attConfig.getKey(), index + diff);
            ComplexMetadataMap current = mapModel.getObject().subMap(attConfig.getKey(), index);

            ComplexMetadataMap old = current.clone();
            service.copy(other, current, attConfig.getTypename());
            service.copy(old, other, attConfig.getTypename());

        } else {
            ComplexMetadataAttribute<Serializable> other =
                    mapModel.getObject().get(Serializable.class, attConfig.getKey(), index + diff);
            ComplexMetadataAttribute<Serializable> current =
                    mapModel.getObject().get(Serializable.class, attConfig.getKey(), index);

            Serializable old = current.getValue();
            current.setValue(other.getValue());
            other.setValue(old);
        }
    }
}
