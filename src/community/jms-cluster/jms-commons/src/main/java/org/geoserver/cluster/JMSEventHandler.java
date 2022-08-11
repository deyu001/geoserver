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

package org.geoserver.cluster;

import com.thoughtworks.xstream.XStream;
import java.io.Serializable;
import java.util.Properties;
import org.geotools.util.logging.Logging;

/**
 * An handler is an extension class for the JMS platform which define a set of basic operations:
 *
 * <ul>
 *   <li><b>serialize:</b> {@link JMSEventHandler#serialize(Object)}
 *   <li><b>deserialize:</b> {@link JMSEventHandler#deserialize(Serializable)}
 *   <li><b>synchronize:</b> {@link JMSEventHandler#synchronize(Object)}
 * </ul>
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @param <S> type implementing Serializable
 * @param <O> the type of the object this handler is able to handle
 */
public abstract class JMSEventHandler<S extends Serializable, O> {
    private static final long serialVersionUID = 8208466391619901813L;

    protected static final java.util.logging.Logger LOGGER =
            Logging.getLogger(JMSEventHandler.class);

    private final Class<JMSEventHandlerSPI<S, O>> generatorClass;

    private Properties properties;

    protected final XStream xstream;
    /**
     * @param xstream an already initialized xstream
     * @param clazz the SPI class which generate this kind of handler
     */
    public JMSEventHandler(final XStream xstream, Class<JMSEventHandlerSPI<S, O>> clazz) {
        this.generatorClass = clazz;
        this.xstream = xstream;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    /** @return the generatorClass */
    public final Class<JMSEventHandlerSPI<S, O>> getGeneratorClass() {
        return generatorClass;
    }

    /**
     * Its scope is to serialize from an object of type <O> to instance of a Serializable object.
     * <br>
     * That instance will be used by the {@link JMSPublisher} to send the object over a JMS topic.
     * <br>
     *
     * <p>This method is used exclusively on the Server side.
     *
     * @param o the object of type <O> to serialize
     * @return a serializable object
     */
    public abstract S serialize(O o) throws Exception;

    /**
     * Its scope is to create a new instance of type <O> de-serializing the object of type <S>.<br>
     * That instance will be used by the {@link JMSSynchronizer} to obtain (from the JMS topic) an
     * instance to pass to the synchronize method ( {@link #synchronize(Object)}).<br>
     *
     * <p>This method is used exclusively on the Client side
     *
     * @param o the object of type <O> to serialize
     * @return a serializable object
     */
    public abstract O deserialize(S o) throws Exception;

    /**
     * Its scope is to do something with the deserialized {@link #deserialize(Serializable)} object.
     *
     * <p>This method is used exclusively on the Client side
     *
     * @param deserialized the deserialized object
     * @return a boolean true if the operation ends successfully false otherwise
     * @throws Exception if something goes wrong
     */
    public abstract boolean synchronize(O deserialized) throws Exception;
}
