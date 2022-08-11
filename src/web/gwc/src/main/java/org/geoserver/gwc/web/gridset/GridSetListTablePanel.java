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

package org.geoserver.gwc.web.gridset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.grid.GridSet;

/**
 * Panel listing the configured GridSet object on a table
 *
 * @author groldan
 * @see GridSetTableProvider
 */
public abstract class GridSetListTablePanel extends GeoServerTablePanel<GridSet> {

    private static final long serialVersionUID = 5957961031378924960L;

    public GridSetListTablePanel(
            final String id, final GridSetTableProvider provider, final boolean selectable) {
        super(id, provider, selectable);
    }

    @Override
    protected Component getComponentForProperty(
            final String id, final IModel<GridSet> itemModel, final Property<GridSet> property) {

        if (property == GridSetTableProvider.NAME) {
            GridSet gridSet = itemModel.getObject();
            return nameLink(id, gridSet);

        } else if (property == GridSetTableProvider.EPSG_CODE) {

            String epsgCode = (String) property.getModel(itemModel).getObject();
            return new Label(id, epsgCode);

        } else if (property == GridSetTableProvider.TILE_DIMENSION) {

            String tileDimension = (String) property.getModel(itemModel).getObject();
            return new Label(id, tileDimension);

        } else if (property == GridSetTableProvider.ZOOM_LEVELS) {

            Integer zoomLevels = (Integer) property.getModel(itemModel).getObject();
            return new Label(id, zoomLevels.toString());

        } else if (property == GridSetTableProvider.QUOTA_USED) {

            Quota usedQuota = (Quota) property.getModel(itemModel).getObject();
            String quotaStr = usedQuota == null ? "N/A" : usedQuota.toNiceString();
            return new Label(id, quotaStr);

        } else if (property == GridSetTableProvider.ACTION_LINK) {
            String gridSetName = (String) property.getModel(itemModel).getObject();

            Component actionLink = actionLink(id, gridSetName);

            return actionLink;
        }

        throw new IllegalArgumentException("Unknown property: " + property.getName());
    }

    protected abstract Component nameLink(final String id, final GridSet gridSet);

    protected abstract Component actionLink(final String id, String gridSetName);

    /**
     * Overrides to return a disabled and non selectable checkbox if the GridSet for the item is an
     * internally defined one
     *
     * @see org.geoserver.web.wicket.GeoServerTablePanel#selectOneCheckbox
     */
    @Override
    protected CheckBox selectOneCheckbox(Item<GridSet> item) {
        CheckBox cb = super.selectOneCheckbox(item);

        GridSet gs = item.getModelObject();

        String name = gs.getName();
        final boolean internal = GWC.get().isInternalGridSet(name);
        if (internal) {
            cb.setEnabled(false);
            cb.setModelObject(Boolean.FALSE);
        }
        return cb;
    }

    /**
     * Overrides to remove any internal gridset from the list
     *
     * @see org.geoserver.web.wicket.GeoServerTablePanel#getSelection()
     */
    @Override
    public List<GridSet> getSelection() {
        List<GridSet> selection = new ArrayList<>(super.getSelection());
        for (Iterator<GridSet> it = selection.iterator(); it.hasNext(); ) {
            GridSet g = it.next();
            if (GWC.get().isInternalGridSet(g.getName())) {
                it.remove();
            }
        }
        return selection;
    }
}
