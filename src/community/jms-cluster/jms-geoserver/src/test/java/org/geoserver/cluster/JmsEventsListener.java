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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.geoserver.cluster.events.ToggleType;
import org.springframework.jms.listener.SessionAwareMessageListener;

/** A simple JMS events listener for our tests. */
public final class JmsEventsListener extends JMSApplicationListener
        implements SessionAwareMessageListener<Message> {

    public enum Status {
        NO_STATUS,
        SELECT_CONTINUE,
        REJECT_CONTINUE,
        SELECT_STOP,
        REJECT_STOP
    }

    private static final List<Message> messages = new ArrayList<>();

    public JmsEventsListener() {
        super(ToggleType.SLAVE);
    }

    @Override
    public void onMessage(Message message, Session session) throws JMSException {
        synchronized (messages) {
            // we just need to store the received message
            messages.add(message);
        }
    }

    public static void clear() {
        synchronized (messages) {
            // clear the processing pending messages
            messages.clear();
        }
    }

    /**
     * Blocking helper method that allows us to wait for certain messages in a certain time. The
     * stop method will be used to check if we have all the messages we need. Only messages that
     * match one of the provided handlers keys will be selected.
     */
    public static List<Message> getMessagesByHandlerKey(
            int timeoutMs, Function<List<Message>, Boolean> stop, String... keys) {
        List<String> keysList = Arrays.asList(keys);
        return JmsEventsListener.getMessages(
                timeoutMs,
                stop,
                (message) -> {
                    try {
                        String handlerKey =
                                message.getStringProperty(JMSEventHandlerSPI.getKeyName());
                        if (keysList.contains(handlerKey)) {
                            // we want this message
                            return Status.SELECT_CONTINUE;
                        }
                    } catch (Exception exception) {
                        // we got an exception let's just ignore this message
                    }
                    // not the message we want
                    return Status.REJECT_CONTINUE;
                });
    }

    /**
     * Blocking helper method that allows us to wait for certain messages in a certain time. The
     * stop method will be used to check if we have all the messages we need. The selector method is
     * used to select only certain messages.
     */
    public static List<Message> getMessages(
            int timeoutMs,
            Function<List<Message>, Boolean> stop,
            Function<Message, Status> selector) {
        List<Message> selected = new ArrayList<>();
        Status status = Status.NO_STATUS;
        int max = (int) Math.ceil(timeoutMs / 10.0);
        int i = 0;
        while (i < max
                && status != Status.SELECT_STOP
                && status != Status.REJECT_STOP
                && !stop.apply(selected)) {
            try {
                // let's wait ten milliseconds
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                // restore the interrupted status and return the current messages we have
                Thread.currentThread().interrupt();
                return selected;
            }
            i++;
            synchronized (messages) {
                for (Message message : messages) {
                    status = selector.apply(message);
                    if (status == Status.SELECT_CONTINUE || status == Status.SELECT_STOP) {
                        // we want this message
                        selected.add(message);
                    }
                    if (status == Status.SELECT_STOP || status == Status.REJECT_STOP) {
                        // we are done
                        break;
                    }
                }
                // clear all processed messages
                messages.clear();
            }
        }
        return selected;
    }

    /** Searches the events that match a certain handler and apply the handler to those elements. */
    public static <T> List<T> getMessagesForHandler(
            List<Message> messages, String handlerName, JMSEventHandler<String, T> handler) {
        List<T> found = new ArrayList<>();
        for (Message message : messages) {
            try {
                String handlerKey = message.getStringProperty(JMSEventHandlerSPI.getKeyName());
                if (handlerKey.equals(handlerName) && message instanceof ObjectMessage) {
                    // we found a message that match's the desired handler
                    String object = ((ObjectMessage) message).getObject().toString();
                    found.add(handler.deserialize(object));
                }
            } catch (Exception exception) {
                // we got an exception let's just ignore this message
            }
        }
        return found;
    }
}
