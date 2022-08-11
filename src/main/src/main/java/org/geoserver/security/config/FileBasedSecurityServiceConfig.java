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

package org.geoserver.security.config;

/**
 * Security service configuration object that is persisted in a file.
 *
 * @author christian
 */
public class FileBasedSecurityServiceConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;
    private String fileName;
    private long checkInterval;

    public FileBasedSecurityServiceConfig() {}

    public FileBasedSecurityServiceConfig(FileBasedSecurityServiceConfig other) {
        super(other);
        fileName = other.getFileName();
        checkInterval = other.getCheckInterval();
    }

    /** @return The name of file to persist configuration in. */
    public String getFileName() {
        return fileName;
    }

    /** Sets the name of file to persist configuration in. */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * The time interval, in milliseconds, in which to check the underlying file for changes.
     *
     * @return the check interval in ms.
     * @see #setCheckInterval(long)
     */
    public long getCheckInterval() {
        return checkInterval;
    }

    /**
     * Sets the time interval, in milliseconds, in which to check the underlying file for changes.
     *
     * <p>This property is typically used in environments (such as a cluster) in which the
     * underlying file may have been modified out of process.
     *
     * <p>A value of > 0 causes {@link FileWatcher} object to be created. A value of <= 0 disables
     * any checking of the underlying file.
     *
     * <p>Hint: the granularity of {@link File} last access time is often a second, values < 1000
     * may not have the desired effect.
     *
     * @param checkInterval The time interval in ms.
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }
}
