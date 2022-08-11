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

package org.geoserver.cluster.hazelcast;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.util.logging.Logger;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.geotools.util.logging.Logging;

/**
 * A {@link ResourceNotificationDispatcher} implementation .
 *
 * <p>A Spring bean of this type shall be configured in the project's {@code applicationContext.xml}
 * spring configuration file in order for {@link ResourceStore} to find it.
 */
public class HzResourceNotificationDispatcher extends SimpleResourceNotificationDispatcher
        implements MessageListener<ResourceNotification> {

    static final String TOPIC_NAME = "resourceWatcher";

    private static final Logger LOGGER = Logging.getLogger(HzResourceNotificationDispatcher.class);

    private HzCluster cluster;

    public HzResourceNotificationDispatcher() {}

    public HzResourceNotificationDispatcher(HzCluster cluster) {
        this.cluster = cluster;
        topic().addMessageListener(this);
    }

    public void setCluster(HzCluster cluster) {
        this.cluster = cluster;
        topic().addMessageListener(this);
    }

    private ITopic<ResourceNotification> topic() {
        if (cluster == null) {
            return null;
        }
        return cluster.getHz().getTopic(TOPIC_NAME);
    }

    @Override
    public void changed(ResourceNotification event) {
        ITopic<ResourceNotification> topic = topic();
        if (topic != null) {
            topic.publish(event);
        } else {
            LOGGER.warning(
                    "Failed to publish resource notification, cluster not initialized (yet).");
            super.changed(event);
        }
    }

    @Override
    public void onMessage(Message<ResourceNotification> msg) {
        LOGGER.info("Received ResourceNotification from HazelCast: " + msg.getMessageObject());

        super.changed(msg.getMessageObject());
    }
}
