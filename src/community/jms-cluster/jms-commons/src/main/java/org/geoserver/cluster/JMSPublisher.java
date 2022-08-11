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

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.Topic;
import org.geoserver.cluster.message.JMSObjectMessageCreator;
import org.geotools.util.logging.Logging;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * JMS MASTER (Producer)
 *
 * <p>Class which define a general purpose producer which sends valid ObjectMessages using a
 * JMSTemplate. Valid means that we are appending to the message some conventional (to this JMS
 * plug-in) properties which can be used to synchronize consumer and producers.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSPublisher {

    static final Logger LOGGER = Logging.getLogger(JMSPublisher.class);

    private final JMSManager jmsManager;

    /** Constructor */
    public JMSPublisher(JMSManager jmsManager) {
        this.jmsManager = jmsManager;
    }

    /**
     * Used to publish the event on the queue.
     *
     * @param <S> a serializable object
     * @param <O> the object to serialize using a JMSEventHandler
     * @param jmsTemplate the template to use to publish on the topic <br>
     *     (default destination should be already set)
     * @param props the JMSProperties used by this instance of GeoServer
     * @param object the object (or event) to serialize and send on the JMS topic
     */
    public <S extends Serializable, O> void publish(
            final Topic destination,
            final JmsTemplate jmsTemplate,
            final Properties props,
            final O object)
            throws JMSException {
        try {

            final JMSEventHandler<S, O> handler = jmsManager.getHandler(object);

            // set the used SPI
            props.put(JMSEventHandlerSPI.getKeyName(), handler.getGeneratorClass().getSimpleName());

            // TODO make this configurable
            final MessageCreator creator =
                    new JMSObjectMessageCreator(handler.serialize(object), props);

            jmsTemplate.send(destination, creator);

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            final JMSException ex = new JMSException(e.getLocalizedMessage());
            ex.initCause(e);
            throw ex;
        }
    }
}
