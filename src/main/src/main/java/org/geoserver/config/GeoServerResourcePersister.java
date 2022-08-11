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

package org.geoserver.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

/**
 * Handles the persistence of addition resources when changes happen to the catalog, such as rename,
 * remove and change of workspace.
 */
public class GeoServerResourcePersister implements CatalogListener {

    /** logging instance */
    static Logger LOGGER = Logging.getLogger("org.geoserver.config");

    Catalog catalog;
    GeoServerResourceLoader rl;
    GeoServerDataDirectory dd;

    public GeoServerResourcePersister(Catalog catalog) {
        this.catalog = catalog;
        this.rl = catalog.getResourceLoader();
        this.dd = new GeoServerDataDirectory(rl);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

    @Override
    public void reloaded() {}

    public void handleModifyEvent(CatalogModifyEvent event) {
        Object source = event.getSource();

        try {
            // handle the case of a style changing workspace
            if (source instanceof StyleInfo) {
                int i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    WorkspaceInfo oldWorkspace = (WorkspaceInfo) event.getOldValues().get(i);
                    WorkspaceInfo newWorkspace =
                            ResolvingProxy.resolve(
                                    catalog, (WorkspaceInfo) event.getNewValues().get(i));
                    Resource oldDir = dd.getStyles(oldWorkspace);
                    Resource newDir = dd.getStyles(newWorkspace);
                    URI oldDirURI = new URI(oldDir.path());

                    // look for any resource files (image, etc...) and copy them over, don't move
                    // since they could be shared among other styles
                    for (Resource old : dd.additionalStyleResources((StyleInfo) source)) {
                        if (old.getType() != Type.UNDEFINED) {
                            URI oldURI = new URI(old.path());
                            final URI relative = oldDirURI.relativize(oldURI);
                            final Resource target = newDir.get(relative.getPath()).parent();
                            copyResToDir(old, target);
                        }
                    }

                    // move over the config file and the sld
                    for (Resource old : baseResources((StyleInfo) source)) {
                        if (old.getType() != Type.UNDEFINED) {
                            moveResToDir(old, newDir);
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleRemoveEvent(CatalogRemoveEvent event) {
        Object source = event.getSource();
        try {
            if (source instanceof StyleInfo) {
                removeStyle((StyleInfo) source);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeStyle(StyleInfo s) throws IOException {
        Resource sld = dd.style(s);
        if (Resources.exists(sld)) {
            Resource sldBackup = dd.get(sld.path() + ".bak");
            int i = 1;
            while (Resources.exists(sldBackup)) {
                sldBackup = dd.get(sld.path() + ".bak." + i++);
            }
            LOGGER.fine("Removing the SLD as well but making backup " + sldBackup.name());
            sld.renameTo(sldBackup);
        }
    }

    /*
     * returns the SLD file as well
     */
    private List<Resource> baseResources(StyleInfo s) throws IOException {
        List<Resource> list = Arrays.asList(dd.config(s), dd.style(s));
        return list;
    }

    private void moveResToDir(Resource r, Resource newDir) {
        rl.move(r.path(), newDir.get(r.name()).path());
    }

    private void copyResToDir(Resource r, Resource newDir) throws IOException {
        Resource newR = newDir.get(r.name());
        try (InputStream in = r.in();
                OutputStream out = newR.out()) {
            IOUtils.copy(in, out);
        }
    }
}
