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

package org.geoserver.web.data.layer;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.locationtech.jts.geom.Geometry;

@SuppressWarnings("serial")
public class AttributeEditPage extends GeoServerSecuredPage {

    AttributeDescription attribute;

    NewFeatureTypePage previousPage;

    WebMarkupContainer crsContainer;

    WebMarkupContainer sizeContainer;

    boolean newAttribute;

    private TextField<String> nameField;

    String size;

    private TextField<String> sizeField;

    private CRSPanel crsField;

    public AttributeEditPage(
            final AttributeDescription attribute, final NewFeatureTypePage previousPage) {
        this(attribute, previousPage, false);
    }

    AttributeEditPage(
            final AttributeDescription attribute,
            final NewFeatureTypePage previousPage,
            final boolean newAttribute) {
        this.previousPage = previousPage;
        this.newAttribute = newAttribute;
        this.attribute = attribute;
        this.size = String.valueOf(attribute.getSize());

        final Form<AttributeDescription> form =
                new Form<>("form", new CompoundPropertyModel<>(attribute));
        form.setOutputMarkupId(true);
        add(form);

        form.add(nameField = new TextField<>("name"));
        DropDownChoice<Class<?>> binding =
                new DropDownChoice<>(
                        "binding", AttributeDescription.BINDINGS, new BindingChoiceRenderer());
        binding.add(
                new AjaxFormSubmitBehavior("change") {

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        updateVisibility(target);
                    }

                    private void updateVisibility(AjaxRequestTarget target) {
                        sizeContainer.setVisible(String.class.equals(attribute.getBinding()));
                        crsContainer.setVisible(
                                attribute.getBinding() != null
                                        && Geometry.class.isAssignableFrom(attribute.getBinding()));

                        addFeedbackPanels(target);
                        target.add(form);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        updateVisibility(target);
                    }
                });
        form.add(binding);
        form.add(new CheckBox("nullable"));

        sizeContainer = new WebMarkupContainer("sizeContainer");
        sizeContainer.setOutputMarkupId(true);
        form.add(sizeContainer);
        sizeContainer.add(sizeField = new TextField<>("size", new PropertyModel<>(this, "size")));
        sizeContainer.setVisible(String.class.equals(attribute.getBinding()));

        crsContainer = new WebMarkupContainer("crsContainer");
        crsContainer.setOutputMarkupId(true);
        form.add(crsContainer);
        crsContainer.add(crsField = new CRSPanel("crs"));
        crsContainer.setVisible(
                attribute.getBinding() != null
                        && Geometry.class.isAssignableFrom(attribute.getBinding()));

        SubmitLink submit =
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        if (validate()) {
                            if (newAttribute) {
                                previousPage.attributesProvider.addNewAttribute(attribute);
                            }
                            setResponsePage(previousPage);
                        }
                    }
                };
        form.setDefaultButton(submit);
        form.add(submit);
        form.add(
                new Link<Void>("cancel") {

                    @Override
                    public void onClick() {
                        setResponsePage(previousPage);
                    }
                });
    }

    /**
     * We have to resort to manual validation otherwise the ajax tricks performed by the drop down
     * won't work
     */
    protected boolean validate() {
        boolean valid = true;
        if (attribute.getName() == null || attribute.getName().trim().equals("")) {
            nameField.error(new ValidationError().addKey("Required"));
            valid = false;
        }
        if (String.class.equals(attribute.getBinding())) {
            try {
                attribute.setSize(Integer.parseInt(size));
                if (attribute.getSize() <= 0) {
                    sizeField.error(new ParamResourceModel("notPositive", this));
                    valid = false;
                }
            } catch (Exception e) {
                sizeField.error(new ParamResourceModel("notInteger", this, size));
                valid = false;
            }
        }
        if (Geometry.class.isAssignableFrom(attribute.getBinding()) && attribute.getCrs() == null) {
            crsField.error(new ValidationError().addKey("Required"));
            valid = false;
        }

        return valid;
    }

    static class BindingChoiceRenderer extends ChoiceRenderer<Class<?>> {

        public Object getDisplayValue(Class<?> object) {
            return AttributeDescription.getLocalizedName(object);
        }

        public String getIdValue(Class<?> object, int index) {
            return object.getName();
        }
    }
}
