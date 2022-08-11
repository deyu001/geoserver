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

package org.geoserver.wms.web.publish;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.publish.PublishedConfigurationPanel;

/** Configures a {@link LayerInfo} geo-search related metadata */
public class AttributionLayerConfigPanel extends PublishedConfigurationPanel<PublishedInfo> {

    private static final long serialVersionUID = -5229831547353122190L;

    public AttributionLayerConfigPanel(String id, IModel<? extends PublishedInfo> model) {
        super(id, model);

        PublishedInfo layer = model.getObject();

        if (layer.getAttribution() == null) {
            layer.setAttribution(
                    GeoServerApplication.get().getCatalog().getFactory().createAttribution());
        }

        add(
                new TextField<>(
                        "wms.attribution.title", new PropertyModel<>(model, "attribution.title")));

        final TextField<String> href =
                new TextField<>(
                        "wms.attribution.href", new PropertyModel<>(model, "attribution.href"));
        href.add(new UrlValidator());
        href.setOutputMarkupId(true);
        add(href);

        final TextField<String> logo =
                new TextField<>(
                        "wms.attribution.logo", new PropertyModel<>(model, "attribution.logoURL"));
        logo.add(new UrlValidator());
        logo.setOutputMarkupId(true);
        add(logo);

        final TextField<String> type =
                new TextField<>(
                        "wms.attribution.type", new PropertyModel<>(model, "attribution.logoType"));
        type.setOutputMarkupId(true);
        add(type);

        final TextField<Integer> height =
                new TextField<>(
                        "wms.attribution.height",
                        new PropertyModel<>(model, "attribution.logoHeight"),
                        Integer.class);
        height.add(RangeValidator.minimum(0));
        height.setOutputMarkupId(true);
        add(height);

        final TextField<Integer> width =
                new TextField<>(
                        "wms.attribution.width",
                        new PropertyModel<>(model, "attribution.logoWidth"),
                        Integer.class);
        width.add(RangeValidator.minimum(0));
        width.setOutputMarkupId(true);
        add(width);

        add(
                new AjaxSubmitLink("verifyImage") {
                    private static final long serialVersionUID = 6814575194862084111L;

                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        if (logo.getDefaultModelObjectAsString() != null) {
                            try {
                                URL url = new URL(logo.getDefaultModelObjectAsString());
                                URLConnection conn = url.openConnection();
                                type.getModel().setObject(conn.getContentType());
                                BufferedImage image = ImageIO.read(conn.getInputStream());
                                height.setModelValue(new String[] {"" + image.getHeight()});
                                width.setModelValue(new String[] {"" + image.getWidth()});
                            } catch (Exception e) {
                            }
                        }

                        target.add(type);
                        target.add(height);
                        target.add(width);
                    }
                });
    }
}
