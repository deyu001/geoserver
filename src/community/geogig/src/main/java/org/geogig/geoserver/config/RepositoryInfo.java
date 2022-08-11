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

package org.geogig.geoserver.config;

import static com.google.common.base.Objects.equal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.Serializable;
import java.net.URI;
import org.locationtech.geogig.repository.RepositoryResolver;

public class RepositoryInfo implements Serializable, Cloneable {

    private static final long serialVersionUID = -5946705936987075713L;

    private String id;

    /**
     * @deprecated field to support deserialization of old format when it only allowed file:
     *     repositories
     */
    private String parentDirectory;

    /**
     * @deprecated field to support deserialization of old format when it only allowed file:
     *     repositories
     */
    private String name;

    private java.net.URI location;

    private String maskedLocation;

    /**
     * Stores the "nice" name for a repo. This is the name that is shown in the Repository list, as
     * well as what is stored in the GeoGIG repository config. It is transient, as we don't want to
     * serialize this. It's just a place holder for the name until it can be persisted into the repo
     * config.
     */
    private transient String repoName;

    private transient long lastModified;

    public RepositoryInfo() {
        this(null);
    }

    RepositoryInfo(String id) {
        this.id = id;
    }

    public @Override RepositoryInfo clone() {
        try {
            return (RepositoryInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw Throwables.propagate(e);
        }
    }

    private Object readResolve() {
        if (parentDirectory != null && name != null) {
            File file = new File(new File(parentDirectory), name).getAbsoluteFile();
            this.location = file.toURI();
            this.parentDirectory = null;
            this.name = null;
        }
        return this;
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    public URI getLocation() {
        readResolve();
        return this.location;
    }

    public String getMaskedLocation() {
        if (maskedLocation == null) {
            // ensure the location is set
            getLocation();
            // get the masked version of the URI
            this.maskedLocation = GeoServerGeoGigRepositoryResolver.getURI(getRepoName());
        }
        return this.maskedLocation;
    }

    public void setLocation(URI location) {
        this.location = location;
    }

    @VisibleForTesting
    void setParentDirectory(String parent) {
        this.parentDirectory = parent;
    }

    @VisibleForTesting
    void setName(String name) {
        this.name = name;
    }

    public void setRepoName(String name) {
        this.repoName = name;
    }

    public String getRepoName() {
        if (this.location != null) {
            if (this.repoName == null) {
                // lookup the resolver
                if (RepositoryResolver.resolverAvailableForURIScheme(this.location.getScheme())) {
                    RepositoryResolver resolver = RepositoryResolver.lookup(this.location);
                    this.repoName = resolver.getName(this.location);
                }
            }
        }
        return this.repoName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RepositoryInfo)) {
            return false;
        }
        RepositoryInfo r = (RepositoryInfo) o;
        return equal(getId(), r.getId()) && equal(getLocation(), r.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getLocation());
    }

    @Override
    public String toString() {
        return new StringBuilder("[id:")
                .append(getId())
                .append(", URI:")
                .append(getLocation())
                .append("]")
                .toString();
    }

    long getLastModified() {
        return lastModified;
    }

    void setLastModified(long timestamp) {
        this.lastModified = timestamp;
    }
}
