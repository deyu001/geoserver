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

package org.geoserver.backuprestore.web;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/** Detachable model for a specific store. */
public class StoreModel<T extends StoreInfo> extends LoadableDetachableModel<T> {

    private static final long serialVersionUID = -2008934085507417813L;

    private ResourceFilePanel resourceFilePanel;
    IModel workspace;
    String name;

    private StoreModel(T store) {
        super(store);
        setObject(store);
    }

    public StoreModel(ResourceFilePanel resourceFilePanel, T store) {
        this(store);
        this.resourceFilePanel = resourceFilePanel;
    }

    public void setObject(T store) {
        super.setObject(store);
        if (store != null) {
            workspace = new WorkspaceModel(resourceFilePanel, store.getWorkspace());
            name = store.getName();
        } else {
            name = null;
        }
    };

    @Override
    protected T load() {
        if (workspace == null) {
            return null;
        }
        if (name == null) {
            return null;
        }

        if (resourceFilePanel != null
                && resourceFilePanel.getStores() != null
                && !resourceFilePanel.getStores().isEmpty()
                && workspace.getObject() != null
                && resourceFilePanel
                        .getStores()
                        .containsKey(((WorkspaceInfo) workspace.getObject()).getName())) {
            for (StoreInfo st :
                    resourceFilePanel
                            .getStores()
                            .get(((WorkspaceInfo) workspace.getObject()).getName())) {
                if (st.getName().equals(name)) {
                    return (T) st;
                }
            }
        }

        StoreInfo store =
                (T)
                        GeoServerApplication.get()
                                .getCatalog()
                                .getStoreByName(
                                        (WorkspaceInfo) workspace.getObject(),
                                        name,
                                        StoreInfo.class);
        if (store != null) {
            return (T) store;
        }
        return getObject();
    }

    /* (non-Javadoc)
     * @see org.apache.wicket.model.LoadableDetachableModel#detach()
     */
    @Override
    public void detach() {
        super.detach();
        if (workspace != null) {
            workspace.detach();
        }
    }
}
