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

package org.geoserver.web.wicket;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * A panel with two arrows, up and down, supposed to reorder items in a container (a table)
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public class UpDownPanel<T extends Object> extends Panel {

    private static final long serialVersionUID = -5964561496724645286L;
    T entry;
    private ImageAjaxLink<?> upLink;

    private ImageAjaxLink<?> downLink;

    private Component container;

    public UpDownPanel(
            String id,
            final T entry,
            final List<T> items,
            Component container,
            final StringResourceModel upTitle,
            final StringResourceModel downTitle) {
        super(id);
        this.entry = entry;
        this.setOutputMarkupId(true);
        this.container = container;

        upLink =
                new ImageAjaxLink<Void>(
                        "up",
                        new PackageResourceReference(
                                getClass(), "../img/icons/silk/arrow_up.png")) {
                    private static final long serialVersionUID = 2377129539852597050L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = items.indexOf(UpDownPanel.this.entry);
                        items.remove(index);
                        items.add(Math.max(0, index - 1), UpDownPanel.this.entry);
                        target.add(UpDownPanel.this.container);
                        target.add(this);
                        target.add(downLink);
                        target.add(upLink);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.put("title", upTitle.getString());
                        if (items.indexOf(entry) == 0) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        upLink.getImage().add(new AttributeModifier("alt", new ParamResourceModel("up", upLink)));
        upLink.setOutputMarkupId(true);
        add(upLink);

        downLink =
                new ImageAjaxLink<Void>(
                        "down",
                        new PackageResourceReference(
                                getClass(), "../img/icons/silk/arrow_down.png")) {
                    private static final long serialVersionUID = -1770135905138092575L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = items.indexOf(UpDownPanel.this.entry);
                        items.remove(index);
                        items.add(Math.min(items.size(), index + 1), UpDownPanel.this.entry);
                        target.add(UpDownPanel.this.container);
                        target.add(this);
                        target.add(downLink);
                        target.add(upLink);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.put("title", downTitle.getString());
                        if (items.indexOf(entry) == items.size() - 1) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        downLink.getImage()
                .add(new AttributeModifier("alt", new ParamResourceModel("down", downLink)));
        downLink.setOutputMarkupId(true);
        add(downLink);
    }
}
