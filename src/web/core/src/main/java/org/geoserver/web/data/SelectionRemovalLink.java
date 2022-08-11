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

package org.geoserver.web.data;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * A reusable cascading, multiple removal link. Assumes the presence of a table panel filled with
 * catalog objects and a {@link GeoServerDialog} to be used for reporting the objects that will be
 * affected by the removal
 */
@SuppressWarnings("serial")
public class SelectionRemovalLink extends AjaxLink<Void> {

    GeoServerTablePanel<? extends CatalogInfo> catalogObjects;
    GeoServerDialog dialog;

    public SelectionRemovalLink(
            String id,
            GeoServerTablePanel<? extends CatalogInfo> catalogObjects,
            GeoServerDialog dialog) {
        super(id);
        this.catalogObjects = catalogObjects;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        // see if the user selected anything
        final List<? extends CatalogInfo> selection = catalogObjects.getSelection();
        if (selection.isEmpty()) return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(
                target,
                new GeoServerDialog.DialogDelegate() {
                    protected Component getContents(String id) {
                        // show a confirmation panel for all the objects we have to remove
                        return new ConfirmRemovalPanel(id, selection) {
                            @Override
                            protected StringResourceModel canRemove(CatalogInfo info) {
                                return SelectionRemovalLink.this.canRemove(info);
                            }
                        };
                    }

                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        // cascade delete the whole selection
                        Catalog catalog = GeoServerApplication.get().getCatalog();
                        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
                        for (CatalogInfo ci : selection) {
                            ci.accept(visitor);
                        }

                        // the deletion will have changed what we see in the page
                        // so better clear out the selection
                        catalogObjects.clearSelection();
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        // if the selection has been cleared out it's sign a deletion
                        // occurred, so refresh the table
                        if (catalogObjects.getSelection().size() == 0) {
                            setEnabled(false);
                            target.add(SelectionRemovalLink.this);
                            target.add(catalogObjects);
                        }
                    }
                });
    }

    /**
     * Determines if a catalog object can be removed or not.
     *
     * <p>This method returns non-null in cases where the object should not be be removed. The
     * return value should be a description or reason of why the object can not be removed.
     *
     * @param info The object to be removed.
     * @return A message stating why the object can not be removed, or null to indicate that it can
     *     be removed.
     */
    protected StringResourceModel canRemove(CatalogInfo info) {
        return null;
    }
}
