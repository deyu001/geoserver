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

package org.geoserver.config.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;

/**
 * Utility class for XStream related utilities
 *
 * @author Andrea Aime - TOPP
 */
public final class XStreamUtils {
    /**
     * Performs serialization with an {@link XStreamPersister} in a safe manner in which a temp file
     * is used for the serialization so that the true destination file is not partially written in
     * the case of an error.
     *
     * @param f The file to write to, only modified if the temp file serialization was error free.
     * @param obj The object to serialize.
     * @param xp The persister.
     */
    public static void xStreamPersist(File f, Object obj, XStreamPersister xp) throws IOException {
        // first save to a temp file
        final File temp = File.createTempFile(f.getName(), null, f.getParentFile());

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp))) {
            xp.save(obj, out);
            out.flush();
        }

        // no errors, overwrite the original file
        try {
            IOUtils.rename(temp, f);
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    /**
     * Performs serialization with an {@link XStreamPersister} in a safe manner in which a temp file
     * is used for the serialization so that the true destination file is not partially written in
     * the case of an error.
     *
     * @param r The resource to write to, only modified if the temp file serialization was error
     *     free.
     * @param obj The object to serialize.
     * @param xp The persister.
     */
    public static void xStreamPersist(Resource r, Object obj, XStreamPersister xp)
            throws IOException {

        try (OutputStream out = r.out()) {
            xp.save(obj, out);
            out.flush();
        }
    }
}
