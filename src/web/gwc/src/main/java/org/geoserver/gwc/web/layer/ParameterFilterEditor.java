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

package org.geoserver.gwc.web.layer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.StyleParameterFilter;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.IntegerParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;

class ParameterFilterEditor extends FormComponentPanel<Set<ParameterFilter>> {

    private static final Logger LOGGER = Logging.getLogger(ParameterFilterEditor.class);

    private static final long serialVersionUID = 5098470663723800345L;

    private static final List<String> COMMON_KEYS =
            Arrays.asList(
                    "ENV",
                    "FORMAT_OPTIONS",
                    "ANGLE",
                    "BGCOLOR",
                    "BUFFER",
                    "CQL_FILTER",
                    "ELEVATION",
                    "FEATUREID",
                    "FILTER",
                    "PALETTE",
                    "STARTINDEX",
                    "MAXFEATURES",
                    "TIME",
                    "VIEWPARAMS",
                    "FEATUREVERSION");

    private final WebMarkupContainer table;

    private final ListView<ParameterFilter> filters;

    private final ParameterListValidator validator;

    private final DropDownChoice<Class<? extends ParameterFilter>> availableFilterTypes;
    private final TextField<String> newFilterKey;

    private class ParameterListValidator implements IValidator<Set<ParameterFilter>> {

        private static final long serialVersionUID = 1L;

        private boolean validate;

        public ParameterListValidator() {
            this.setEnabled(true);
        }

        @Override
        public void validate(IValidatable<Set<ParameterFilter>> validatable) {
            if (!validate) {
                return;
            }
            Set<ParameterFilter> paramFilters = validatable.getValue();
            if (paramFilters == null) {
                return;
            }

            Set<String> keys = new TreeSet<>();
            for (ParameterFilter filter : paramFilters) {
                // TODO Validate
                final String key = filter.getKey();

                if (key == null) {
                    throw new IllegalStateException("ParameterFilter key is null");
                } else {
                    if (keys.contains(key)) {
                        error(validatable, "ParameterFilterEditor.validation.duplicateKey");
                        return;
                    } else {
                        keys.add(key);
                    }
                }
            }
        }

        private void error(
                IValidatable<Set<ParameterFilter>> validatable,
                final String resourceKey,
                final String... params) {

            ValidationError error = new ValidationError();
            String message;
            if (params == null) {
                message = new ResourceModel(resourceKey).getObject();
            } else {
                message =
                        new ParamResourceModel(
                                        resourceKey, ParameterFilterEditor.this, (Object[]) params)
                                .getObject();
            }
            error.setMessage(message);
            validatable.error(error);
        }

        public void setEnabled(boolean validate) {
            this.validate = validate;
        }
    }

    public ParameterFilterEditor(
            final String id,
            final IModel<Set<ParameterFilter>> model,
            final IModel<? extends CatalogInfo> layerModel) {
        super(id, model);
        add(validator = new ParameterListValidator());

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);

        container.add(table);

        filters =
                new ListView<ParameterFilter>(
                        "parameterFilters", new ArrayList<>(model.getObject())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onBeforeRender() {
                        // let's remove the correct child quickly before wicket just removes the
                        // last one on the list.
                        for (final Iterator<Component> iterator = iterator();
                                iterator.hasNext(); ) {
                            final ListItem<?> child = (ListItem<?>) iterator.next();
                            if (child != null) {
                                if (!getList()
                                        .contains(child.get("subform").getDefaultModelObject())) {
                                    iterator.remove();
                                }
                            }
                        }

                        super.onBeforeRender();
                    }

                    @Override
                    protected void populateItem(final ListItem<ParameterFilter> item) {
                        // odd/even style
                        final int index = item.getIndex();
                        item.add(
                                AttributeModifier.replace(
                                        "class", index % 2 == 0 ? "even" : "odd"));

                        // Create form
                        final Label keyLabel;
                        keyLabel =
                                new Label("key", new PropertyModel<String>(item.getModel(), "key"));
                        item.add(keyLabel);

                        final Component subForm =
                                getSubform("subform", new Model<>(item.getModelObject()));
                        item.add(subForm);

                        final AjaxSubmitLink removeLink;

                        removeLink =
                                new AjaxSubmitLink("removeLink") {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void onSubmit(
                                            AjaxRequestTarget target, Form<?> form) {
                                        getList().remove(getDefaultModelObject());
                                        target.add(container);
                                    }
                                };
                        removeLink.add(new Icon("removeIcon", GWCIconFactory.DELETE_ICON));
                        removeLink.setDefaultModel(item.getModel());
                        removeLink.add(
                                new AttributeModifier(
                                        "title",
                                        new ResourceModel("ParameterFilterEditor.removeLink")));
                        item.add(removeLink);
                    }
                };

        filters.setOutputMarkupId(true);
        // this is necessary to avoid loosing item contents on edit/validation checks
        filters.setReuseItems(true);

        Form<?> filtersForm = new Form<>("filtersForm", filters.getDefaultModel());
        filtersForm.add(filters);

        table.add(filtersForm);

        List<String> parameterKeys = new ArrayList<>(GWC.get().getGridSetBroker().getNames());
        for (ParameterFilter filter : model.getObject()) {
            parameterKeys.remove(filter.getKey());
        }
        Collections.sort(parameterKeys);

        GeoServerAjaxFormLink addStyleFilterLink =
                new GeoServerAjaxFormLink("addStyleFilter") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        StyleParameterFilter newFilter = new StyleParameterFilter();
                        newFilter.setLayer((LayerInfo) layerModel.getObject());

