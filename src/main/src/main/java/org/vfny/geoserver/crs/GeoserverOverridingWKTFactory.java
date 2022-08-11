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

package org.vfny.geoserver.crs;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.geotools.util.URLs;
import org.geotools.util.factory.Hints;

/**
 * Same as the {@link GeoserverCustomWKTFactory}, but this one reads a different file and actually
 * overrides official EPSG code interpretations
 */
public class GeoserverOverridingWKTFactory extends FactoryUsingWKT {
    public static final String SYSTEM_DEFAULT_USER_PROJ_FILE = "user.projections.override.file";

    public GeoserverOverridingWKTFactory() {
        super(null, MAXIMUM_PRIORITY);
    }

    public GeoserverOverridingWKTFactory(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY);
    }

    /**
     * Returns the URL to the property file that contains CRS definitions. The default
     * implementation returns the URL to the {@value #FILENAME} file.
     *
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        String cust_proj_file = System.getProperty(SYSTEM_DEFAULT_USER_PROJ_FILE);
        if (cust_proj_file == null) {
            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            if (loader != null) { // Not available for SystemTestData
                Resource custom_proj = loader.get("user_projections/epsg_overrides.properties");
                if (custom_proj.getType() == Type.RESOURCE) {
                    cust_proj_file = custom_proj.file().getAbsolutePath();
                }
            }
        }
        // Attempt to load user-defined projections
        if (cust_proj_file != null) {
            File proj_file = new File(cust_proj_file);

            if (proj_file.exists()) {
                URL url = URLs.fileToUrl(proj_file);
                if (url != null) {
                    return url;
                } else {
                    LOGGER.log(
                            Level.SEVERE, "Had troubles converting " + cust_proj_file + " to URL");
                }
            }
        }

        // Use the built-in property definitions
        cust_proj_file = "override_epsg.properties";

        return GeoserverOverridingWKTFactory.class.getResource(cust_proj_file);
    }
}
