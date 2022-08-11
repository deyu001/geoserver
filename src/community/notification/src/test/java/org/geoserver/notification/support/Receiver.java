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


package org.geoserver.notification.support;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.notification.common.CustomSaslConfig;
import org.geotools.util.logging.Logging;

public class Receiver {

    protected static Logger LOGGER = Logging.getLogger(Receiver.class);

    private static String BROKER_URI = "amqp://localhost:4432";

    private static final String QUEUE_NAME = "jms/queue";

    private ReceiverService service;

    private Connection connection;

    private Channel channel;

    public Receiver() {}

    public Receiver(String username, String password) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            BROKER_URI = "amqp://" + username + ":" + password + "@localhost:4432";
        }
    }

    public void receive(ReceiverService service) throws Exception {
        // let's setup evrything and start listening
        this.service = service;
        ConnectionFactory factory = createConnectionFactory();
        factory.setSaslConfig(new CustomSaslConfig());
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare("testExchange", "fanout");
        channel.queueDeclare(QUEUE_NAME, false, true, false, null);
        channel.queueBind(QUEUE_NAME, "testExchange", "testRouting");
        channel.basicConsume(QUEUE_NAME, true, newConsumer(channel));
    }

    protected ConnectionFactory createConnectionFactory() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(false);
        factory.setUri(BROKER_URI);
        return factory;
    }

    private DefaultConsumer newConsumer(Channel channel) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body)
                    throws IOException {
                service.manage(body);
            }
        };
    }

    public void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
    }
}
