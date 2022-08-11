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

package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * A Form validator that takes the workspace and store name form components and validates there is
 * not an existing {@link StoreInfo} in the selected workspace with the same name as the one
 * assigned through the store name form component.
 *
 * @author Andrea Aime - OpenGeo
 * @author Gabriel Roldan - OpenGeo
 */
@SuppressWarnings("serial")
public class StoreNameValidator implements IFormValidator {

    FormComponent workspaceComponent;

    FormComponent storeNameComponent;

    private String edittingStoreId;

    private boolean required;

    /**
     * @param workspaceFormComponent the form component for the {@link WorkspaceInfo} assigned to
     *     the {@link StoreInfo} being edited
     * @param storeNameFormComponent the form component for the name assigned to the {@link
     *     StoreInfo}
     * @param edittingStoreId the id for the store being edited. May be {@code null} if we're
     *     talking of a new Store
     */
    public StoreNameValidator(
            final FormComponent workspaceFormComponent,
            final FormComponent storeNameFormComponent,
            final String edittingStoreId) {
        this(workspaceFormComponent, storeNameFormComponent, edittingStoreId, true);
    }

    /**
     * @param workspaceFormComponent the form component for the {@link WorkspaceInfo} assigned to
     *     the {@link StoreInfo} being edited
     * @param storeNameFormComponent the form component for the name assigned to the {@link
     *     StoreInfo}
     * @param edittingStoreId the id for the store being edited. May be {@code null} if we're
     *     talking of a new Store
     * @param required true if store name is required
     */
    public StoreNameValidator(
            final FormComponent workspaceFormComponent,
            final FormComponent storeNameFormComponent,
            final String edittingStoreId,
            boolean required) {
        this.workspaceComponent = workspaceFormComponent;
        this.storeNameComponent = storeNameFormComponent;
        this.edittingStoreId = edittingStoreId;
        this.required = required;
    }

    @Override
    public FormComponent[] getDependentFormComponents() {
        return new FormComponent[] {workspaceComponent, storeNameComponent};
    }

    /**
     * Performs the cross validation between the selected workspace and the assigned store name
     *
     * <p>If there's already a {@link StoreInfo} in the selected workspace with the same name as the
     * chosen one, then the store name form component is set with a proper {@link IValidationError
     * error message}
     *
     * @see IFormValidator#validate(Form)
     */
    @Override
    public void validate(final Form form) {
        final FormComponent[] components = getDependentFormComponents();
        final FormComponent wsComponent = components[0];
        final FormComponent nameComponent = components[1];

        WorkspaceInfo workspace = (WorkspaceInfo) wsComponent.getConvertedInput();
        String name = (String) nameComponent.getConvertedInput();

        if (name == null) {
            if (required) {
                nameComponent.error(
                        new ValidationError("StoreNameValidator.storeNameRequired")
                                .addKey("StoreNameValidator.storeNameRequired"));
            }
            return;
        }

        Catalog catalog = GeoServerApplication.get().getCatalog();

        final StoreInfo existing = catalog.getStoreByName(workspace, name, StoreInfo.class);
        if (existing != null) {
            final String existingId = existing.getId();
            if (!existingId.equals(edittingStoreId)) {
                IValidationError error =
                        new ValidationError("StoreNameValidator.storeExistsInWorkspace")
                                .addKey("StoreNameValidator.storeExistsInWorkspace")
                                .setVariable("workspace", workspace.getName())
                                .setVariable("storeName", name);
                nameComponent.error(error);
            }
        }
    }
}
