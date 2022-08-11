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

package org.geoserver.web.data.store.panel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geotools.util.logging.Logging;

/**
 * Makes sure the file path for files do start with file:// otherwise stuff like /home/user/file.shp
 * won't be recognized as valid. Also, if a path is inside the data directory it will be turned into
 * a relative path
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FileModel implements IModel<String> {
    private static final long serialVersionUID = 3911203737278340528L;

    static final Logger LOGGER = Logging.getLogger(FileModel.class);

    IModel<String> delegate;
    File rootDir;

    public FileModel(IModel<String> delegate) {
        this(delegate, GeoServerExtensions.bean(GeoServerResourceLoader.class).getBaseDirectory());
    }

    public FileModel(IModel<String> delegate, File rootDir) {
        this.delegate = delegate;
        this.rootDir = rootDir;
    }

    private boolean isSubfile(File root, File selection) {
        if (selection == null || "".equals(selection.getPath())) return false;
        if (selection.equals(root)) return true;

        return isSubfile(root, selection.getParentFile());
    }

    @Override
    public String getObject() {
        Object obj = delegate.getObject();
        if (obj instanceof URL) {
            URL url = (URL) obj;
            return url.toExternalForm();
        }
        return (String) obj;
    }

    public void detach() {
        // TODO Auto-generated method stub

    }

    public void setObject(String location) {

        if (location != null) {
            File dataDirectory = canonicalize(rootDir);
            File file = canonicalize(new File(location));
            if (isSubfile(dataDirectory, file)) {
                File curr = file;
                String path = null;
                // paranoid check to avoid infinite loops
                while (curr != null && !curr.equals(dataDirectory)) {
                    if (path == null) {
                        path = curr.getName();
                    } else {
                        path = curr.getName() + "/" + path;
                    }
                    curr = curr.getParentFile();
                }
                location = "file:" + path;
            } else {
                File dataFile = Files.url(rootDir, location);
                if (dataFile == null || dataFile.equals(file)) {
                    // not relative to the data directory, does not need fixing
                    location = "file://" + file.getAbsolutePath();
                }
            }
        }
        delegate.setObject(location);
    }

    /** Turns a file in canonical form if possible */
    File canonicalize(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Could not convert " + file + " into canonical form", e);
            return file;
        }
    }
}
