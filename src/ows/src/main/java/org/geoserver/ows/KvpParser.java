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

import java.util.logging.Logger;
import org.geotools.util.Version;

/**
 * Parses a key-value pair into a key-object pair.
 *
 * <p>This class is intended to be subclassed. Subclasses need declare the key in which they parse,
 * and the type of object they parse into.
 *
 * <p>Instances need to be declared in a spring context like the following:
 *
 * <pre>
 *         <code>
 *  &lt;bean id="myKvpParser" class="org.xzy.MyKvpParser"/&gt;
 *         </code>
 * </pre>
 *
 * Where <code>com.xzy.MyKvpParser</code> could be something like:
 *
 * <pre>
 *         <code>
 *  public class MyKvpParser extends KvpParser {
 *
 *     public MyKvpParser() {
 *        super( "MyKvp", MyObject.class )l
 *     }
 *
 *     public Object parse( String value ) {
 *        return new MyObject( value );
 *     }
 *  }
 *         </code>
 * </pre>
 *
 * <p><b>Operation Binding</b>
 *
 * <p>In the normal case, a kvp parser is engaged when a request specifies a name which matches the
 * name declared by the kvp parser. It is also possible to attach a kvp parser so that it only
 * engages on a particular operation. This is done by declaring the one or more of the following:
 *
 * <ul>
 *   <li>service
 *   <li>version
 *   <li>request
 * </ul>
 *
 * <p>When a kvp parser declares one or more of these properties, it will only be engaged if an
 * incoming request specicfies matching values of the properties.
 *
 * <p>The following bean declaration would create the above kvp parser so that it only engages when
 * the service is "MyService", and the request is "MyRequest".
 *
 * <pre>
 *         <code>
 *  &lt;bean id="myKvpParser" class="org.xzy.MyKvpParser"&gt;
 *    &lt;property name="service"&gt;MyService&lt;/property&gt;
 *    &lt;property name="request"&gt;MyRequest&lt;/property&gt;
 *  &lt;bean&gt;
 *         </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class KvpParser {
    /** logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.ows");

    /** The key. */
    String key;

    /** The class of parsed objects. */
    Class binding;

    /** The service to bind to */
    String service;

    /** The version of the service to bind to */
    Version version;

    /** The request to bind to */
    String request;

    public KvpParser(String key, Class binding) {
        this.key = key;
        this.binding = binding;
    }

    /** @return The name of the key the parser binds to. */
    public String getKey() {
        return key;
    }

    /** @return The type of parsed objects. */
    protected Class<?> getBinding() {
        return binding;
    }

    /** @return The service to bind to, may be <code>null</code>. */
    public final String getService() {
        return service;
    }

    /** Sets the service to bind to. */
    public final void setService(String service) {
        this.service = service;
    }

    /** @return The version to bind to, or <code>null</code>. */
    public final Version getVersion() {
        return version;
    }

    /** Sets the version to bind to. */
    public final void setVersion(Version version) {
        this.version = version;
    }

    /** Sets the request to bind to. */
    public final void setRequest(String request) {
        this.request = request;
    }

    /** @return The request to bind to, or <code>null</code>. */
    public String getRequest() {
        return request;
    }

    /**
     * Parses the string representation into the object representation.
     *
     * @param value The string value.
     * @return The parsed object, or null if it could not be parsed.
     * @throws Exception In the event of an unsuccesful parse.
     */
    public abstract Object parse(String value) throws Exception;
}
