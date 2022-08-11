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

package org.geoserver.importer.web;

import java.io.File;
import java.io.IOException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

public class SpatialFilePanel extends ImportSourcePanel {

    String file;

    TextField<String> fileField;
    GeoServerDialog dialog;

    public SpatialFilePanel(String id) {
        super(id);

        add(dialog = new GeoServerDialog("dialog"));

        Form<SpatialFilePanel> form = new Form<>("form", new CompoundPropertyModel<>(this));
        add(form);

        fileField = new TextField<>("file");
        fileField.setRequired(true);
        fileField.setOutputMarkupId(true);
        form.add(fileField);
        form.add(chooserButton(form));
    }

    public ImportData createImportSource() throws IOException {
        File file = new File(this.file);
        return FileData.createFromFile(file);
    };

    Component chooserButton(Form form) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("chooser") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        dialog.setTitle(new ParamResourceModel("chooseFile", this));
                        dialog.showOkCancel(
                                target,
                                new GeoServerDialog.DialogDelegate() {

                                    @Override
                                    protected Component getContents(String id) {
                                        // use what the user currently typed
                                        File file = null;
                                        if (!fileField.getInput().trim().equals("")) {
                                            file = new File(fileField.getInput());
                                            if (!file.exists()) file = null;
                                        }

                                        GeoServerFileChooser chooser =
                                                new GeoServerFileChooser(id, new Model<>(file)) {
                                                    @Override
                                                    protected void fileClicked(
                                                            File file, AjaxRequestTarget target) {
                                                        SpatialFilePanel.this.file =
                                                                file.getAbsolutePath();

                                                        fileField.clearInput();
                                                        fileField.setModelObject(
                                                                file.getAbsolutePath());

                                                        target.add(fileField);
                                                        dialog.close(target);
                                                    }
                                                };

                                        initFileChooser(chooser);
                                        return chooser;
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        GeoServerFileChooser chooser =
                                                (GeoServerFileChooser) contents;
                                        file =
                                                ((File) chooser.getDefaultModelObject())
                                                        .getAbsolutePath();

                                        // clear the raw input of the field won't show the new model
                                        // value
                                        fileField.clearInput();
                                        // fileField.setModelObject(file);

                                        target.add(fileField);
                                        return true;
                                    }

                                    @Override
                                    public void onClose(AjaxRequestTarget target) {
                                        // update the field with the user chosen value
                                        target.add(fileField);
                                    }
                                });
                    }
                };
        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);
        return link;
    }

    SubmitLink submitLink() {
        return new SubmitLink("submit") {

            @Override
            public void onSubmit() {
                //                try {
                //                    // check there is not another store with the same name
                //                    WorkspaceInfo workspace = generalParams.getWorkpace();
                //                    NamespaceInfo namespace = getCatalog()
                //                            .getNamespaceByPrefix(workspace.getName());
                //                    StoreInfo oldStore = getCatalog().getStoreByName(workspace,
                // generalParams.name,
                //                            StoreInfo.class);
                //                    if (oldStore != null) {
                //                        error(new
                // ParamResourceModel("ImporterError.duplicateStore",
                //                                DirectoryPage.this, generalParams.name,
                // workspace.getName()).getString());
                //                        return;
                //                    }
                //
                //                    // build/reuse the store
                //                    String storeType = new
                // ShapefileDataStoreFactory().getDisplayName();
                //                    Map<String, Serializable> params = new HashMap<String,
                // Serializable>();
                //                    params.put(ShapefileDataStoreFactory.URLP.key, new
                // File(directory).toURI()
                //                            .toURL().toString());
                //                    params.put(ShapefileDataStoreFactory.NAMESPACEP.key, new
                // URI(namespace.getURI()).toString());
                //
                //                    DataStoreInfo si;
                //                    StoreInfo preExisting = getCatalog().getStoreByName(workspace,
                // generalParams.name,
                //                            StoreInfo.class);
                //                    boolean storeNew = false;
                //                    if (preExisting != null) {
                //                        if (!(preExisting instanceof DataStoreInfo)) {
                //                            error(new ParamResourceModel("storeExistsNotVector",
                // this, generalParams.name));
                //                            return;
                //                        }
                //                        si = (DataStoreInfo) preExisting;
                //                        if (!si.getType().equals(storeType)
                //                                || !si.getConnectionParameters().equals(params)) {
                //                            error(new ParamResourceModel("storeExistsNotSame",
                // this, generalParams.name));
                //                            return;
                //                        }
                //                        // make sure it's enabled, we just verified the directory
                // exists
                //                        si.setEnabled(true);
                //                    } else {
                //                        storeNew = true;
                //                        CatalogBuilder builder = new CatalogBuilder(getCatalog());
                //                        builder.setWorkspace(workspace);
                //                        si = builder.buildDataStore(generalParams.name);
                //                        si.setDescription(generalParams.description);
                //                        si.getConnectionParameters().putAll(params);
                //                        si.setEnabled(true);
                //                        si.setType(storeType);
                //
                //                        getCatalog().add(si);
                //                    }
                //
                //                    // redirect to the layer chooser
                //                    PageParameters pp = new PageParameters();
                //                    pp.put("store", si.getName());
                //                    pp.put("workspace", workspace.getName());
                //                    pp.put("storeNew", storeNew);
                //                    pp.put("workspaceNew", false);
                //                    setResponsePage(VectorLayerChooserPage.class, pp);
                //                } catch (Exception e) {
                //                    LOGGER.log(Level.SEVERE, "Error while setting up mass import",
                // e);
                //                }

            }
        };
    }

    protected void initFileChooser(GeoServerFileChooser fileChooser) {
        // chooser.setFilter(new Model(new ExtensionFile"(".shp")));
    }
}
