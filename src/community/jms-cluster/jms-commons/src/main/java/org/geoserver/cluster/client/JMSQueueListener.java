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

package org.geoserver.cluster.client;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.geoserver.cluster.JMSApplicationListener;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.JMSManager;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.events.ToggleType;
import org.geotools.util.logging.Logging;
import org.springframework.jms.listener.SessionAwareMessageListener;

/**
 * JMS Client (Consumer)
 *
 * <p>Class which leverages on commons classes to define a Topic consumer handling incoming messages
 * using runtime loaded SPI to instantiate needed handlers.
 *
 * @see {@link JMSManager}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSQueueListener extends JMSApplicationListener
        implements SessionAwareMessageListener<Message> {

    private static final java.util.logging.Logger LOGGER =
            Logging.getLogger(JMSQueueListener.class);

    private final JMSManager jmsManager;

    public JMSQueueListener(final JMSManager jmsManager) {
        super(ToggleType.SLAVE);
        this.jmsManager = jmsManager;
    }

    private AtomicLong consumedEvents = new AtomicLong();

    @Override
    public void onMessage(Message message, Session session) throws JMSException {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Incoming message event for session: " + session.toString());
        }

        // CHECKING LISTENER STATUS
        if (!isEnabled()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Incoming message is swallowed since this component is disabled");
            }
            return;
        }
        // FILTERING INCOMING MESSAGE
        if (!message.propertyExists(JMSConfiguration.INSTANCE_NAME_KEY)) {
            throw new JMSException(
                    "Unable to handle incoming message, property \'"
                            + JMSConfiguration.INSTANCE_NAME_KEY
                            + "\' not set.");
        }

        // FILTERING INCOMING MESSAGE
        if (!message.propertyExists(JMSConfiguration.GROUP_KEY)) {
            throw new JMSException(
                    "Unable to handle incoming message, property \'"
                            + JMSConfiguration.GROUP_KEY
                            + "\' not set.");
        }

        // check if message comes from a master with the same name of this slave
        if (message.getStringProperty(JMSConfiguration.INSTANCE_NAME_KEY)
                .equals(config.getConfiguration(JMSConfiguration.INSTANCE_NAME_KEY))) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Incoming message discarded: source is equal to destination");
            }
            // if so discard the message
            return;
        }

        // check if message comes from a different group
        final String group = message.getStringProperty(JMSConfiguration.GROUP_KEY);
        final String localGroup = config.getConfiguration(JMSConfiguration.GROUP_KEY);
        if (!group.equals(localGroup)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Incoming message discarded: incoming group-->"
                                + group
                                + " is different from the local one-->"
                                + localGroup);
            }
            // if so discard the message
            return;
        }

        // check the property which define the SPI used (to serialize on the
        // server side).
        if (!message.propertyExists(JMSEventHandlerSPI.getKeyName()))
            throw new JMSException(
                    "Unable to handle incoming message, property \'"
                            + JMSEventHandlerSPI.getKeyName()
                            + "\' not set.");

        // END -> FILTERING INCOMING MESSAGE

        // get the name of the SPI used to serialize the message
        final String generatorClass = message.getStringProperty(JMSEventHandlerSPI.getKeyName());
        if (generatorClass == null || generatorClass.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to handle a message without a generator class name");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Incoming message was serialized using an handler generated by: \'"
                            + generatorClass
                            + "\'");
        }

        // USING INCOMING MESSAGE
        if (message instanceof ObjectMessage) {

            final ObjectMessage objMessage = (ObjectMessage) (message);
            final Serializable obj = objMessage.getObject();

            try {
                // lookup the SPI handler, search is performed using the
                // name
                final JMSEventHandler<Serializable, Object> handler =
                        jmsManager.getHandlerByClassName(generatorClass);
                if (handler == null) {
                    throw new JMSException(
                            "Unable to find SPI named \'"
                                    + generatorClass
                                    + "\', be shure to load that SPI into your context.");
                }

                final Enumeration<String> keys = message.getPropertyNames();
                final Properties options = new Properties();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    options.put(key, message.getObjectProperty(key));
                }
                handler.setProperties(options);

                // try to synchronize object locally
                if (!handler.synchronize(handler.deserialize(obj))) {
                    throw new JMSException(
                            "Unable to synchronize message locally.\n SPI: " + generatorClass);
                }

            } catch (Exception e) {
                final JMSException jmsE = new JMSException(e.getLocalizedMessage());
                jmsE.initCause(e);
                throw jmsE;
            } finally {
                this.consumedEvents.incrementAndGet();
            }
        } else throw new JMSException("Unrecognized message type for catalog incoming event");
    }

    // /**
    // * @deprecated unused/untested
    // */
    // private static void getStreamMessage(Message message) throws JMSException
    // {
    // if (message instanceof StreamMessage) {
    // StreamMessage streamMessage = StreamMessage.class.cast(message);
    //
    // File file;
    // // FILTERING incoming message
    // // if (!message.propertyExists(JMSEventType.FILENAME_KEY))
    // // throw new JMSException(
    // // "Unable to handle incoming message, property \'"
    // // + JMSEventType.FILENAME_KEY + "\' not set.");
    //
    // FileOutputStream fos = null;
    // try {
    // file = new File(GeoserverDataDirectory
    // .getGeoserverDataDirectory().getCanonicalPath(), "");
    // // TODO get file name
    // // message.getStringProperty(JMSEventType.FILENAME_KEY));
    // fos = new FileOutputStream(file);
    // final int size = 1024;
    // final byte[] buf = new byte[size];
    // int read = 0;
    // streamMessage.reset();
    // while ((read = streamMessage.readBytes(buf)) != -1) {
    // fos.write(buf, 0, read);
    // fos.flush();
    // }
    // } catch (IOException e) {
    // if (LOGGER.isErrorEnabled()) {
    // LOGGER.error(e.getLocalizedMessage(), e);
    // }
    // throw new JMSException(e.getLocalizedMessage());
    // } catch (JMSException e) {
    // if (LOGGER.isErrorEnabled()) {
    // LOGGER.error(e.getLocalizedMessage(), e);
    // }
    // throw new JMSException(e.getLocalizedMessage());
    // } finally {
    // IOUtils.closeQuietly(fos);
    // }
    //
    // } else
    // throw new JMSException(
    // "Unrecognized message type for catalog incoming event");
    // }

    public long getConsumedEvents() {
        return consumedEvents.get();
    }

    public void resetconsumedevents() {
        consumedEvents.set(0);
    }
}
