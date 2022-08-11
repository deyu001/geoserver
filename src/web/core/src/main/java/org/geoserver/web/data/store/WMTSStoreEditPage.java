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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.ows.wmts.WebMapTileServer;

@SuppressWarnings("serial")
public class WMTSStoreEditPage extends AbstractWMTSStorePage {

    public static final String STORE_NAME = "storeName";
    public static final String WS_NAME = "wsName";

    /** Uses a "name" parameter to locate the datastore */
    public WMTSStoreEditPage(PageParameters parameters) {
        String wsName = parameters.get(WS_NAME).toOptionalString();
        String storeName = parameters.get(STORE_NAME).toString();
        WMTSStoreInfo store = getCatalog().getStoreByName(wsName, storeName, WMTSStoreInfo.class);
        initUI(store);
    }

    /** Creates a new edit page directly from a store object. */
    public WMTSStoreEditPage(WMTSStoreInfo store) {
        initUI(store);
    }

    @Override
    protected void onSave(WMTSStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException {
        if (!info.isEnabled()) {
            doSaveStore(info);
        } else {
            try {
                // try to see if we can connect
                getCatalog().getResourcePool().clear(info);
                // do not call info.getWebMapServer cause it ends up calling
                // resourcepool.getWebMapServer with the unproxied instance (old values)
                // info.getWebMapServer(null).getCapabilities();
                WebMapTileServer wmts = getCatalog().getResourcePool().getWebMapTileServer(info);
                wmts.getCapabilities();
                doSaveStore(info);
            } catch (Exception e) {
                confirmSaveOnConnectionFailure(info, target, e);
            }
        }
    }

    /**
     * Performs the save of the store.
     *
     * <p>This method may be subclasses to provide custom save functionality.
     */
    protected void doSaveStore(WMTSStoreInfo info) {
        Catalog catalog = getCatalog();

        // Cloning into "expandedStore" through the super class "clone" method
        WMTSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        getCatalog().validate(expandedStore, false).throwIfInvalid();

        getCatalog().save(info);
        doReturn(StorePage.class);
    }

    private void confirmSaveOnConnectionFailure(
            final WMTSStoreInfo info,
            final AjaxRequestTarget requestTarget,
            final Exception error) {

        getCatalog().getResourcePool().clear(info);

        final String exceptionMessage;
        {
            String message = error.getMessage();
            if (message == null && error.getCause() != null) {
                message = error.getCause().getMessage();
            }
            exceptionMessage = message;
        }

        dialog.showOkCancel(
                requestTarget,
                new GeoServerDialog.DialogDelegate() {

                    boolean accepted = false;

                    @Override
                    protected Component getContents(String id) {
                        return new StoreConnectionFailedInformationPanel(
                                id, info.getName(), exceptionMessage);
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        doSaveStore(info);
                        accepted = true;
                        return true;
                    }

                    @Override
                    protected boolean onCancel(AjaxRequestTarget target) {
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        if (accepted) {
                            doReturn(StorePage.class);
                        }
                    }
                });
    }
}
