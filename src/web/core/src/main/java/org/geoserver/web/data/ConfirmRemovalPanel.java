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

import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.DELETE;
import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.EXTRA_STYLE_REMOVED;
import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.GROUP_CHANGED;
import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.STYLE_RESET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.CascadeRemovalReporter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

@SuppressWarnings("serial")
public class ConfirmRemovalPanel extends Panel {
    List<? extends CatalogInfo> roots;

    public ConfirmRemovalPanel(String id, CatalogInfo... roots) {
        this(id, Arrays.asList(roots));
    }

    public ConfirmRemovalPanel(String id, List<? extends CatalogInfo> roots) {
        super(id);
        this.roots = roots;

        // track objects that could not be removed
        Map<CatalogInfo, StringResourceModel> notRemoved = new HashMap<>();

        // collect the objects that will be removed (besides the roots)
        Catalog catalog = GeoServerApplication.get().getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);
        for (Iterator<? extends CatalogInfo> i = roots.iterator(); i.hasNext(); ) {
            CatalogInfo root = i.next();
            StringResourceModel reason = canRemove(root);
            if (reason != null) {
                notRemoved.put(root, reason);
                i.remove();
            } else {
                root.accept(visitor);
            }
        }
        visitor.removeAll(roots);

        // add roots
        WebMarkupContainer root = new WebMarkupContainer("rootObjects");
        root.add(new Label("rootObjectNames", names(roots)));
        root.setVisible(!roots.isEmpty());
        add(root);

        // objects that could not be removed
        WebMarkupContainer nr = new WebMarkupContainer("notRemovedObjects");
        nr.setVisible(!notRemoved.isEmpty());
        nr.add(notRemovedList(notRemoved));
        add(nr);

        // removed objects root (we show it if any removed object is on the list)
        WebMarkupContainer removed = new WebMarkupContainer("removedObjects");
        List<CatalogInfo> cascaded = visitor.getObjects(CatalogInfo.class, DELETE);
        // remove the resources, they are cascaded, but won't be show in the UI
        for (Iterator<CatalogInfo> it = cascaded.iterator(); it.hasNext(); ) {
            CatalogInfo catalogInfo = it.next();
            if (catalogInfo instanceof ResourceInfo) it.remove();
        }
        removed.setVisible(!cascaded.isEmpty());
        add(removed);

        // removed workspaces
        WebMarkupContainer wsr = new WebMarkupContainer("workspacesRemoved");
        removed.add(wsr);
        List<WorkspaceInfo> workspaces = visitor.getObjects(WorkspaceInfo.class);
        if (workspaces.isEmpty()) wsr.setVisible(false);
        wsr.add(new Label("workspaces", names(workspaces)));

        // removed stores
        WebMarkupContainer str = new WebMarkupContainer("storesRemoved");
        removed.add(str);
        List<StoreInfo> stores = visitor.getObjects(StoreInfo.class);
        if (stores.isEmpty()) str.setVisible(false);
        str.add(new Label("stores", names(stores)));

        // removed layers
        WebMarkupContainer lar = new WebMarkupContainer("layersRemoved");
        removed.add(lar);
        List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, DELETE);
        if (layers.isEmpty()) lar.setVisible(false);
        lar.add(new Label("layers", names(layers)));

        // removed groups
        WebMarkupContainer grr = new WebMarkupContainer("groupsRemoved");
        removed.add(grr);
        List<LayerGroupInfo> groups = visitor.getObjects(LayerGroupInfo.class, DELETE);
        if (groups.isEmpty()) grr.setVisible(false);
        grr.add(new Label("groups", names(groups)));

        // removed styles
        WebMarkupContainer syr = new WebMarkupContainer("stylesRemoved");
        removed.add(syr);
        List<StyleInfo> styles = visitor.getObjects(StyleInfo.class, DELETE);
        if (styles.isEmpty()) syr.setVisible(false);
        syr.add(new Label("styles", names(styles)));

        // modified objects root (we show it if any modified object is on the list)
        WebMarkupContainer modified = new WebMarkupContainer("modifiedObjects");
        modified.setVisible(
                visitor.getObjects(null, EXTRA_STYLE_REMOVED, GROUP_CHANGED, STYLE_RESET).size()
                        > 0);
        add(modified);

        // layers modified
        WebMarkupContainer lam = new WebMarkupContainer("layersModified");
        modified.add(lam);
        layers = visitor.getObjects(LayerInfo.class, STYLE_RESET, EXTRA_STYLE_REMOVED);
        if (layers.isEmpty()) lam.setVisible(false);
        lam.add(new Label("layers", names(layers)));

        // groups modified
        WebMarkupContainer grm = new WebMarkupContainer("groupsModified");
        modified.add(grm);
        groups = visitor.getObjects(LayerGroupInfo.class, GROUP_CHANGED);
        if (groups.isEmpty()) grm.setVisible(false);
        grm.add(new Label("groups", names(groups)));
    }

    public List<? extends CatalogInfo> getRoots() {
        return roots;
    }

    String names(List<?> objects) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < objects.size(); i++) {
            sb.append(name(objects.get(i)));
            if (i < (objects.size() - 1)) sb.append(", ");
        }
        return sb.toString();
    }

    String name(Object object) {
        try {
            return BeanUtils.getProperty(object, "name");
        } catch (Exception e) {
            throw new RuntimeException(
                    "A catalog object that does not have "
                            + "a 'name' property has been used, this is unexpected",
                    e);
        }
    }

    ListView<CatalogInfo> notRemovedList(final Map<CatalogInfo, StringResourceModel> notRemoved) {
        List<CatalogInfo> items = new ArrayList<>(notRemoved.keySet());
        ListView<CatalogInfo> lv =
                new ListView<CatalogInfo>("notRemovedList", items) {

                    @Override
                    protected void populateItem(ListItem<CatalogInfo> item) {
                        CatalogInfo object = item.getModelObject();
                        StringResourceModel reason = notRemoved.get(object);
                        item.add(new Label("name", name(object)));
                        item.add(new Label("reason", reason));
                    }
                };
        return lv;
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
