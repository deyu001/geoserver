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

package org.geoserver.cluster.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Properties;
import javax.jms.JMSException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.cluster.JMSPublisher;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.utils.BeanUtils;
import org.geoserver.cluster.server.events.StyleModifyEvent;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * JMS MASTER (Producer) Listener used to send GeoServer Catalog events over the JMS channel.
 *
 * @see {@link JMSApplicationListener}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSCatalogListener extends JMSAbstractGeoServerProducer implements CatalogListener {

    private static final java.util.logging.Logger LOGGER =
            Logging.getLogger(JMSCatalogListener.class);

    private final JMSPublisher jmsPublisher;
    private final GeoServerResourceLoader loader;
    private final GeoServerDataDirectory dataDirectory;

    /**
     * Constructor
     *
     * @param topicTemplate the getJmsTemplate() object used to send message to the topic queue
     */
    public JMSCatalogListener(
            final Catalog catalog,
            final JMSPublisher jmsPublisher,
            GeoServerResourceLoader loader,
            GeoServerDataDirectory dataDirectory) {
        super();
        this.jmsPublisher = jmsPublisher;
        this.loader = loader;
        this.dataDirectory = dataDirectory;
        catalog.addListener(this);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine(
                    "Incoming event of type " + event.getClass().getSimpleName() + " from Catalog");
        }

        // skip incoming events if producer is not Enabled
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // update properties
        final Properties options = getProperties();

        try {
            // check if we may publish also the file
            final CatalogInfo info = event.getSource();
            if (info instanceof StyleInfo) {
                final StyleInfo sInfo = ((StyleInfo) info);
                WorkspaceInfo wInfo = sInfo.getWorkspace();
                Resource styleFile = null;

                // make sure we work fine with workspace specific styles
                if (wInfo != null) {
                    styleFile =
                            loader.get(
                                    File.separator
                                            + "workspaces"
                                            + File.separator
                                            + wInfo.getName()
                                            + File.separator
                                            + "styles"
                                            + File.separator
                                            + sInfo.getFilename());

                } else {
                    styleFile = loader.get("styles/" + sInfo.getFilename());
                }
                // checks
                if (!Resources.exists(styleFile)
                        || !Resources.canRead(styleFile)
                        || !(styleFile.getType() == Type.RESOURCE)) {
                    throw new IllegalStateException(
                            "Unable to find style for event: " + sInfo.toString());
                }

                // transmit the file
                jmsPublisher.publish(
                        getTopic(), getJmsTemplate(), options, new DocumentFile(styleFile));
            }

            // propagate the event
            jmsPublisher.publish(getTopic(), getJmsTemplate(), options, event);
        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            final CatalogException ex = new CatalogException(e);
            throw ex;
        }
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine(
                    "Incoming message event of type "
                            + event.getClass().getSimpleName()
                            + " from Catalog");
        }

        // skip incoming events until context is loaded
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // update properties
        Properties options = getProperties();

        try {
            jmsPublisher.publish(getTopic(), getJmsTemplate(), options, event);
        } catch (JMSException e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            throw new CatalogException(e);
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine(
                    "Incoming message event of type "
                            + event.getClass().getSimpleName()
                            + " from Catalog");
        }

        // skip incoming events until context is loaded
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // update properties
        Properties options = getProperties();

        // check if we may publish also the file
        CatalogInfo info = event.getSource();

        // if the modified object was a style we need to send the style file too
        if (info instanceof StyleInfo) {
            // we need to get the associated resource file, for this we need to look
            // at the final object we use a proxy to preserver the original object
            StyleInfo styleInfo = ModificationProxy.create((StyleInfo) info, StyleInfo.class);
            // updated the proxy object with the new values
            try {
                BeanUtils.smartUpdate(styleInfo, event.getPropertyNames(), event.getNewValues());
            } catch (Exception exception) {
                // there is nothing we can do about this
                throw new RuntimeException(
                        String.format(
                                "Error setting proxy of style '%s' new values.",
                                styleInfo.getName()),
                        exception);
            }
            // get style associated resource
            Resource resource = dataDirectory.get(styleInfo, styleInfo.getFilename());
            if (!resource.file().exists()) {
                // this should not happen we throw an exception
                throw new RuntimeException(
                        String.format(
                                "Style file '%s' for style '%s' could not be found.",
                                styleInfo.getFilename(), styleInfo.getName()));
            }
            try {
                // read the style file to an array of bytes
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try {
                    IOUtils.copy(resource.in(), output);
                } catch (Exception exception) {
                    throw new RuntimeException(
                            String.format(
                                    "Error reading style '%s' file '%s'.",
                                    styleInfo.getName(), resource.file().getAbsolutePath()),
                            exception);
                }
                // publish the style event
                jmsPublisher.publish(
                        getTopic(),
                        getJmsTemplate(),
                        options,
                        new StyleModifyEvent(event, output.toByteArray()));
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format(
                                "Error publishing file associated with style '%s'.",
                                styleInfo.getName()),
                        exception);
            }
        } else {
            // propagate the catalog modified event
            try {
                jmsPublisher.publish(getTopic(), getJmsTemplate(), options, event);
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format(
                                "Error publishing catalog modified event of type '%s'.",
                                info.getClass().getSimpleName()),
                        exception);
            }
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine(
                    "Incoming message event of type "
                            + event.getClass().getSimpleName()
                            + " from Catalog");
        }

        // EAT EVENT
        // this event should be generated locally (to slaves) by the catalog
        // itself
    }

    @Override
    public void reloaded() {

        // skip incoming events until context is loaded
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // EAT EVENT

        // TODO disable and re-enable the producer!!!!!
        // this is potentially a problem since this listener should be the first
        // called by the GeoServer.
    }
}
