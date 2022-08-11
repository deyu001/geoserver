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

package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.wms.eo.EoLayerType;

/** Allows to edit the Band Coverage Layer */
@SuppressWarnings("serial")
public class BandsLayerEntryPanel extends Panel {

    private LayerInfo layer;
    private StyleInfo layerStyle;

    @SuppressWarnings({"rawtypes"})
    public BandsLayerEntryPanel(String id, final Form form, WorkspaceInfo workspace) {
        super(id);
        setOutputMarkupId(true);

        LayerGroupInfo group = (LayerGroupInfo) form.getModel().getObject();

        int pos = 0;
        for (PublishedInfo p : group.getLayers()) {
            if (p instanceof LayerInfo) {
                if (EoLayerType.BAND_COVERAGE
                        .name()
                        .equals(((LayerInfo) p).getMetadata().get(EoLayerType.KEY))) {
                    layer = (LayerInfo) p;
                    layerStyle = group.getStyles().get(pos);
                }
            }
            pos++;
        }

        Link link =
                new Link("bandsLayer") {
                    @Override
                    public void onClick() {
                        PageParameters pp = new PageParameters();
                        if (layer.getResource().getStore().getWorkspace() != null) {
                            pp.add(
                                    ResourceConfigurationPage.WORKSPACE,
                                    layer.getResource().getStore().getWorkspace().getName());
                        }
                        pp.add(ResourceConfigurationPage.NAME, layer.getName());
                        setResponsePage(ResourceConfigurationPage.class, pp);
                    }
                };
        link.add(new Label("bandsLayerName", new PropertyModel(layer, "name")));
        add(link);

        // global styles
        List<StyleInfo> globalStyles = new ArrayList<StyleInfo>();
        List<StyleInfo> allStyles = GeoServerApplication.get().getCatalog().getStyles();
        for (StyleInfo s : allStyles) {
            if (s.getWorkspace() == null) {
                globalStyles.add(s);
            }
        }

        // available styles
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        styles.addAll(globalStyles);
        if (workspace != null) {
            styles.addAll(GeoServerApplication.get().getCatalog().getStylesByWorkspace(workspace));
        }
        Collections.sort(styles, new StyleInfoNameComparator());

        DropDownChoice<StyleInfo> styleField =
                new DropDownChoice<StyleInfo>(
                        "bandsLayerStyle",
                        new PropertyModel<StyleInfo>(this, "layerStyle"),
                        styles) {
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return form.getConverter(type);
                    }
                };
        styleField.setNullValid(true);
        styleField.setOutputMarkupId(true);
        add(styleField);
    }

    public void setLayer(LayerInfo layer) {
        this.layer = layer;
    }

    public void setLayerStyle(StyleInfo layerStyle) {
        this.layerStyle = layerStyle;
    }

    public LayerInfo getLayer() {
        return layer;
    }

    public StyleInfo getLayerStyle() {
        return layerStyle;
    }
}
