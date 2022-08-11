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

import java.io.Reader;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.geotools.util.Version;

/**
 * Creates a request bean from xml.
 *
 * <p>A request bean is an object which captures the parameters of an operation being requested to a
 * service.
 *
 * <p>An xml request reader must declare the root element of xml documents that it is capable of
 * reading. This is accomplished with {@link #getElement()} and {@link QName#getNamespaceURI()}.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class XmlRequestReader {
    /** the qualified name of the element this reader can read. */
    final QName element;

    /** Appliction specific version number. */
    final Version version;

    /** Service identifier */
    final String serviceId;

    /**
     * Creates the xml reader for the specified element.
     *
     * @param element The qualified name of the element the reader reads.
     */
    public XmlRequestReader(QName element) {
        this(element, null, null);
    }

    /**
     * Creates the xml reader for the specified element.
     *
     * @param namespace The namespace of the element
     * @param local The local name of the element
     */
    public XmlRequestReader(String namespace, String local) {
        this(new QName(namespace, local));
    }

    /**
     * Creates the xml reader for the specified element of a particular version.
     *
     * @param element The qualified name of the element the reader reads.
     * @param version The version of the element in which the reader supports, may be <code>null
     *     </code>.
     */
    public XmlRequestReader(QName element, Version version, String serviceId) {
        this.element = element;
        this.version = version;
        this.serviceId = serviceId;

        if (element == null) {
            throw new NullPointerException("element");
        }
    }

    /** @return The qualified name of the element that this reader reads. */
    public QName getElement() {
        return element;
    }

    /** @return The version of the element that this reader reads. */
    public Version getVersion() {
        return version;
    }

    /**
     * Reads the xml and initializes the request object.
     *
     * <p>The <tt>request</tt> parameter may be <code>null</code>, so in this case the request
     * reader would be responsible for creating the request object, or throwing an exception if this
     * is not supported.
     *
     * <p>In the case of the <tt>request</tt> being non <code>null</code>, the request reader may
     * chose to modify and return <tt>request</tt>, or create a new request object and return it.
     *
     * <p>The <tt>kvp</tt> is used to support mixed style reading of the request object from xml and
     * from a set of key value pairs. This map is often empty.
     */
    public abstract Object read(Object request, Reader reader, Map kvp) throws Exception;

    /**
     * Two XmlReaders considered equal if namespace,element, and version properties are the same.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof XmlRequestReader)) {
            return false;
        }

        XmlRequestReader other = (XmlRequestReader) obj;

        return new EqualsBuilder()
                .append(element, other.element)
                .append(version, other.version)
                .append(serviceId, other.serviceId)
                .isEquals();
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + element + ", " + serviceId + ", " + version + ")";
    }

    /** Implementation of hashcode. */
    public int hashCode() {
        return new HashCodeBuilder().append(element).append(version).append(serviceId).toHashCode();
    }

    public String getServiceId() {
        return serviceId;
    }
}
