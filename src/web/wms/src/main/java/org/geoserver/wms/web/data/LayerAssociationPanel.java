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

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Style page tab for configuring style-layer associations. Lists all layers, and allows setting the
 * current style as the default style for each layer, or as an associated style.
 */
public class LayerAssociationPanel extends StyleEditTabPanel {
    private static final long serialVersionUID = -59522993086560769L;

    private class LayerProvider extends GeoServerDataProvider<LayerInfo> {
        private static final long serialVersionUID = -1800971869092748431L;

        @Override
        public List<LayerInfo> getItems() {
            return getStylePage().getCatalog().getLayers();
        }

        Property<LayerInfo> workspace =
                new AbstractProperty<LayerInfo>("Workspace") {
                    private static final long serialVersionUID = -1851109132536014276L;

                    @Override
                    public Object getPropertyValue(LayerInfo x) {
                        return x.getResource().getStore().getWorkspace().getName();
                    }
                };

        Property<LayerInfo> layer =
                new AbstractProperty<LayerInfo>("Layer") {
                    private static final long serialVersionUID = -1041914399204405146L;

                    @Override
                    public Object getPropertyValue(LayerInfo x) {
                        return x.getName();
                    }
                };

        Property<LayerInfo> defaultStyle =
                new AbstractProperty<LayerInfo>("Default") {

                    @Override
                    public Object getPropertyValue(LayerInfo x) {
                        return defaultEditedStyle(x);
                    }
                };

        Property<LayerInfo> associatedStyle =
                new AbstractProperty<LayerInfo>("Associated") {
                    private static final long serialVersionUID = 890930107903888545L;

                    @Override
                    public Object getPropertyValue(LayerInfo x) {
                        return usesEditedStyle(x);
                    }
                };

        @Override
        public List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(workspace, layer, defaultStyle, associatedStyle);
        }
    }

    protected Boolean usesEditedStyle(LayerInfo l) {
        for (StyleInfo s : l.getStyles()) {
            if (s.getName().equals(getStylePage().getStyleInfo().getName())) return true;
        }
        return false;
    }

    protected Boolean defaultEditedStyle(LayerInfo l) {
        StyleInfo s = l.getDefaultStyle();
        if (s != null) {
            return s.getName().equals(getStylePage().getStyleInfo().getName());
        }
        return false;
    }

    public LayerAssociationPanel(String id, AbstractStylePage parent) {
        super(id, parent);

        final LayerProvider layerProvider = new LayerProvider();

        GeoServerTablePanel<LayerInfo> layerTable =
                new GeoServerTablePanel<LayerInfo>("layer.table", layerProvider) {
                    private static final long serialVersionUID = 6100831799966767858L;

                    @Override
                    public Component getComponentForProperty(
                            String id, IModel<LayerInfo> value, Property<LayerInfo> property) {
                        final LayerInfo layer = value.getObject();
                        String text = property.getPropertyValue(layer).toString();
                        if (property == layerProvider.defaultStyle) {
                            IModel<Boolean> model =
                                    new IModel<java.lang.Boolean>() {
                                        private static final long serialVersionUID =
                                                -5895600269146950033L;

                                        public Boolean getObject() {
                                            return defaultEditedStyle(layer);
                                        }

                                        public void setObject(java.lang.Boolean b) {
                                            if (b) {
                                                layer.setDefaultStyle(parent.getStyleInfo());
                                            } else {
                                                if (layer.getStyles().size() == 0) {
                                                    layer.setDefaultStyle(
                                                            parent.getCatalog()
                                                                    .getStyleByName("generic"));
                                                } else {
                                                    StyleInfo s =
                                                            layer.getStyles().iterator().next();
                                                    layer.setDefaultStyle(s);
                                                }
                                            }
                                            parent.getCatalog().save(layer);
                                        }

                                        public void detach() {}
                                    };

                            Fragment fragment =
                                    new Fragment(
                                            id,
                                            "layer.default.checkbox",
                                            LayerAssociationPanel.this);
                            fragment.add(
                                    new AjaxCheckBox("default.selected", model) {
                                        private static final long serialVersionUID =
                                                3572882767660629935L;

                                        public void onUpdate(AjaxRequestTarget target) {}
                                    });
                            return fragment;
                        } else if (property == layerProvider.associatedStyle) {
                            IModel<Boolean> model =
                                    new IModel<java.lang.Boolean>() {
                                        private static final long serialVersionUID =
                                                -5895600269146950033L;

                                        public Boolean getObject() {
                                            return usesEditedStyle(layer);
                                        }

                                        public void setObject(java.lang.Boolean b) {
                                            if (b) {
                                                layer.getStyles().add(parent.getStyleInfo());
                                            } else {
                                                StyleInfo s = null;
                                                for (StyleInfo candidate : layer.getStyles()) {
                                                    if (candidate
                                                            .getName()
                                                            .equals(
                                                                    parent.getStyleInfo()
                                                                            .getName())) {
                                                        s = candidate;
                                                        break;
                                                    }
                                                }
                                                if (s != null) layer.getStyles().remove(s);
                                            }
                                            parent.getCatalog().save(layer);
                                        }

                                        public void detach() {}
                                    };

                            Fragment fragment =
                                    new Fragment(
                                            id,
                                            "layer.association.checkbox",
                                            LayerAssociationPanel.this);
                            fragment.add(
                                    new AjaxCheckBox("association.selected", model) {
                                        private static final long serialVersionUID =
                                                3572882767660629935L;

                                        public void onUpdate(AjaxRequestTarget target) {}
                                    });
                            return fragment;
                        } else {
                            return new Label(id, text);
                        }
                    };
                };
        add(layerTable);
    }
}
