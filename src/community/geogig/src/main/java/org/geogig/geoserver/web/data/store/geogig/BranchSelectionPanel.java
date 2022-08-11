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

package org.geogig.geoserver.web.data.store.geogig;

import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.DataStoreInfo;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.repository.RepositoryResolver;

public class BranchSelectionPanel extends FormComponentPanel<String> {
    private static final long serialVersionUID = 1L;

    private final DropDownChoice<String> choice;

    private final IModel<String> repositoryUriModel;

    private transient Supplier<RepositoryManager> manager = () -> RepositoryManager.get();

    public BranchSelectionPanel(
            String id,
            IModel<String> repositoryUriModel,
            IModel<String> branchNameModel,
            Form<DataStoreInfo> storeEditForm) {
        super(id, branchNameModel);
        this.repositoryUriModel = repositoryUriModel;

        final List<String> choices = new ArrayList<String>();
        choice = new DropDownChoice<String>("branchDropDown", branchNameModel, choices);
        choice.setOutputMarkupId(true);
        choice.setNullValid(true);
        choice.setRequired(false);
        add(choice);
        updateChoices(false, null);

        final AjaxSubmitLink refreshLink =
                new AjaxSubmitLink("refresh", storeEditForm) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        onSubmit(target, form);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateChoices(true, form);
                        target.add(BranchSelectionPanel.this.choice);
                    }
                };
        add(refreshLink);
    }

    @Override
    public void convertInput() {
        choice.processInput();
        String branch = choice.getConvertedInput();
        setModelObject(branch);
        setConvertedInput(branch);
    }

    @VisibleForTesting
    void setRepositoryManager(Supplier<RepositoryManager> supplier) {
        this.manager = supplier;
    }

    public void updateChoices(boolean reportError, Form<?> form) {
        final String repoUriStr = repositoryUriModel.getObject();
        if (REPOSITORY.sample != null && REPOSITORY.sample.equals(repoUriStr)) {
            return;
        }
        List<String> branchNames = new ArrayList<>();
        if (repoUriStr != null) {
            try {
                RepositoryManager manager = this.manager.get();
                URI repoURI = new URI(repoUriStr);
                RepositoryResolver resolver = RepositoryResolver.lookup(repoURI);
                String repoName = resolver.getName(repoURI);
                RepositoryInfo repoInfo = manager.getByRepoName(repoName);
                if (repoInfo != null) {
                    String repoId = repoInfo.getId();
                    List<Ref> branchRefs = manager.listBranches(repoId);
                    for (Ref branch : branchRefs) {
                        branchNames.add(branch.localName());
                    }
                }
            } catch (IOException | URISyntaxException e) {
                if (reportError) {
                    form.error("Could not list branches: " + e.getMessage());
                }
            }
            String current = (String) choice.getModelObject();
            if (current != null && !branchNames.contains(current)) {
                branchNames.add(0, current);
            }
        }
        choice.setChoices(branchNames);
    }
}
