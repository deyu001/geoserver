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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.layer.StyleParameterFilter;

/**
 * Subform that displays basic information about a ParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public class StyleParameterFilterSubform
        extends AbstractParameterFilterSubform<StyleParameterFilter> {

    /** Model Set<String> as a List<String> and optionally add a dummy element at the beginning. */
    static class SetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        private final IModel<Set<String>> realModel;

        private final List<String> fakeObject;

        protected final String extra;

        public SetAsListModel(IModel<Set<String>> realModel, String extra) {
            super();
            this.realModel = realModel;
            this.extra = extra;

            Set<String> realObj = realModel.getObject();

            int size;
            if (realObj == null) {
                size = 0;
            } else {
                size = realObj.size();
            }
            if (extra != null) {
                size++;
            }
            fakeObject = new ArrayList<>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();

            fakeObject.clear();

            if (extra != null) fakeObject.add(extra);
            if (realObj != null) fakeObject.addAll(realObj);

            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if (object == null) {
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<>(object);
                newObj.remove(extra);
                realModel.setObject(new HashSet<>(object));
            }
        }
    }

    static class LabelledEmptyStringModel implements IModel<String> {

        private static final long serialVersionUID = 7591957769540603345L;

        private final IModel<String> realModel;

        final String label;

        public LabelledEmptyStringModel(IModel<String> realModel, String label) {
            super();
            this.realModel = realModel;
            this.label = label;
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public String getObject() {
            String s = realModel.getObject();
            if (s == null || s.isEmpty()) {
                return label;
            } else {
                return s;
            }
        }

        @Override
        public void setObject(String object) {
            if (label.equals(object)) {
                realModel.setObject("");
            } else {
                realModel.setObject(object);
            }
        }
    }
    /**
     * Model Set<String> as a List<String> and add an option to represent the set being {@literal
     * null}
     */
    static class NullableSetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        private final IModel<Set<String>> realModel;

        private final List<String> fakeObject;

        protected final String nullify;

        public NullableSetAsListModel(IModel<Set<String>> realModel, String nullify) {
            super();
            this.realModel = realModel;
            this.nullify = nullify;

            Set<String> realObj = realModel.getObject();

            int size;
            if (realObj == null) {
                size = 1;
            } else {
                size = realObj.size();
            }
            fakeObject = new ArrayList<>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();

            fakeObject.clear();

            if (realObj != null) {
                fakeObject.addAll(realObj);
            } else {
                fakeObject.add(nullify);
            }

            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if (object == null || object.contains(nullify)) {
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<>(object);
                newObj.remove(nullify);
                realModel.setObject(new HashSet<>(object));
            }
        }
    }

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public StyleParameterFilterSubform(String id, IModel<StyleParameterFilter> model) {
        super(id, model);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        final Component defaultValue;

        final String allStyles = getLocalizer().getString("allStyles", this);
        final String layerDefault = getLocalizer().getString("layerDefault", this);

        final IModel<List<String>> availableStylesModelDefault =
                new SetAsListModel(new PropertyModel<>(getModel(), "layerStyles"), layerDefault);
        final IModel<List<String>> availableStylesModelAllowed =
                new SetAsListModel(new PropertyModel<>(getModel(), "layerStyles"), allStyles);
        final IModel<List<String>> selectedStylesModel =
                new NullableSetAsListModel(new PropertyModel<>(getModel(), "styles"), allStyles);
        final IModel<String> selectedDefaultModel =
                new LabelledEmptyStringModel(
                        new PropertyModel<>(getModel(), "realDefault"), layerDefault);

        defaultValue =
                new DropDownChoice<>(
                        "defaultValue", selectedDefaultModel, availableStylesModelDefault);
        add(defaultValue);

        final CheckBoxMultipleChoice<String> styles =
                new CheckBoxMultipleChoice<>(
                        "styles", selectedStylesModel, availableStylesModelAllowed);
        styles.setPrefix("<li>");
        styles.setSuffix("</li>");
        add(styles);
    }
}
