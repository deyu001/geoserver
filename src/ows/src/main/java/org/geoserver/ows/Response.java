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

package org.geoserver.ows;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Response to an operation, which serializes the result of the operation to an output stream.
 *
 * <p>A response must specify the following information:
 *
 * <ul>
 *   <li>The type of object it is capable of serializing, the class is bound to. See {@link
 *       #getBinding()}.
 *   <li>The mime-type of the resulting response. See {@link #getMimeType(Object, Operation)}.
 * </ul>
 *
 * <p>Optionally, a response may declare a well-known name for it. This well known name corresponds
 * to the "outputFormat" parameter which is supported on many types of OWS request.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class Response {

    public static final String DISPOSITION_INLINE = "inline";
    public static final String DISPOSITION_ATTACH = "attachment";

    /** Class of object to serialize */
    final Class<?> binding;

    /** The well known "outputFormat" of the response */
    final Set<String> outputFormats;

    /**
     * Constructor which specified the class this response is bound to.
     *
     * @param binding The class of object the response serializes.
     */
    public Response(Class<?> binding) {
        this(binding, (Set<String>) null);
    }

    /**
     * Constructor which specified the class this response is bound to, and a common name for the
     * type of response.
     *
     * @param binding The class of object the response serializes
     * @param outputFormat A common name for the response.
     */
    public Response(Class<?> binding, String outputFormat) {
        this(binding, outputFormat == null ? null : Collections.singleton(outputFormat));
    }

    /**
     * Constructor which specified the class this response is bound to, and a set of common names
     * for the type of response.
     *
     * @param binding The class of object the response serializes
     * @param outputFormats A set of common names for the response.
     */
    public Response(Class<?> binding, Set<String> outputFormats) {
        if (binding == null) {
            throw new NullPointerException("binding may not be null");
        }

        if (outputFormats == null) {
            outputFormats = Collections.emptySet();
        }

        this.binding = binding;
        this.outputFormats = outputFormats;
    }

    /** @return The type of object the response can handle. */
    public final Class<?> getBinding() {
        return binding;
    }

    /** @return Set of common or well-known name for the response, may be empty. */
    public final Set<String> getOutputFormats() {
        return outputFormats;
    }

    /**
     * Determines if the response can handle the operation being performed.
     *
     * <p>This method is called before {@link #write(Object, OutputStream, Operation)}.
     *
     * <p>Subclasses should override this method to perform additional checks against the operation
     * being performed. Example might be checking the version of the service.
     *
     * @param operation The operation being performed.
     * @return <code>true</code> if the response can handle the operation, otherwise <code>false
     *     </code>
     */
    public boolean canHandle(Operation operation) {
        return true;
    }

    /**
     * Returns the mime type to be uses when writing the response.
     *
     * @param value The value to serialize
     * @param operation The operation being performed.
     * @return The mime type of the response, must not be <code>null</code>
     */
    public abstract String getMimeType(Object value, Operation operation) throws ServiceException;

    /**
     * Returns a 2xn array of Strings, each of which is an HTTP header pair to be set on the HTTP
     * Response. Can return null if there are no headers to be set on the response.
     *
     * @param value The value to serialize
     * @param operation The operation being performed.
     * @return 2xn string array containing string-pairs of HTTP headers/values
     */
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        // default implementation returns null = no headers to set
        return null;
    }

    /**
     * Serializes <code>value</code> to <code>output</code>.
     *
     * <p>The <code>operation</code> bean is provided for context.
     *
     * @param value The value to serialize.
     * @param output The output stream.
     * @param operation The operation which resulted in <code>value</code>
     * @throws IOException Any I/O errors that occur
     * @throws ServiceException Any service errors that occur
     */
    public abstract void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException;

    /**
     * Get the preferred Content-Disposition header for this response. The default is inline.
     * Subclasses can prefer attachment.
     *
     * @param value The value that will be serialized
     * @param operation The operation which resulted in <code>value</code>
     * @return inline or attachment
     */
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_INLINE;
    }

    /**
     * Get a name for a Content-Disposition attachment filename. The mimetype should match the file
     * extension. The default implementation will use the mimetype and operation id to attempt to
     * build a name.
     *
     * @param value The value that will be serialized
     * @param operation The operation being performed
     * @return null or a filename such as result.txt or map.tiff
     */
    public String getAttachmentFileName(Object value, Operation operation) {
        String mimeType = getMimeType(value, operation);
        String opName = operation.getId();
        String name = null;
        if (mimeType != null) {
            name = "geoserver";
            if (opName != null) {
                name = name + "-" + opName;
            }
            String[] typeParts = mimeType.split(";");
            name = name + "." + typeParts[0].split("/")[0];
        }
        return name;
    }

    /**
     * Returns the charset for this response, the Dispatcher will set it in the ServletResponse. The
     * default implementation returns <code>null</code>, in this case no encoding should be set.
     * Subclasses returning text documents (CSV,HTML,JSON) should override taking into account
     * SettingsInfo.getCharset() as well as the specific encoding requirements of the returned
     * format.
     */
    public String getCharset(Operation operation) {
        return null;
    }
}
