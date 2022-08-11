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

package org.geoserver.web.data.layer;

import java.io.Serializable;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

/**
 * The bean to be rendered in the new layer page
 *
 * @author Andrea Aime - OpenGeo
 */
public class Resource implements Comparable<Resource>, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 8744964557875392120L;
    /** The resource name */
    String name;

    String uri;

    /** If this resource has already been published, or not */
    boolean published;

    /** Specified if this resource is from a multi-coverage reader */
    boolean multiCoverageReader;

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Resource(Name name) {
        super();
        this.name = name.getLocalPart();
        this.uri = name.getNamespaceURI();
    }

    public String getLocalName() {
        return name;
    }

    public Name getName() {
        return new NameImpl(uri, name);
    }

    public boolean isPublished() {
        return published;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Resource other = (Resource) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        return true;
    }

    public int compareTo(Resource o) {
        // unpublished resources first
        if (published && !o.published) return -1;
        else if (!published && o.published) return 1;
        // the compare by local name, as it's unlikely the users will see the
        // namespace URI (and the prefix is not available in Name)
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name + "(" + published + ")";
    }

    public boolean isMultiCoverageReader() {
        return multiCoverageReader;
    }

    public void setMultiCoverageReader(boolean multiCoverageReader) {
        this.multiCoverageReader = multiCoverageReader;
    }
}
