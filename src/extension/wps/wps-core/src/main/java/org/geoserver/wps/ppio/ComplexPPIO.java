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

package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Process parameter input / output for arbitrary data on a specific mime type.
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 */
public abstract class ComplexPPIO extends ProcessParameterIO {

    /** mime type of encoded content. */
    protected String mimeType;

    /** Constructor. */
    protected ComplexPPIO(Class externalType, Class internalType, String mimeType) {
        super(externalType, internalType);
        this.mimeType = mimeType;
    }

    /** The mime type of the parameter of the data in encoded form. */
    public final String getMimeType() {
        return mimeType;
    }

    /**
     * Decodes the parameter from an external source or input stream.
     *
     * <p>This method should parse the input stream into its "internal" representation.
     *
     * @param input The input stream.
     * @return An object of type {@link #getType()}.
     */
    public abstract Object decode(InputStream input) throws Exception;

    /**
     * Decodes the parameter from an extenral source that has been pre-parsed.
     *
     * <p>This method should transform the object from the external representation to the internal
     * representation.
     *
     * @param input An object of type {@link #getExternalType()}
     * @return An object of type {@link #getType()}.
     */
    public Object decode(Object input) throws Exception {
        return input;
    }

    /** Encodes the internal object representation of a parameter into an output stream */
    public abstract void encode(Object value, OutputStream os) throws Exception;

    /**
     * Encodes the internal object representation of a parameter into an output stream using
     * specific encoding parameters
     */
    public void encode(Object value, Map<String, Object> encodingParameters, OutputStream os)
            throws Exception {
        encode(value, os);
    };

    /**
     * Provides a suitable extension for the output file. Implement this if the file extension is
     * not depend on the object being encoded
     */
    public String getFileExtension() {
        return ".bin";
    }

    /**
     * Provides a suitable extension for the output file given the object being encoded. The default
     * implementation simply calls {@link #getFileExtension()}
     */
    public String getFileExtension(Object object) {
        return getFileExtension();
    }
}
