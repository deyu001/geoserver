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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StorePanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.grid.GridSet;

/**
 * Page listing all the available GridSets, following the usual filter/sort/page approach and
 * providing ways to bulk delete gridsets and to add new ones.
 *
 * <p>
 * <!-- Implementation detail:
 * <p>
 * <ul>
 * <li> {@link GridSetListTablePanel} is the table of avaiable gridsets
 * <li>uses a {@link GridSetTableProvider} to get the current gridsets and table properties
 * <li>which in turn uses a {@link GridSetDetachableModel} to get each gridset out of
 * {@link GWC#getGridSetBroker()}
 * </ul>
 *
 * -->
 *
 * @see StorePanel
 */
@SuppressWarnings("serial")
public class GridSetsPage extends GeoServerSecuredPage {

    private GridSetsPanel table;

    private SelectionRemovalLink removal;

    private GeoServerDialog dialog;

    private class GridSetsPanel extends GridSetListTablePanel {

        public GridSetsPanel(String id, GridSetTableProvider provider) {
            super(id, provider, true);
        }

        @Override
        protected void onSelectionUpdate(AjaxRequestTarget target) {
            removal.setEnabled(table.getSelection().size() > 0);
            target.add(removal);
        }

        @Override
        protected Component nameLink(final String id, final GridSet gridSet) {

            final String gridSetName = gridSet.getName();
            final boolean isInternal =
                    GWC.get().getGridSetBroker().getEmbeddedNames().contains(gridSetName);

            SimpleBookmarkableLink link;

            link =
                    new SimpleBookmarkableLink(
                            id,
                            GridSetEditPage.class,
                            new Model<>(gridSetName),
                            AbstractGridSetPage.GRIDSET_NAME,
                            gridSetName);

            if (isInternal) {
                link.add(new AttributeModifier("style", new Model<>("font-style: italic;")));
                link.add(
                        new AttributeModifier(
                                "title", new ResourceModel("nameLink.titleInternalGridSet")));
            } else {
                link.add(new AttributeModifier("title", new ResourceModel("nameLink.title")));
            }
            return link;
        }

        @Override
        protected Component actionLink(final String id, String gridSetName) {
            SimpleBookmarkableLink link;
            link =
                    new SimpleBookmarkableLink(
                            id,
                            GridSetNewPage.class,
                            new ResourceModel("templateLink"),
                            AbstractGridSetPage.GRIDSET_TEMPLATE_NAME,
                            gridSetName);

            link.add(new AttributeModifier("title", new ResourceModel("templateLink.title")));
            return link;
        }
    }

    public GridSetsPage() {

        GridSetTableProvider provider =
                new GridSetTableProvider() {
                    @Override
                    public List<GridSet> getItems() {
                        return new ArrayList<>(GWC.get().getGridSetBroker().getGridSets());
                    }
                };
        // the table, and wire up selection change
        table = new GridSetsPanel("table", provider);
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        BookmarkablePageLink newLink = new BookmarkablePageLink("addNew", GridSetNewPage.class);
        newLink.add(new AttributeModifier("title", new ResourceModel("addNew.title")));
        header.add(newLink);

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        removal.add(new AttributeModifier("title", new ResourceModel("removalLink.title")));

        return header;
    }

    private class SelectionRemovalLink extends AjaxLink<GridSet> {

        GeoServerTablePanel<GridSet> gridsets;

        GeoServerDialog dialog;

        public SelectionRemovalLink(
                final String id,
                final GeoServerTablePanel<GridSet> gridsets,
                final GeoServerDialog dialog) {
            super(id);
            this.gridsets = gridsets;
            this.dialog = dialog;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            // see if the user selected anything
            List<GridSet> selection = gridsets.getSelection();
            if (selection.isEmpty()) {
                return;
            }

            final Set<String> selectedGridsetIds = new HashSet<>();
            for (GridSet gset : selection) {
                selectedGridsetIds.add(gset.getName());
            }

            dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

            // if there is something to cancel, let's warn the user about what
            // could go wrong, and if the user accepts, let's delete what's needed
            dialog.showOkCancel(
                    target,
                    new GeoServerDialog.DialogDelegate() {

                        @Override
                        protected Component getContents(final String id) {
                            final GWC gwc = GWC.get();

                            final int count = selectedGridsetIds.size();

                            Quota totalQuota = new Quota();

                            for (String gridsetId : selectedGridsetIds) {
                                Quota usedQuotaByGridSet = gwc.getUsedQuotaByGridSet(gridsetId);
                                if (usedQuotaByGridSet != null) {
                                    totalQuota.add(usedQuotaByGridSet);
                                }
                            }

                            final Set<String> affectedLayers =
                                    gwc.getLayerNamesForGridSets(selectedGridsetIds);

                            IModel<String> confirmModel =
                                    new ParamResourceModel(
                                            "GridSetsPage.confirmGridsetsDelete",
                                            GridSetsPage.this,
                                            String.valueOf(count),
                                            String.valueOf(affectedLayers.size()),
                                            totalQuota.toNiceString());

                            Label confirmMessage = new Label(id, confirmModel);
                            confirmMessage.setEscapeModelStrings(false); // allow some html markup
                            return confirmMessage;
                        }

                        @Override
                        protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                            // cascade delete the whole selection
                            GWC gwc = GWC.get();
                            try {
                                gwc.removeGridSets(selectedGridsetIds);
                            } catch (Exception e) {
                                getPage().error(e.getMessage());
                                LOGGER.log(Level.WARNING, e.getMessage(), e);
                            }
                            gridsets.clearSelection();
                            return true;
                        }

                        @Override
                        public void onClose(AjaxRequestTarget target) {
                            // if the selection has been cleared out it's sign a deletion
                            // occurred, so refresh the table
                            if (gridsets.getSelection().size() == 0) {
                                setEnabled(false);
                                target.add(SelectionRemovalLink.this);
                                target.add(gridsets);
                            }
                        }
                    });
        }
    }
}
