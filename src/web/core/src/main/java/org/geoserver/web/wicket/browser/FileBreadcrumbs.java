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

package org.geoserver.web.wicket.browser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A panel showing the path between the root directory and the current directory as a set of links
 * separated by "/", much like breadcrumbs in a web site.
 *
 * @author Andrea Aime - OpenGeo
 */
public abstract class FileBreadcrumbs extends Panel {
    private static final long serialVersionUID = 2821319341957784628L;

    IModel<File> rootFile;

    public FileBreadcrumbs(String id, IModel<File> rootFile, IModel<File> currentFile) {
        super(id, currentFile);

        this.rootFile = rootFile;
        add(
                new ListView<File>("path", new BreadcrumbModel(rootFile, currentFile)) {

                    private static final long serialVersionUID = -855582301247703291L;

                    @Override
                    protected void populateItem(ListItem<File> item) {
                        File file = item.getModelObject();
                        boolean last = item.getIndex() == getList().size() - 1;

                        // the link to the current path item
                        Label name = new Label("pathItem", file.getName() + "/");
                        Link<File> link =
                                new IndicatingAjaxFallbackLink<File>(
                                        "pathItemLink", item.getModel()) {

                                    private static final long serialVersionUID =
                                            4295991386838610752L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        pathItemClicked(getModelObject(), target);
                                    }
                                };
                        link.add(name);
                        item.add(link);
                        link.setEnabled(!last);
                    }
                });
    }

    public void setRootFile(File root) {
        rootFile.setObject(root);
    }

    public void setSelection(File selection) {
        setDefaultModelObject(selection);
    }

    protected abstract void pathItemClicked(File file, AjaxRequestTarget target);

    static class BreadcrumbModel implements IModel<List<File>> {
        private static final long serialVersionUID = -3497123851146725406L;

        IModel<File> rootFileModel;

        IModel<File> currentFileModel;

        public BreadcrumbModel(IModel<File> rootFileModel, IModel<File> currentFileModel) {
            this.rootFileModel = rootFileModel;
            this.currentFileModel = currentFileModel;
        }

        public List<File> getObject() {
            File root = rootFileModel.getObject();
            File current = currentFileModel.getObject();

            // get all directories between current and root
            List<File> files = new ArrayList<>();
            while (current != null && !current.equals(root)) {
                files.add(current);
                current = current.getParentFile();
            }
            if (current != null && current.equals(root)) files.add(root);
            // reverse the order, we want them ordered from root
            // to current
            Collections.reverse(files);

            return files;
        }

        public void setObject(List<File> object) {
            throw new UnsupportedOperationException("This model cannot be set!");
        }

        public void detach() {
            // nothing to do here
        }
    }
}
