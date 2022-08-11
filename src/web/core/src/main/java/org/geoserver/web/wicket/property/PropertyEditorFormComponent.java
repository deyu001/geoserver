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

package org.geoserver.web.wicket.property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.ValidationError;
import org.springframework.util.StringUtils;

/**
 * Form component panel for editing {@link Properties} property.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PropertyEditorFormComponent extends FormComponentPanel<Properties> {

    private static final long serialVersionUID = -1960584178014140068L;
    ListView<Tuple> listView;
    List<Tuple> invalidTuples = null;

    public PropertyEditorFormComponent(String id) {
        super(id);
        init();
    }

    public PropertyEditorFormComponent(String id, IModel<Properties> model) {
        super(id, model);
        init();
    }

    void init() {
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        listView =
                new ListView<Tuple>("list") {
                    private static final long serialVersionUID = -7250612551499360015L;

                    @Override
                    protected void populateItem(ListItem<Tuple> item) {
                        item.setModel(new CompoundPropertyModel<>(item.getModelObject()));
                        item.add(
                                new TextField<String>("key")
                                        .add(
                                                new AjaxFormComponentUpdatingBehavior("blur") {
                                                    private static final long serialVersionUID =
                                                            5416373713193788662L;

                                                    @Override
                                                    protected void onUpdate(
                                                            AjaxRequestTarget target) {}
                                                }));
                        item.add(
                                new TextField<String>("value")
                                        .add(
                                                new AjaxFormComponentUpdatingBehavior("blur") {
                                                    private static final long serialVersionUID =
                                                            -8679502120189597358L;

                                                    @Override
                                                    protected void onUpdate(
                                                            AjaxRequestTarget target) {}
                                                }));
                        item.add(
                                new AjaxLink<Tuple>("remove", item.getModel()) {
                                    private static final long serialVersionUID =
                                            3201264868229144613L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        List<Tuple> l = listView.getModelObject();
                                        l.remove(getModelObject());
                                        target.add(container);
                                    }
                                });
                    }
                };
        // listView.setReuseItems(true);
        container.add(listView);

        add(
                new AjaxLink<Void>("add") {
                    private static final long serialVersionUID = 4741595573705562351L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        listView.getModelObject().add(new Tuple());
                        target.add(container);
                    }
                });
    }

    List<Tuple> tuples() {

        if (invalidTuples != null) return invalidTuples;

        Properties props = getModelObject();
        if (props == null) {
            props = new Properties();
        }

        List<Tuple> tuples = new ArrayList<>();
        for (Map.Entry<Object, Object> e : props.entrySet()) {
            tuples.add(new Tuple((String) e.getKey(), (String) e.getValue()));
        }

        Collections.sort(tuples);
        return tuples;
    }

    @Override
    protected void onBeforeRender() {
        listView.setModel(new ListModel<>(tuples()));
        super.onBeforeRender();
    }

    @Override
    public void convertInput() {
        for (org.apache.wicket.Component component : listView) {
            ListItem<?> item = (ListItem<?>) component;
            ((FormComponent<?>) item.get("key")).updateModel();
            ((FormComponent<?>) item.get("value")).updateModel();
        }

        Properties props = getModelObject();
        if (props == null) {
            props = new Properties();
        }

        props.clear();
        for (Tuple t : listView.getModelObject()) {
            props.put(t.getKey(), t.getValue());
        }

        setConvertedInput(props);
    }

    @Override
    public void validate() {
        invalidTuples = null;
        for (Tuple t : listView.getModelObject()) {
            if (StringUtils.hasLength(t.getKey()) == false) {
                invalidTuples = listView.getModelObject();
                error(new ValidationError("KeyRequired").addKey("KeyRequired"));
                return;
            }
            if (StringUtils.hasLength(t.getValue()) == false) {
                invalidTuples = listView.getModelObject();
                error(new ValidationError("ValueRequired").addKey("ValueRequired"));
                return;
            }
        }

        super.validate();
    }

    static class Tuple implements Serializable, Comparable<Tuple> {
        private static final long serialVersionUID = 1L;

        private String key;
        private String value;

        public Tuple() {}

        public Tuple(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public int compareTo(Tuple o) {
            return key != null ? key.compareTo(o.key) : o.key == null ? 0 : -1;
        }
    }
}
