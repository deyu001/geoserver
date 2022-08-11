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

package org.geogig.geoserver.web.repository;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.model.DropDownModel;
import org.geogig.geoserver.model.PGBeanModel;
import org.geogig.geoserver.model.RepoDirModel;
import org.geogig.geoserver.model.RepoNameModel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

/**
 * Wicket form panel to hold form components for configuring a GeoGig repository. This panel allows
 * for configuring a GeoGig repository URI via multiple child panels, one for each type of
 * configuration. Currently, only File/Directory based config and PostgreSQL backend configurations
 * are supported. The DropDownChoice switches the configuration types and hides/exposes fields
 * specific to the choice selected. Each choice should introduce a wrapping IModel implementation
 * that transforms the repository URI location field of {@link RepositoryInfo} into a data model
 * that maps configuration fields into UI components.
 */
class GeoGigRepositoryInfoFormComponent extends FormComponentPanel<RepositoryInfo> {

    private static final long serialVersionUID = 1L;

    private final TextParamPanel repoNamePanel;
    private final DropDownChoiceParamPanel dropdownPanel;
    private final GeoGigDirectoryFormComponent directoryComponent;
    private final PostgresConfigFormPanel pgPanel;

    private WebMarkupContainer settingsContainer;

    GeoGigRepositoryInfoFormComponent(String id, IModel<RepositoryInfo> model, boolean isNew) {
        super(id, model);

        // add the dropdown to switch between configurations
        dropdownPanel =
                new DropDownChoiceParamPanel(
                        "configChoicePanel",
                        new DropDownModel(model),
                        new ResourceModel(
                                "GeoGigRepositoryInfoFormComponent.repositoryType",
                                "Repository Type"),
                        DropDownModel.CONFIG_LIST,
                        true);
        final DropDownChoice<Serializable> dropDownChoice = dropdownPanel.getFormComponent();
        dropDownChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        final String value = dropDownChoice.getModelObject().toString();
                        directoryComponent.setVisible(DropDownModel.DIRECTORY_CONFIG.equals(value));
                        pgPanel.setVisible(DropDownModel.PG_CONFIG.equals(value));
                        target.add(settingsContainer);
                    }
                });
        dropdownPanel.setEnabled(isNew);
        add(dropdownPanel);

        settingsContainer = new WebMarkupContainer("settingsContainer");
        settingsContainer.setOutputMarkupId(true);
        add(settingsContainer);

        IModel<String> nameModel = new RepoNameModel(model);
        repoNamePanel =
                new TextParamPanel(
                        "repositoryNamePanel",
                        nameModel,
                        new ResourceModel(
                                "GeoGigRepositoryInfoFormComponent.repositoryName",
                                "Repository Name"),
                        true);
        settingsContainer.add(repoNamePanel);

        pgPanel = new PostgresConfigFormPanel("pgPanel", new PGBeanModel(model));
        pgPanel.setVisible(
                DropDownModel.PG_CONFIG.equals(dropDownChoice.getModelObject().toString()));
        settingsContainer.add(pgPanel);

        directoryComponent =
                new GeoGigDirectoryFormComponent("parentDirectory", new RepoDirModel(model));
        directoryComponent.setOutputMarkupId(true);
        directoryComponent.setVisible(
                DropDownModel.DIRECTORY_CONFIG.equals(dropDownChoice.getModelObject().toString()));
        directoryComponent.setEnabled(isNew);
        settingsContainer.add(directoryComponent);
    }

    @Override
    public void convertInput() {
        RepositoryInfo modelObject = getModelObject();
        // determine type
        URI location = modelObject.getLocation();
        final String repoTypeChoice =
                location != null
                        ? DropDownModel.getType(location)
                        : dropdownPanel.getFormComponent().getConvertedInput().toString();
        if (null != repoTypeChoice) {
            String repoName =
                    repoNamePanel.getFormComponent().getConvertedInput().toString().trim();
            modelObject.setRepoName(repoName);
            switch (repoTypeChoice) {
                case DropDownModel.PG_CONFIG:
                    // PG config used
                    PostgresConfigBean bean = pgPanel.getConvertedInput();
                    // build a URI out of the config
                    URI uri = bean.buildUriForRepo(repoName);
                    modelObject.setLocation(uri);
                    break;
                case DropDownModel.DIRECTORY_CONFIG:
                    if (modelObject.getLocation() == null) {
                        // local directory used
                        String path = directoryComponent.getConvertedInput().trim();
                        Path uriPath = Paths.get(path, UUID.randomUUID().toString());
                        modelObject.setLocation(uriPath.toUri());
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Unknown repositry type '%s', expected one of %s, %s",
                                    repoTypeChoice,
                                    DropDownModel.PG_CONFIG,
                                    DropDownModel.DIRECTORY_CONFIG));
            }
        }
        setConvertedInput(modelObject);
    }
}
