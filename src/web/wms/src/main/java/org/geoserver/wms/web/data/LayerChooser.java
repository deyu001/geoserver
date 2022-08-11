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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Panel for selecting a layer from the list of layers. Used by {@link LayerAttributePanel} and
 * {@link OpenLayersPreviewPanel} to change the preview layer.
 */
public class LayerChooser extends Panel {

    private static final long serialVersionUID = -127345071729297975L;

    private static class LayerProvider extends GeoServerDataProvider<LayerInfo> {
        private static final long serialVersionUID = -2117784735301652240L;

        private AbstractStylePage parent;

        public LayerProvider(AbstractStylePage parent) {
            this.parent = parent;
        }

        public static Property<LayerInfo> workspace =
                new AbstractProperty<LayerInfo>("Workspace") {
                    private static final long serialVersionUID = -7055816211775541759L;

                    public Object getPropertyValue(LayerInfo x) {
                        return x.getResource().getStore().getWorkspace().getName();
                    }
                };

        public static Property<LayerInfo> store =
                new AbstractProperty<LayerInfo>("Store") {
                    private static final long serialVersionUID = -4021230907568644439L;

                    public Object getPropertyValue(LayerInfo x) {
                        return x.getResource().getStore().getName();
                    }
                };

        public static Property<LayerInfo> name =
                new AbstractProperty<LayerInfo>("Layer") {
                    private static final long serialVersionUID = 8913729089849537790L;

                    public Object getPropertyValue(LayerInfo x) {
                        return x.getName();
                    }
                };

        @Override
        public List<LayerInfo> getItems() {
            List<LayerInfo> items = new ArrayList<>();
            for (LayerInfo l : parent.getCatalog().getLayers()) {
                if (l.getResource() instanceof FeatureTypeInfo) {
                    items.add(l);
                }
                if (l.getResource() instanceof CoverageInfo) {
                    items.add(l);
                }
            }
            return items;
        }

        @Override
        public List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(workspace, store, name);
        }
    }

    public LayerChooser(final String id, final AbstractStylePage parent) {
        super(id);
        LayerProvider provider = new LayerProvider(parent);
        GeoServerTablePanel<LayerInfo> table =
                new GeoServerTablePanel<LayerInfo>("layer.table", provider) {
                    private static final long serialVersionUID = 1196129584558094662L;

                    @Override
                    public Component getComponentForProperty(
                            String id, IModel<LayerInfo> value, Property<LayerInfo> property) {
                        final LayerInfo layer = value.getObject();
                        final String text = property.getPropertyValue(layer).toString();

                        if (property == LayerProvider.name) {
                            return new Fragment(id, "layer.link", LayerChooser.this) {
                                private static final long serialVersionUID = -7619814477490657757L;

                                {
                                    add(
                                            new GeoServerAjaxFormLink("link", parent.styleForm) {
                                                {
                                                    add(new Label("layer.name", new Model<>(text)));
                                                }

                                                private static final long serialVersionUID =
                                                        8020574396677784792L;

                                                @Override
                                                protected void onClick(
                                                        AjaxRequestTarget target, Form<?> form) {
                                                    parent.getLayerModel().setObject(layer);
                                                    parent.getPopup().close(target);
                                                    parent.configurationChanged();
                                                    parent.addFeedbackPanels(target);
                                                    target.add(parent.styleForm);
                                                }

                                                @Override
                                                public boolean getDefaultFormProcessing() {
                                                    return false;
                                                }
                                            });
                                }
                            };
                        } else {
                            return new Label(id, text);
                        }
                    }
                };
        add(table);
    }
}
