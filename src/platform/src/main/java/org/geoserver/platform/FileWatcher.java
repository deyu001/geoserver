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

package org.geoserver.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Watches a files last modified date to determine when a file has been changed.
 *
 * <p>Client code using this class should call {@link #isModified()} to determine if the file has
 * changed since the last check, and {@link #read()} to read the contents of the file and update the
 * last check timestamp.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class FileWatcher<T> {
    protected Resource resource;
    private long lastModified = Long.MIN_VALUE;
    private long lastCheck;
    private boolean stale;

    public FileWatcher(Resource resource) {
        this.resource = resource;
    }

    public FileWatcher(File file) {
        this.resource = Files.asResource(file);
    }

    public File getFile() {
        return resource.file();
    }

    public Resource getResource() {
        return resource;
    }

    /**
     * Reads the file updating the last check timestamp.
     *
     * <p>Subclasses can override {@link #parseFileContents(InputStream)} to do something when the
     * file is read.
     *
     * @return parsed file contents
     */
    public T read() throws IOException {
        T result = null;

        if (resource.getType() == Type.RESOURCE) {

            try (InputStream is = resource.in()) {
                result = parseFileContents(is);

                lastModified = resource.lastmodified();
                lastCheck = System.currentTimeMillis();
                stale = false;
            }
        }

        return result;
    }

    /**
     * Parses the contents of the file being watched.
     *
     * <p>Subclasses should override.
     */
    protected T parseFileContents(InputStream in) throws IOException {
        return null;
    }

    /** Determines if the underlying file has been modified since the last check. */
    public boolean isModified() {
        long now = System.currentTimeMillis();
        if ((now - lastCheck) > 1000) {
            lastCheck = now;
            stale =
                    (resource.getType() != Type.UNDEFINED)
                            && (resource.lastmodified() != lastModified);
        }
        return stale;
    }

    /**
     * Method to set the last modified time stamp. Clients synchronized with the actual file content
     * and knowing the last modified time stamp can avoid unnecessary reload operations
     *
     * @param lastModified last modified time
     */
    public void setKnownLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
