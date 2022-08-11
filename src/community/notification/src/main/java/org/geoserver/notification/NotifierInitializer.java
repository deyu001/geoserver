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


package org.geoserver.notification;

import com.thoughtworks.xstream.XStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.notification.common.NotificationConfiguration;
import org.geoserver.notification.common.NotificationXStreamInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

public class NotifierInitializer implements GeoServerInitializer {

    static Logger LOGGER = Logging.getLogger(NotifierInitializer.class);

    public static final String PROPERTYFILENAME = "notifier.xml";
    public static final String THREAD_NAME = "MessageMultiplexer";

    private GeoServerResourceLoader loader;

    public NotifierInitializer(GeoServerResourceLoader loader) {
        this.loader = loader;
    }

    public void initialize(GeoServer geoServer) throws Exception {

        XStream xs = new XStream();
        List<NotificationXStreamInitializer> xstreamInitializers =
                GeoServerExtensions.extensions(NotificationXStreamInitializer.class);
        for (NotificationXStreamInitializer ni : xstreamInitializers) {
            ni.init(xs);
        }
        NotificationConfiguration cfg = getConfiguration(xs);
        MessageMultiplexer mm = new MessageMultiplexer(cfg);

        List<INotificationCatalogListener> catalogListeners =
                GeoServerExtensions.extensions(INotificationCatalogListener.class);
        for (INotificationCatalogListener cl : catalogListeners) {
            cl.setMessageMultiplexer(mm);
            geoServer.getCatalog().addListener(cl);
        }

        List<INotificationTransactionListener> transactionListeners =
                GeoServerExtensions.extensions(INotificationTransactionListener.class);
        for (INotificationTransactionListener tl : transactionListeners) {
            tl.setMessageMultiplexer(mm);
        }

        (new Thread(mm, THREAD_NAME)).start();
    }

    private NotificationConfiguration getConfiguration(XStream xs) {
        NotificationConfiguration nc = null;
        try {
            Resource f = this.loader.get(Paths.path("notifier", PROPERTYFILENAME));
            if (!Resources.exists(f)) {
                /*
                 * Copy and use the sample notifier
                 */
                IOUtils.copy(
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(NotifierInitializer.PROPERTYFILENAME),
                        f.file());
            }
            nc = (NotificationConfiguration) xs.fromXML(f.in());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return nc;
    }
}
