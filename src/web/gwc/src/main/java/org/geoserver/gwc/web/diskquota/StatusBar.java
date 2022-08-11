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

package org.geoserver.gwc.web.diskquota;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

public class StatusBar extends Panel {

    private static final long serialVersionUID = 1L;

    public StatusBar(
            final String id,
            final IModel<Number> limitModel,
            final IModel<Number> progressModel,
            final IModel<String> progressMessageModel) {
        super(id);
        setOutputMarkupId(true);
        add(
                new Behavior() {
                    private static final long serialVersionUID = -8058471260136015254L;

                    @Override
                    public void renderHead(Component component, IHeaderResponse response) {
                        response.render(
                                CssHeaderItem.forReference(
                                        new PackageResourceReference(
                                                StatusBar.class, "statusbar.css")));
                    }
                });

        WebMarkupContainer usageBar = new WebMarkupContainer("statusBarProgress");
        WebMarkupContainer excessBar = new WebMarkupContainer("statusBarExcess");

        final double limit = limitModel.getObject().doubleValue();
        final double used = progressModel.getObject().doubleValue();
        final double excess = used - limit;

        int usedPercentage;
        int excessPercentage;

        final int progressWidth = 200; // progress bar with, i.e. 100%

        if (excess > 0) {
            excessPercentage = (int) Math.round((excess * progressWidth) / used);
            usedPercentage = progressWidth - excessPercentage;
        } else {
            usedPercentage = (int) Math.round(used * progressWidth / limit);
            excessPercentage = 0;
        }

        usageBar.add(
                new AttributeModifier(
                        "style",
                        new Model<>(
                                "width: "
                                        + usedPercentage
                                        + "px; left: 5px; border-left: inherit;")));

        String redStyle =
                "width: " + excessPercentage + "px; left: " + (5 + usedPercentage) + "px;";
        excessBar.add(new AttributeModifier("style", new Model<>(redStyle)));

        add(usageBar);
        add(excessBar);
        add(new Label("progressLabel", progressMessageModel));

        // TODO:make the argument models truly dynamic
        // add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
    }
}
