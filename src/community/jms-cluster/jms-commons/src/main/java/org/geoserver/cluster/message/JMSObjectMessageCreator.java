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

package org.geoserver.cluster.message;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.springframework.jms.core.MessageCreator;

/**
 * Class implementing a MessageCreator which is used to produce valid ObjectMessages
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @param <S> serializable class
 * @param <O> object to serialize
 */
public class JMSObjectMessageCreator implements MessageCreator {

    private final Serializable serialized;
    private final Properties properties;

    public JMSObjectMessageCreator(final Serializable serialized, final Properties props) {
        this.serialized = serialized;
        this.properties = props;
    }

    protected void updateProperties(Message message) throws JMSException {
        // append the name of the server
        message.setObjectProperty(
                JMSConfiguration.INSTANCE_NAME_KEY,
                properties.get(JMSConfiguration.INSTANCE_NAME_KEY));

        // set other properties
        final Set<Entry<Object, Object>> set = properties.entrySet();
        final Iterator<Entry<Object, Object>> it = set.iterator();
        while (it.hasNext()) {
            final Entry<Object, Object> entry = it.next();
            message.setObjectProperty(entry.getKey().toString(), entry.getValue());
        }
    }

    @Override
    public Message createMessage(Session session) throws JMSException {

        ObjectMessage message;
        try {
            message = session.createObjectMessage(serialized);
        } catch (Exception e) {
            final JMSException ex = new JMSException(e.getLocalizedMessage());
            ex.initCause(e);
            throw ex;
        }

        // // set the used SPI
        // message.setStringProperty(JMSEventHandlerSPI.getKeyName(),
        // handlerGenerator.getSimpleName());

        // append properties
        updateProperties(message);

        return message;
    }
}
