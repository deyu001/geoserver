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

package org.geoserver.wms.web.data;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.util.logging.Logging;

/**
 * Style page tab for displaying layer attributes. Includes a link for changing the current preview
 * layer. Delegates to {@link BandsPanel} or {@link LayerAttributePanel} to display the attributes,
 * depending on the type of the layer resource.
 */
public class LayerAttributePanel extends StyleEditTabPanel {

    static final Logger LOGGER = Logging.getLogger(LayerAttributePanel.class);

    private static final long serialVersionUID = -5936224477909623317L;

    public LayerAttributePanel(String id, AbstractStylePage parent) throws IOException {
        super(id, parent);

        // Change layer link
        PropertyModel<String> layerNameModel =
                new PropertyModel<>(parent.getLayerModel(), "prefixedName");
        add(
                new SimpleAjaxLink<String>("changeLayer", layerNameModel) {
                    private static final long serialVersionUID = 7341058018479354596L;

                    public void onClick(AjaxRequestTarget target) {
                        ModalWindow popup = parent.getPopup();

                        popup.setInitialHeight(400);
                        popup.setInitialWidth(600);
                        popup.setTitle(new Model<>("Choose layer to edit"));
                        popup.setContent(new LayerChooser(popup.getContentId(), parent));
                        popup.show(target);
                    }
                });

        this.setDefaultModel(parent.getLayerModel());

        updateAttributePanel();
    }

    @Override
    protected void configurationChanged() {
        try {
            updateAttributePanel();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not update LayerAttributePanel", e);
        }
    }

    protected void updateAttributePanel() throws IOException {
        ResourceInfo resource = this.getStylePage().getLayerInfo().getResource();

        if (this.get("attributePanel") != null) {
            this.remove("attributePanel");
        }
        if (resource instanceof FeatureTypeInfo) {
            this.add(new DataPanel("attributePanel", (FeatureTypeInfo) resource));
        } else if (resource instanceof CoverageInfo) {
            this.add(new BandsPanel("attributePanel", (CoverageInfo) resource));
        }
    }
}
