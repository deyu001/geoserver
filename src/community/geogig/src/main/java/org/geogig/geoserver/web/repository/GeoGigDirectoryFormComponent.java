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

import java.io.File;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.FileParamPanel;

/**
 * A panel to browse the filesystem for geogig repositories.
 *
 * <p>Adapted from {@link FileParamPanel}
 */
class GeoGigDirectoryFormComponent extends FormComponentPanel<String> {

    private static final long serialVersionUID = -7456670856888745195L;

    private final TextField<String> directory;

    private final ModalWindow dialog;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    GeoGigDirectoryFormComponent(final String id, final IModel<String> valueModel) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, valueModel);

        // add the dialog for the file chooser
        add(dialog = new ModalWindow("dialog"));

        // the text field, with a decorator for validations
        directory = new TextField<>("value", valueModel);
        directory.setRequired(true);
        directory.setOutputMarkupId(true);

        IModel<String> labelModel =
                new ResourceModel("GeoGigDirectoryFormComponent.directory", "Parent directory") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        String value = super.getObject();
                        return value + " *";
                    }
                };

        final Label directoryLabel = new Label("directoryLabel", labelModel.getObject());
        add(directoryLabel);

        directory.setLabel(labelModel);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("wrapper");
        feedback.add(directory);
        feedback.add(chooserButton());
        add(feedback);
    }

    @Override
    public void convertInput() {
        String uri = directory.getConvertedInput();
        setConvertedInput(uri);
    }

    private Component chooserButton() {
        AjaxSubmitLink link =
                new AjaxSubmitLink("chooser") {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        File file = null;
                        directory.processInput();
                        String input = directory.getConvertedInput();
                        if (input != null && !input.isEmpty()) {
                            file = new File(input);
                        }

                        final boolean makeRepositoriesSelectable = false;
                        DirectoryChooser chooser =
                                new DirectoryChooser(
                                        dialog.getContentId(),
                                        new Model<>(file),
                                        makeRepositoriesSelectable) {

                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void geogigDirectoryClicked(
                                            final File file, AjaxRequestTarget target) {
                                        // clear the raw input of the field won't show the new model
                                        // value
                                        directory.clearInput();
                                        directory.setModelObject(file.getAbsolutePath());

                                        target.add(directory);
                                        dialog.close(target);
                                    };

                                    @Override
                                    protected void directorySelected(
                                            File file, AjaxRequestTarget target) {
                                        directory.clearInput();
                                        directory.setModelObject(file.getAbsolutePath());
                                        target.add(directory);
                                        dialog.close(target);
                                    }
                                };
                        chooser.setFileTableHeight(null);
                        dialog.setContent(chooser);
                        dialog.setTitle(
                                new ResourceModel(
                                        "GeoGigDirectoryFormComponent.chooser.chooseParentTile"));
                        dialog.show(target);
                    }
                };
        return link;
    }
}
