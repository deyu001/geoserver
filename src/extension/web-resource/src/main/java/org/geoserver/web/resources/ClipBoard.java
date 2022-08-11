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

package org.geoserver.web.resources;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.treeview.TreeNode;
import org.geoserver.web.treeview.TreeView;

/**
 * Holds Clipboard information for Resource TreeView and updates marks accordingly.
 *
 * @author Niels Charlier
 */
public class ClipBoard implements Serializable {

    private static final long serialVersionUID = -5996563694112082364L;

    private static final String MARK_COPY = "copy";

    private static final String MARK_CUT = "cut";

    /** The associated Resource TreeView */
    protected final TreeView<Resource> treeView;

    /** The resource nodes on the clipboard */
    protected Set<TreeNode<Resource>> items = new HashSet<>();

    /** Whether resource node was cut or copied. */
    protected boolean clipBoardIsCopy;

    /**
     * Constructor.
     *
     * @param treeView the tree view
     */
    public ClipBoard(TreeView<Resource> treeView) {
        this.treeView = treeView;
        treeView.registerMark(MARK_CUT);
        treeView.registerMark(MARK_COPY);
    }

    /**
     * Get the resource node that is on the clipboard.
     *
     * @return the resource node on the clipboard.
     */
    public Set<TreeNode<Resource>> getItems() {
        return Collections.unmodifiableSet(items);
    }

    /**
     * Get the cut/copy state.
     *
     * @return Whether resource node was cut or copied.
     */
    public boolean isCopy() {
        return clipBoardIsCopy;
    }

    /**
     * Put new nodes on the clipboard, replacing anything that is currently on it (or erase it).
     *
     * @param nodes the new cut/copied resource node
     * @param clipBoardIsCopy the cut/copy state
     * @param target AjaxRequestTarget to update the marks on the view
     */
    public void setItems(
            Collection<? extends TreeNode<Resource>> nodes,
            boolean clipBoardIsCopy,
            AjaxRequestTarget target) {
        treeView.clearMarked(MARK_CUT);
        treeView.clearMarked(MARK_COPY);
        for (TreeNode<Resource> clipBoardItem : items) {
            target.add(treeView.getNearestView(clipBoardItem));
        }
        items.clear();
        items.addAll(nodes);
        this.clipBoardIsCopy = clipBoardIsCopy;
        for (TreeNode<Resource> clipBoardItem : items) {
            treeView.addMarked(clipBoardIsCopy ? MARK_COPY : MARK_CUT, clipBoardItem);
        }
    }

    /**
     * Put new nodes on the clipboard, replacing anything that is currently on it (or erase it). The
     * cut/copy state is left unchanged.
     *
     * @param nodes the new cut/copied resource node ("null" to erase clipboard)
     * @param target AjaxRequestTarget to update the marks on the view
     */
    public void setItems(Collection<? extends TreeNode<Resource>> nodes, AjaxRequestTarget target) {
        setItems(nodes, clipBoardIsCopy, target);
    }

    /**
     * Add a node to the current clipboard.
     *
     * @param clipBoardItem the cut/copy state
     * @param target AjaxRequestTarget to update the marks on the view
     */
    public void addItem(TreeNode<Resource> clipBoardItem, AjaxRequestTarget target) {
        target.add(treeView.getNearestView(clipBoardItem));
        items.add(clipBoardItem);
        treeView.addMarked(clipBoardIsCopy ? MARK_COPY : MARK_CUT, clipBoardItem);
    }

    /** Clear all non-existing resources from the clip board. */
    public void clearRemoved() {
        Set<TreeNode<Resource>> newClipboard = new HashSet<>();
        for (TreeNode<Resource> node : items) {
            if (Resources.exists(node.getObject())) {
                newClipboard.add(node);
            }
        }
        items = newClipboard;
    }
}
