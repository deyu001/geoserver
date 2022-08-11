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

package org.geoserver.metadata.web.panel.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.geoserver.metadata.data.dto.AttributeConfiguration;

public class DropDownPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public DropDownPanel(
            String id,
            String attributeKey,
            IModel<String> model,
            List<String> values,
            IModel<List<String>> selectedValues) {

        super(id, model);

        if (selectedValues == null) { // not part of repeatable
            add(createDropDown(attributeKey, model, values));
        } else { // part of repeatable
            add(createDropDown(attributeKey, model, values, selectedValues));
        }
    }

    private DropDownChoice<String> createDropDown(
            String attributeKey, IModel<String> model, List<String> values) {
        DropDownChoice<String> choice =
                new DropDownChoice<String>("dropdown", model, values, createRenderer(attributeKey));
        choice.setNullValid(true);
        return choice;
    }

    private DropDownChoice<String> createDropDown(
            String attributeKey,
            IModel<String> model,
            List<String> values,
            IModel<List<String>> selectedValues) {
        DropDownChoice<String> choice =
                new DropDownChoice<String>(
                        "dropdown",
                        model,
                        new IModel<List<String>>() {
                            private static final long serialVersionUID = -2410089772309709492L;

                            @Override
                            public List<String> getObject() {
                                Set<String> currentList = new TreeSet<>();
                                currentList.addAll(values);
                                currentList.removeIf(i -> selectedValues.getObject().contains(i));
                                if (!Strings.isEmpty(model.getObject())) {
                                    currentList.add(model.getObject());
                                }
                                return new ArrayList<String>(currentList);
                            }

                            @Override
                            public void setObject(List<String> object) {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public void detach() {}
                        },
                        createRenderer(attributeKey));
        choice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 1989673955080590525L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(
                                DropDownPanel.this.findParent(
                                        RepeatableAttributesTablePanel.class));
                    }
                });
        choice.setNullValid(true);
        return choice;
    }

    private IChoiceRenderer<String> createRenderer(String attributeKey) {
        return new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(String object) {
                return getString(
                        AttributeConfiguration.PREFIX + attributeKey + "." + object, null, object);
            }

            @Override
            public String getIdValue(String object, int index) {
                return object;
            }

            @Override
            public String getObject(String id, IModel<? extends List<? extends String>> choices) {
                return Strings.isEmpty(id) ? null : id;
            }
        };
    }
}
