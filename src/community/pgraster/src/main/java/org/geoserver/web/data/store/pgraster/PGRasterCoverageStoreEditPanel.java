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

package org.geoserver.web.data.store.pgraster;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;

/**
 * Provides more components for PGRaster store automatic configuration
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public final class PGRasterCoverageStoreEditPanel extends StoreEditPanel {

    private CheckBox enabled;

    public PGRasterCoverageStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        // double container dance to get stuff to show up and hide on demand (grrr)
        final WebMarkupContainer configsContainer = new WebMarkupContainer("configsContainer");
        configsContainer.setOutputMarkupId(true);
        add(configsContainer);

        final PGRasterPanel advancedConfigPanel =
                new PGRasterPanel("pgpanel", paramsModel, storeEditForm);
        advancedConfigPanel.setOutputMarkupId(true);
        advancedConfigPanel.setVisible(false);
        configsContainer.add(advancedConfigPanel);

        // TODO: Check whether this constructor is properly setup
        final TextParamPanel url =
                new TextParamPanel(
                        "urlPanel",
                        new PropertyModel(paramsModel, "URL"),
                        new ResourceModel("url", "URL"),
                        true);
        final FormComponent urlFormComponent = url.getFormComponent();
        urlFormComponent.add(new FileExistsValidator());
        add(url);

        // enabled flag, and show the rest only if enabled is true
        IModel<Boolean> enabledModel = new Model<Boolean>(false);
        enabled = new CheckBox("enabled", enabledModel);
        add(enabled);
        enabled.add(
                new AjaxFormComponentUpdatingBehavior("click") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean visible = enabled.getModelObject();

                        advancedConfigPanel.setVisible(visible);
                        target.add(configsContainer);
                    }
                });

        /*
         * Listen to form submission and update the model's URL
         */
        storeEditForm.add(
                new IFormValidator() {
                    private static final long serialVersionUID = 1L;

                    public FormComponent[] getDependentFormComponents() {
                        if (enabled.getModelObject()) {
                            return advancedConfigPanel.getDependentFormComponents();
                        } else {
                            return new FormComponent[] {urlFormComponent};
                        }
                    }

                    public void validate(final Form form) {
                        CoverageStoreInfo storeInfo = (CoverageStoreInfo) form.getModelObject();
                        String coverageUrl = urlFormComponent.getValue();
                        if (enabled.getModelObject()) {
                            coverageUrl = advancedConfigPanel.buildURL() + coverageUrl;
                        }

                        storeInfo.setURL(coverageUrl);
                    }
                });
    }

    private FormComponent addTextPanel(final IModel paramsModel, final String paramName) {

        final String resourceKey = getClass().getSimpleName() + "." + paramName;

        final boolean required = true;

        final TextParamPanel textParamPanel =
                new TextParamPanel(
                        paramName,
                        new MapModel(paramsModel, paramName),
                        new ResourceModel(resourceKey, paramName),
                        required);
        textParamPanel.getFormComponent().setType(String.class);

        String defaultTitle = paramName;

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(AttributeModifier.replace("title", title));

        add(textParamPanel);
        return textParamPanel.getFormComponent();
    }
}
