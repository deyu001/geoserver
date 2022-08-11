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

package org.geoserver.wms;

import java.io.Serializable;
import java.util.Objects;

/**
 * Cache configuration object for WMS remote styles. Allows enabling the cache and setting the size,
 * in terms of entries and single entry size.
 *
 * @author maurobartolomeoli@gmail.com
 */
public class CacheConfiguration implements Cloneable, Serializable {

    private boolean enabled;
    private int maxEntries = 1000;
    private long maxEntrySize = 50 * 1024;

    public CacheConfiguration() {
        super();
    }

    public CacheConfiguration(boolean enabled) {
        super();
        this.enabled = enabled;
    }

    public CacheConfiguration(boolean enabled, int maxEntries, long maxEntrySize) {
        super();
        this.enabled = enabled;
        this.maxEntries = maxEntries;
        this.maxEntrySize = maxEntrySize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Enables / disables caching. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    /** Sets max # of entries in cache. */
    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public long getMaxEntrySize() {
        return maxEntrySize;
    }

    /** Sets max entry size in bytes. */
    public void setMaxEntrySize(long maxEntrySize) {
        this.maxEntrySize = maxEntrySize;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CacheConfiguration) {
            CacheConfiguration other = (CacheConfiguration) obj;
            return other.enabled == enabled
                    && other.maxEntries == maxEntries
                    && other.maxEntrySize == maxEntrySize;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, maxEntries, maxEntrySize);
    }

    public Object clone() {
        return new CacheConfiguration(enabled, maxEntries, maxEntrySize);
    }
}
