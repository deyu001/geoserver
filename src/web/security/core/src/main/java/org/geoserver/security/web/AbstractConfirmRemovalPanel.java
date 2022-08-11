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

package org.geoserver.security.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class AbstractConfirmRemovalPanel<T> extends Panel {

    private static final long serialVersionUID = 1L;

    List<T> roots;
    List<IModel<String>> problems;

    @SafeVarargs
    public AbstractConfirmRemovalPanel(String id, T... roots) {
        this(id, null, Arrays.asList(roots));
    }

    @SafeVarargs
    public AbstractConfirmRemovalPanel(String id, Model<?> model, T... roots) {
        this(id, model, Arrays.asList(roots));
    }

    public AbstractConfirmRemovalPanel(String id, List<T> roots) {
        this(id, null, roots);
    }

    public AbstractConfirmRemovalPanel(String id, Model<?> model, List<T> rootObjects) {
        super(id, model);
        setRootObjectsAndProblems(rootObjects);

        // add roots
        WebMarkupContainer root = new WebMarkupContainer("rootObjects");
        // root.add(new Label("rootObjectNames", names(roots)));
        // root.setVisible(!roots.isEmpty());
        add(root);

        // removed objects root (we show it if any removed object is on the list)
        WebMarkupContainer removed = new WebMarkupContainer("removedObjects");
        add(removed);

        // removed
        WebMarkupContainer rulesRemoved = new WebMarkupContainer("rulesRemoved");
        removed.add(rulesRemoved);
        if (roots.isEmpty()) removed.setVisible(false);
        else {
            rulesRemoved.add(
                    new ListView<String>("rules", names(roots)) {
                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Label("name", item.getModelObject()));
                        }
                    });
        }

        WebMarkupContainer problematic = new WebMarkupContainer("problematicObjects");
        add(problematic);

        WebMarkupContainer rulesNotRemoved = new WebMarkupContainer("rulesNotRemoved");
        problematic.add(rulesNotRemoved);
        if (problems.isEmpty()) problematic.setVisible(false);
        else {
            rulesNotRemoved.add(
                    new ListView<String>("problems", problems(problems)) {
                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Label("name", item.getModelObject()));
                        }
                    });
        }
    }

    void setRootObjectsAndProblems(List<T> rootObjects) {
        roots = new ArrayList<>();
        problems = new ArrayList<>();
        for (T obj : rootObjects) {
            IModel<String> model = canRemove(obj);
            if (model == null) roots.add(obj);
            else problems.add(model);
        }
    }

    List<String> problems(List<IModel<String>> objects) {
        List<String> l = new ArrayList<>();
        for (IModel<String> m : objects) {
            l.add(m.getObject());
        }
        return l;
    }

    List<String> names(List<T> objects) {
        List<String> l = new ArrayList<>();
        for (T obj : objects) {
            l.add(name(obj));
        }
        return l;
    }

    String name(T object) {
        try {
            return getConfirmationMessage(object);
        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        } catch (Exception e) {
            throw new RuntimeException(
                    "A data object that does not have "
                            + "a 'name' property has been used, this is unexpected",
                    e);
        }
    }

    protected IModel<String> canRemove(T data) {
        return null;
    }

    protected abstract String getConfirmationMessage(T object) throws Exception;

    public List<T> getRoots() {
        return roots;
    }
}
