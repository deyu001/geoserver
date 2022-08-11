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

package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.gridset.GridSetListTablePanel;
import org.geoserver.gwc.web.gridset.GridSetTableProvider;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;

/**
 * Form component that edits the default {@link GWCConfig#getDefaultCachingGridSetIds() cached
 * gridsets} for {@link CachingOptionsPanel}.
 *
 * @author groldan
 */
class DefaultGridsetsEditor extends FormComponentPanel<Set<String>> {

    private static final long serialVersionUID = 5098470663723800345L;

    private final IModel<? extends List<String>> selection;

    private DefaultGridSetsTable defaultGridsetsTable;

    private final DropDownChoice<String> availableGridSets;

    private class DefaultGridSetsTable extends GridSetListTablePanel {
        private static final long serialVersionUID = -3301795024743630393L;

        public DefaultGridSetsTable(String id, GridSetTableProvider provider) {
            super(id, provider, false);
            setOutputMarkupId(true);
            setPageable(false);
            setFilterable(false);
        }

        @Override
        protected Component nameLink(final String id, final GridSet gridSet) {
            Label label = new Label(id, gridSet.getName());
            label.add(new AttributeModifier("title", new Model<>(gridSet.getDescription())));
            return label;
        }

        @Override
        protected Component actionLink(final String id, String gridSetName) {

            @SuppressWarnings("rawtypes")
            Component removeLink =
                    new ImageAjaxLink(id, GWCIconFactory.DELETE_ICON) {
                        private static final long serialVersionUID = 1L;

                        /** Removes the selected item from the provider's model */
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            final String gridsetName = getDefaultModelObjectAsString();
                            List<String> selection =
                                    DefaultGridsetsEditor.this.selection.getObject();
                            selection.remove(gridsetName);
                            List<String> choices = new ArrayList<>(availableGridSets.getChoices());
                            choices.add(gridsetName);
                            Collections.sort(choices);
                            availableGridSets.setChoices(choices);
                            target.add(defaultGridsetsTable);
                            target.add(availableGridSets);
                        }
                    };
            removeLink.setDefaultModel(new Model<>(gridSetName));

            return removeLink;
        }

        @Override
        protected Component getComponentForProperty(
                String id, IModel<GridSet> itemModel, Property<GridSet> property) {
            // Property objects are package access, so we can't statically reference them here
            // see org.geoserver.gwc.web.gridset.ACTION_LINK
            final String propertyName = property.getName();
            // the Remove link property name is the empty string. If that is the property name,
            // return the actionLink here.
            if (Strings.isEmpty(propertyName)) {
                return actionLink(id, itemModel.getObject().getName());
            }
            return null;
        }
    }

    public DefaultGridsetsEditor(final String id, final IModel<Set<String>> model) {
        super(id, model);
        selection = new Model<>(new ArrayList<>(model.getObject()));

        GridSetTableProvider provider =
                new GridSetTableProvider() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<GridSet> getItems() {
                        GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
                        List<String> list = selection.getObject();
                        List<GridSet> gridsets = new ArrayList<>(list.size());
                        for (String id : list) {
                            GridSet gridSet = gridSetBroker.get(id);
                            if (gridSet != null) {
                                gridsets.add(gridSet);
                            }
                        }
                        return gridsets;
                    }
                };

        defaultGridsetsTable = new DefaultGridSetsTable("table", provider);
        add(defaultGridsetsTable);

        IModel<List<String>> availableModel =
                new LoadableDetachableModel<List<String>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<String> load() {
                        List<String> gridSetNames =
                                new ArrayList<>(GWC.get().getGridSetBroker().getNames());
                        for (String gsId : selection.getObject()) {
                            gridSetNames.remove(gsId);
                        }
                        Collections.sort(gridSetNames);
                        return gridSetNames;
                    }
                };

        availableGridSets =
                new DropDownChoice<>("availableGridsets", new Model<>(), availableModel);
        availableGridSets.setOutputMarkupId(true);
        add(availableGridSets);

        GeoServerAjaxFormLink addGridsubsetLink =
                new GeoServerAjaxFormLink("addGridset") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        availableGridSets.processInput();

                        final String selectedGridset = availableGridSets.getModelObject();
                        if (null == selectedGridset) {
                            return;
                        }

                        List<String> choices = new ArrayList<>(availableGridSets.getChoices());
                        choices.remove(selectedGridset);
                        availableGridSets.setChoices(choices);
                        availableGridSets.setEnabled(!choices.isEmpty());

                        List<String> selectedIds = selection.getObject();
                        selectedIds.add(selectedGridset);
                        // Execute setPageable() in order to re-create the inner record list
                        // updated.
                        defaultGridsetsTable.setPageable(false);
                        target.add(defaultGridsetsTable);
                        target.add(availableGridSets);
                    }
                };
        addGridsubsetLink.add(new Icon("addIcon", GWCIconFactory.ADD_ICON));
        add(addGridsubsetLink);
    }

    @Override
    public void convertInput() {
        List<String> defaultGridsetIds = selection.getObject();
        Set<String> convertedInput = new HashSet<>();
        convertedInput.addAll(defaultGridsetIds);
        setConvertedInput(convertedInput);
    }

    /** */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }
}
