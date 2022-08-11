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

import java.io.IOException;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geotools.data.DataAccess;
import org.geotools.data.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Provides a form to configure a new geotools {@link DataAccess}
 *
 * @author Gabriel Roldan
 */
public class DataAccessNewPage extends AbstractDataAccessPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new datastore configuration page to create a new datastore of the given type
     *
     * @param dataStoreFactDisplayName the type of datastore to create, given by its factory display
     *     name
     */
    public DataAccessNewPage(final String dataStoreFactDisplayName) {
        super();

        final WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();
        if (defaultWs == null) {
            throw new IllegalStateException("No default Workspace configured");
        }
        final NamespaceInfo defaultNs = getCatalog().getDefaultNamespace();
        if (defaultNs == null) {
            throw new IllegalStateException("No default Namespace configured");
        }

        // Param[] parametersInfo = dsFact.getParametersInfo();
        // for (int i = 0; i < parametersInfo.length; i++) {
        // Serializable value;
        // final Param param = parametersInfo[i];
        // if (param.sample == null || param.sample instanceof Serializable) {
        // value = (Serializable) param.sample;
        // } else {
        // value = String.valueOf(param.sample);
        // }
        // }

        DataStoreInfo info = getCatalog().getFactory().createDataStore();
        info.setWorkspace(defaultWs);
        info.setEnabled(true);
        info.setType(dataStoreFactDisplayName);

        initUI(info);
    }

    /**
     * Callback method called when the submit button have been pressed and the parameters validation
     * has succeed.
     *
     * @see AbstractDataAccessPage#onSaveDataStore(Form)
     */
    @Override
    protected final void onSaveDataStore(
            final DataStoreInfo info, AjaxRequestTarget target, boolean doReturn)
            throws IllegalArgumentException {
        if (!storeEditPanel.onSave()) {
            return;
        }

        final Catalog catalog = getCatalog();

        // Cloning into "expandedStore" through the super class "clone" method
        DataStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        DataAccess<? extends FeatureType, ? extends Feature> dataStore;
        try {
            // REVISIT: this may need to be done after saving the DataStoreInfo
            dataStore = expandedStore.getDataStore(new NullProgressListener());
            dataStore.dispose();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error obtaining new data store", e);
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }
            throw new IllegalArgumentException(
                    "Error creating data store, check the parameters. Error message: " + message);
        }

        DataStoreInfo savedStore = catalog.getResourcePool().clone(info, true);
        try {
            // GeoServer Env substitution; validate first
            catalog.validate(savedStore, true).throwIfInvalid();

            // save a copy, so if NewLayerPage fails we can keep on editing this one without being
            // proxied

            // GeoServer Env substitution; force to *AVOID* resolving env placeholders...
            savedStore = catalog.getResourcePool().clone(info, false);
            // ...and save
            catalog.add(savedStore);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error adding data store to catalog", e);
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }

            throw new IllegalArgumentException(
                    "Error creating data store with the provided parameters: " + message);
        }

        final NewLayerPage newLayerPage;
        try {
            // The ID is assigned by the catalog and therefore cannot be cloned
            newLayerPage = new NewLayerPage(savedStore.getId());
        } catch (RuntimeException e) {
            try {
                catalog.remove(expandedStore);
                catalog.remove(savedStore);
            } catch (Exception removeEx) {
                LOGGER.log(Level.WARNING, "Error removing just added datastore!", e);
            }
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        if (doReturn) {
            setResponsePage(newLayerPage);
        } else {
            setResponsePage(new DataAccessEditPage(savedStore.getId()));
        }
    }
}
