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

package org.geoserver.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geotools.filter.v1_0.OGC;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.opengis.filter.Filter;

/**
 * Base class for all AccessLimits declared by a {@link ResourceAccessManager}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DataAccessLimits extends AccessLimits {
    private static final OGCConfiguration CONFIGURATION = new OGCConfiguration();

    private static final long serialVersionUID = 2594922992934373705L;

    /**
     * Used for vector reading, for raster if there is a read param taking an OGC filter, and in WMS
     * if the remote server supports CQL filters and on feature info requests. For workspaces it
     * will be just INCLUDE or EXCLUDE to allow or deny access to the workspace
     */
    transient Filter readFilter;

    /**
     * Builds a generic DataAccessLimits
     *
     * @param readFilter This filter will be merged with the request read filters to limit the
     *     features/tiles that can be actually read
     */
    public DataAccessLimits(CatalogMode mode, Filter readFilter) {
        super(mode);
        this.readFilter = readFilter;
    }

    /**
     * This filter will be merged with the request read filters to limit the features/tiles that can
     * be actually read
     */
    public Filter getReadFilter() {
        return readFilter;
    }

    /** The catalog mode for this layer */
    public CatalogMode getMode() {
        return mode;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        readFilter = readFilter(in);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        writeFilter(readFilter, out);
    }

    /**
     * Writes the non Serializable Filter object ot the ObjectOutputStream via a OGC Filter XML
     * encoding conversion
     */
    protected void writeFilter(Filter filter, ObjectOutputStream out) throws IOException {
        if (filter != null) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                Encoder encoder = new Encoder(CONFIGURATION);
                encoder.encode(filter, OGC.Filter, bos);
                out.writeObject(bos.toByteArray());
            }
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Reads from the object input stream a string representing a filter in OGC XML encoding and
     * parses it back to a Filter object
     */
    protected Filter readFilter(ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte[] serializedReadFilter = (byte[]) in.readObject();
        if (serializedReadFilter != null) {
            try {
                Parser p = new Parser(CONFIGURATION);
                return (Filter) p.parse(new ByteArrayInputStream(serializedReadFilter));
            } catch (Exception e) {
                throw (IOException) new IOException("Failed to parse filter").initCause(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((readFilter == null) ? 0 : readFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        DataAccessLimits other = (DataAccessLimits) obj;
        if (readFilter == null) {
            if (other.readFilter != null) return false;
        } else if (!readFilter.equals(other.readFilter)) return false;
        return true;
    }
}