                        addFilter(newFilter);

                        target.add(container);
                    }
                };
        addStyleFilterLink.add(new Icon("addIcon", GWCIconFactory.ADD_ICON));
        add(addStyleFilterLink);

        // FIXME: make this extensible so new kinds of filter can be supported by
        ArrayList<Class<? extends ParameterFilter>> filterTypes = new ArrayList<>();
        filterTypes.add(StringParameterFilter.class);
        filterTypes.add(FloatParameterFilter.class);
        filterTypes.add(IntegerParameterFilter.class);
        filterTypes.add(RegexParameterFilter.class);

        availableFilterTypes =
                new DropDownChoice<>(
                        "availableFilterTypes",
                        new Model<>(),
                        new Model<>(filterTypes),
                        new ChoiceRenderer<Class<? extends ParameterFilter>>() {

                            /** serialVersionUID */
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object getDisplayValue(Class<? extends ParameterFilter> object) {
                                String resource =
                                        "ParameterFilterEditor.filtername."
                                                + object.getCanonicalName();
                                try {
                                    // Try to look up a localized name for the class
                                    return getLocalizer()
                                            .getString(resource, ParameterFilterEditor.this);
                                } catch (MissingResourceException ex) {
                                    // Use the simple name as a backup
                                    if (LOGGER.isLoggable(Level.CONFIG))
                                        LOGGER.log(
                                                Level.CONFIG,
                                                "Could not find localization resource"
                                                        + " for ParameterFilter subclass "
                                                        + object.getCanonicalName());

                                    return object.getSimpleName();
                                }
                            }

                            @Override
                            public String getIdValue(
                                    Class<? extends ParameterFilter> object, int index) {
                                return Integer.toString(index);
                            }
                        });
        availableFilterTypes.setOutputMarkupId(true);
        add(availableFilterTypes);

        newFilterKey = new TextField<>("newFilterKey", Model.of(""));
        add(newFilterKey);

        // TODO update this to eliminate keys that are in use
        final RepeatingView commonKeys = new RepeatingView("commonKeys");
        for (String key : COMMON_KEYS) {
            commonKeys.add(new Label(commonKeys.newChildId(), key));
        }
        add(commonKeys);

        GeoServerAjaxFormLink addFilterLink =
                new GeoServerAjaxFormLink("addFilter") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        availableFilterTypes.processInput();
                        newFilterKey.processInput();
                        String key = newFilterKey.getModelObject();
                        if (key == null || key.isEmpty()) {
                            ParamResourceModel rm =
                                    new ParamResourceModel(
                                            "ParameterFilterEditor.nonEmptyFilter", null, "");
                            error(rm.getString());
                        } else {
                            Class<? extends ParameterFilter> type =
                                    availableFilterTypes.getModelObject();

                            try {
                                ParameterFilter newFilter = type.getConstructor().newInstance();
                                newFilter.setKey(key);
                                addFilter(newFilter);
                                newFilterKey.setModel(Model.of("")); // Reset the key field
                            } catch (NoSuchMethodException ex) {
                                LOGGER.log(Level.WARNING, "No Default Constructor for " + type, ex);
                            } catch (InvocationTargetException
                                    | SecurityException
                                    | InstantiationException
                                    | IllegalAccessException ex) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Could not execute default Constructor for " + type,
                                        ex);
                            }
                        }
                        target.add(container);
                    }
                };
        addFilterLink.add(new Icon("addIcon", GWCIconFactory.ADD_ICON));
        add(addFilterLink);
    }

    /** Returns an appropriate subform for the given ParameterFilter model */
    @SuppressWarnings("unchecked")
    private Component getSubform(String id, IModel<? extends ParameterFilter> model) {

        if (model.getObject() instanceof RegexParameterFilter) {
            return new RegexParameterFilterSubform(id, (IModel<RegexParameterFilter>) model);
        }
        if (model.getObject() instanceof StyleParameterFilter) {
            return new StyleParameterFilterSubform(id, (IModel<StyleParameterFilter>) model);
        }
        if (model.getObject() instanceof StringParameterFilter) {
            return new StringParameterFilterSubform(id, (IModel<StringParameterFilter>) model);
        }
        if (model.getObject() instanceof FloatParameterFilter) {
            return new FloatParameterFilterSubform(id, (IModel<FloatParameterFilter>) model);
        }
        if (model.getObject() instanceof IntegerParameterFilter) {
            return new IntegerParameterFilterSubform(id, (IModel<IntegerParameterFilter>) model);
        }
        return new DefaultParameterFilterSubform(id, (IModel<ParameterFilter>) model);
    }

    @Override
    public void convertInput() {
        filters.visitChildren(
                (component, visit) -> {
                    if (component instanceof FormComponent) {
                        FormComponent<?> formComponent = (FormComponent<?>) component;
                        formComponent.processInput();
                    }
                });
        List<ParameterFilter> info = filters.getModelObject();
        HashSet<ParameterFilter> convertedInput = new HashSet<>(info);
        setConvertedInput(convertedInput);
    }

    private boolean hasFilter(String key) {
        for (ParameterFilter existing : filters.getModelObject()) {
            if (existing.getKey().equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private boolean addFilter(ParameterFilter filter) {
        if (hasFilter(filter.getKey())) return false;

        filters.getModelObject().add(filter);
        return true;
    }

    /** */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }

    public void setValidating(final boolean validate) {
        validator.setEnabled(validate);
    }
}
