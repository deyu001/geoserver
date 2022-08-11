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

package org.geoserver.security.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.RuleRolesFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;

/** Abstract page binding a {@link DataAccessRule} */
@SuppressWarnings("serial")
public abstract class AbstractServiceAccessRulePage extends AbstractSecurityPage {

    protected DropDownChoice<String> serviceChoice, methodChoice;
    protected RuleRolesFormComponent rolesFormComponent;

    public AbstractServiceAccessRulePage(final ServiceAccessRule rule) {

        // build the form
        Form<ServiceAccessRule> form = new Form<>("form", new CompoundPropertyModel<>(rule));
        add(form);
        form.add(new EmptyRolesValidator());

        form.add(serviceChoice = new DropDownChoice<>("service", getServiceNames()));
        serviceChoice.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        methodChoice.updateModel();
                        target.add(methodChoice);
                    }
                });
        serviceChoice.setRequired(true);

        form.add(methodChoice = new DropDownChoice<>("method", new MethodsModel(rule)));

        // we add on change behavior to ensure the underlying model is updated but don't actually
        // do anything on change... this allows us to keep state when someone adds a new role,
        // leaving the page, TODO: find a better way to do this
        // methodChoice.add(new OnChangeAjaxBehavior() {
        //    @Override
        //    protected void onUpdate(AjaxRequestTarget target) {}
        // });
        methodChoice.setOutputMarkupId(true);
        methodChoice.setRequired(true);

        form.add(
                rolesFormComponent =
                        new RuleRolesFormComponent("roles", new PropertyModel<>(rule, "roles")));
        // new Model((Serializable)new ArrayList(rule.getRoles()))));

        // build the submit/cancel
        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        onFormSubmit((ServiceAccessRule) getForm().getModelObject());
                    }
                });
        form.add(new BookmarkablePageLink<>("cancel", ServiceAccessRulePage.class));
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(ServiceAccessRule rule);

    /** Returns a sorted list of workspace names */
    ArrayList<String> getServiceNames() {
        ArrayList<String> result = new ArrayList<>();
        for (Service ows : GeoServerExtensions.extensions(Service.class)) {
            if (!result.contains(ows.getId())) result.add(ows.getId());
        }
        Collections.sort(result);
        result.add(0, "*");

        return result;
    }

    class EmptyRolesValidator extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {rolesFormComponent};
        }

        @Override
        public void validate(Form<?> form) {
            // only validate on final submit
            if (form.findSubmittingButton() != form.get("save")) {
                return;
            }
            updateModels();
            String roleInputString =
                    rolesFormComponent.getPalette().getRecorderComponent().getInput();
            if ((roleInputString == null || roleInputString.trim().isEmpty())
                    && !rolesFormComponent.isHasAnyRole()) {
                form.error(new ParamResourceModel("emptyRoles", getPage()).getString());
            }
        }
    }

    class MethodsModel implements IModel<List<String>> {

        ServiceAccessRule rule;

        MethodsModel(ServiceAccessRule rule) {
            this.rule = rule;
        }

        @Override
        public List<String> getObject() {
            List<String> result = new ArrayList<>();
            for (Service ows : GeoServerExtensions.extensions(Service.class)) {
                String service = rule.getService();
                if (ows.getId().equals(service)) {
                    for (String operation : ows.getOperations()) {
                        if (!result.contains(operation)) {
                            result.add(operation);
                        }
                    }
                }
            }
            Collections.sort(result);
            result.add(0, "*");
            return result;
        }

        @Override
        public void setObject(List<String> object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void detach() {}
    }

    protected void updateModels() {
        serviceChoice.updateModel();
        methodChoice.updateModel();
        rolesFormComponent.updateModel();
    }
}
