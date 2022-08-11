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


package org.geoserver.notification.common.sender;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.notification.common.CustomSaslConfig;
import org.geoserver.notification.common.NotificationXStreamDefaultInitializer;
import org.geotools.util.logging.Logging;

/**
 * Initialize the AMQP broker client connection and delegate to specific RabbitMQ client
 * implementation the dispatch of payload.
 *
 * <p>The broker connection parameters are populated by {@link XStream} deserialization, using the
 * configuration provided by {@link NotificationXStreamDefaultInitializer}
 *
 * <p>Anonymous connection is possible using {@link CustomSaslConfig}
 *
 * @param host the host to which the underlying TCP connection is made
 * @param port the port number to which the underlying TCP connection is made
 * @param virtualHost a path which acts as a namespace (optional)
 * @param username if present is used for SASL exchange (optional)
 * @param password if present is used for SASL exchange (optional)
 * @author Xandros
 * @see FanoutRabbitMQSender
 */
public abstract class RabbitMQSender implements NotificationSender, Serializable {

    private static Logger LOGGER = Logging.getLogger(RabbitMQSender.class);

    private static final long serialVersionUID = 1370640635300148935L;

    protected String host;

    protected String virtualHost;

    protected int port;

    protected String username;

    protected String password;

    protected String uri;

    protected Connection conn;

    protected Channel channel;

    public void initialize() throws Exception {
        if (uri == null) {
            if (this.username != null
                    && !this.username.isEmpty()
                    && this.password != null
                    && !this.password.isEmpty()) {
                this.uri =
                        "amqp://"
                                + this.username
                                + ":"
                                + this.password
                                + "@"
                                + this.host
                                + ":"
                                + this.port;
            } else {
                this.uri = "amqp://" + this.host + ":" + this.port;
            }
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(this.uri);
            String vHost =
                    (this.virtualHost != null && !this.virtualHost.isEmpty()
                            ? this.virtualHost
                            : "/");
            factory.setVirtualHost(vHost);
            factory.setSaslConfig(new CustomSaslConfig());
            conn = factory.newConnection();
            channel = conn.createChannel();
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING, "Error while trying to initialize RabbitMQ Sender Connecton", e);
        }
    }

    public void close() throws Exception {
        if (this.channel != null) {
            this.channel.close();
        }

        if (this.conn != null) {
            this.conn.close();
        }
    }

    // Prepare Connection Channel
    public void send(byte[] payload) throws Exception {
        try {
            this.initialize();
            this.sendMessage(payload);
        } catch (Exception e) {
            LOGGER.log(Level.FINEST, e.getMessage(), e);
        } finally {
            this.close();
        }
    }

    // Send message to the Queue by using Channel
    public abstract void sendMessage(byte[] payload) throws IOException;
}
