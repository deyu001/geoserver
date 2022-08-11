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

package org.geoserver.taskmanager.external;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.util.string.Strings;
import org.geoserver.taskmanager.util.Secured;

/**
 * Persist and read files. All actions on this service are relative to the configured rootFolder.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 * @author Niels Charlier
 */
public interface FileService extends Serializable, Secured {

    String PLACEHOLDER_VERSION = "###";

    /**
     * User-friendly description of this file service.
     *
     * @return description
     */
    String getDescription();

    /**
     * List existing all nested folders in this file service. e.g. /foo/ /foo/bar/ /other/
     *
     * @return list of existing sub folders
     */
    List<String> listSubfolders();

    /**
     * Create a file in the file service
     *
     * @param filePath the path of the file, relative to this service
     * @param content the content of the file
     * @return a location string that can be used to configure a Geoserver store
     */
    void create(String filePath, InputStream content, boolean doPrepare) throws IOException;

    /**
     * Create a file in the file service
     *
     * @param filePath the path of the file, relative to this service
     * @param content the content of the file
     * @return a location string that can be used to configure a Geoserver store
     */
    default void create(String filePath, InputStream content) throws IOException {
        create(filePath, content, false);
    }

    /**
     * Check if this file exists.
     *
     * @param filePath the path of the file, relative to this service
     * @return whether the file exists
     */
    boolean checkFileExists(String filePath) throws IOException;

    /**
     * Get current and next version of a versioned file
     *
     * @param filePath the original file path
     * @return the versioned file info
     */
    FileReference getVersioned(String filePath);

    /**
     * Delete this file.
     *
     * @param filePath the path of the file, relative to this service
     * @return whether anything was actually deleted.
     */
    boolean delete(String filePath) throws IOException;

    /**
     * Read this file.
     *
     * @param filePath the path of the file, relative to this service
     * @return inputstream with data
     */
    InputStream read(String filePath) throws IOException;

    /**
     * Returns the rootFolder. All actions on the service are relative to the rootFolder.
     *
     * @return the rootFolder.
     */
    String getRootFolder();

    /**
     * Returns the URI for the path
     *
     * @param filePath the file path
     * @return the URI
     */
    URI getURI(String filePath);

    static String versioned(String filePath) {
        if (filePath.contains(PLACEHOLDER_VERSION)) {
            return filePath;
        }
        String ext = FilenameUtils.getExtension(filePath);
        if (Strings.isEmpty(ext)) {
            return filePath + "." + PLACEHOLDER_VERSION;
        } else {
            return FilenameUtils.removeExtension(filePath) + "." + PLACEHOLDER_VERSION + "." + ext;
        }
    }
}
