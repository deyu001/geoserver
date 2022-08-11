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

import java.util.logging.Level;
import javax.management.RuntimeErrorException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.layer.NewLayerPage;
import org.opengis.coverage.grid.Format;

/**
 * Supports coverage store configuration
 *
 * @author Andrea Aime
 * @author Gabriel Roldan
 */
public class CoverageStoreNewPage extends AbstractCoverageStorePage {

    /**
     * @param coverageFactoryName the {@link Format#getName() name} of the format to create a new
     *     raster coverage for
     */
    public CoverageStoreNewPage(final String coverageFactoryName) {
        Catalog catalog = getCatalog();
        final WorkspaceInfo workspace = catalog.getDefaultWorkspace();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        store.setWorkspace(workspace);
        store.setType(coverageFactoryName);
        store.setEnabled(true);
        store.setURL("file:data/example.extension");

        initUI(store);
    }

    @Override
    protected void onSave(final CoverageStoreInfo info, AjaxRequestTarget target, boolean doReturn)
            throws IllegalArgumentException {
        final Catalog catalog = getCatalog();

        /*
         * Try saving a copy of it so if the process fails somehow the original "info" does not end
         * up with an id set
         */
        CoverageStoreInfo expandedStore = getCatalog().getResourcePool().clone(info, true);
        CoverageStoreInfo savedStore = catalog.getFactory().createCoverageStore();

        // GR: this shouldn't fail, the Catalog.save(StoreInfo) API does not declare any action in
        // case
        // of a failure!... strange, why a save can't fail?
        // Still, be cautious and wrap it in a try/catch block so the page does not blow up
        try {
            // GeoServer Env substitution; validate first
            catalog.validate(expandedStore, false).throwIfInvalid();

            // GeoServer Env substitution; force to *AVOID* resolving env placeholders...
            savedStore = catalog.getResourcePool().clone(info, false);
            // ... and save
            catalog.save(savedStore);
        } catch (RuntimeException e) {
            LOGGER.log(Level.INFO, "Adding the store for " + info.getURL(), e);
            throw new IllegalArgumentException(
                    "The coverage store could not be saved. Failure message: " + e.getMessage());
        }

        onSuccessfulSave(info, catalog, savedStore, doReturn);
    }

    protected void onSuccessfulSave(
            final CoverageStoreInfo info,
            final Catalog catalog,
            CoverageStoreInfo savedStore,
            boolean doReturn) {
        if (doReturn) {
            // the StoreInfo save succeeded... try to present the list of coverages (well, _the_
            // coverage while the getotools coverage api does not allow for more than one
            NewLayerPage layerChooserPage;
            try {
                catalog.getResourcePool().clone(savedStore, true);
                // The ID is assigned by the catalog and therefore cannot be cloned
                layerChooserPage = new NewLayerPage(savedStore.getId());
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.INFO,
                        "Getting list of coverages for saved store " + info.getURL(),
                        e);
                // doh, can't present the list of coverages, means saving the StoreInfo is
                // meaningless.
                try { // be extra cautious
                    catalog.remove(savedStore);
                } catch (RuntimeErrorException shouldNotHappen) {
                    LOGGER.log(Level.WARNING, "Can't remove CoverageStoreInfo after adding it!", e);
                }
                // tell the caller why we failed...
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            setResponsePage(layerChooserPage);
        } else {
            setResponsePage(new CoverageStoreEditPage(savedStore.getId()));
        }
    }
}
