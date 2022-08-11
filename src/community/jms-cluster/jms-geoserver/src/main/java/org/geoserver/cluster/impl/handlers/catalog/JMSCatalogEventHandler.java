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

package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.jms.JMSException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;

/**
 * Abstract class which use Xstream as message serializer/de-serializer. We extend this class to
 * implementing synchronize method.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class JMSCatalogEventHandler extends JMSEventHandler<String, CatalogEvent> {
    public JMSCatalogEventHandler(
            final XStream xstream, Class<JMSEventHandlerSPI<String, CatalogEvent>> clazz) {
        super(xstream, clazz);
        // omit not serializable fields
        omitFields();
    }

    /**
     * omit not serializable fields
     *
     * @see {@link XStream}
     */
    private void omitFields() {
        // omit not serializable fields
        xstream.omitField(CatalogImpl.class, "listeners");
        xstream.omitField(CatalogImpl.class, "facade");
        xstream.omitField(CatalogImpl.class, "resourcePool");
        xstream.omitField(CatalogImpl.class, "resourceLoader");
    }

    @Override
    public String serialize(CatalogEvent event) throws Exception {
        return xstream.toXML(removeCatalogProperties(event));
    }

    @Override
    public CatalogEvent deserialize(String s) throws Exception {

        final Object source = xstream.fromXML(s);
        if (source instanceof CatalogEvent) {
            final CatalogEvent ev = (CatalogEvent) source;
            if (LOGGER.isLoggable(Level.FINE)) {
                final CatalogInfo info = ev.getSource();
                LOGGER.fine("Incoming message event of type CatalogEvent: " + info.getId());
            }
            return ev;
        } else {
            throw new JMSException("Unable to deserialize the following object:\n" + s);
        }
    }

    /** Make sure that properties of type catalog are not serialized for catalog modified events. */
    private CatalogEvent removeCatalogProperties(CatalogEvent event) {
        if (!(event instanceof CatalogModifyEvent)) {
            // not a modify event so nothing to do
            return event;
        }
        CatalogModifyEvent modifyEvent = (CatalogModifyEvent) event;
        // index all the properties that are not of catalog type
        List<Integer> indexes = new ArrayList<>();
        int totalProperties = modifyEvent.getPropertyNames().size();
        for (int i = 0; i < totalProperties; i++) {
            // we only need to check the new values
            Object value = modifyEvent.getNewValues().get(i);
            if (!(value instanceof Catalog)) {
                // not a property of type catalog
                indexes.add(i);
            }
        }
        // let's see if we need to do anything
        if (indexes.size() == totalProperties) {
            // no properties of type catalog, we can use the original event
            return event;
        }
        // well we need to create a new modify event and ignore the properties of catalog type
        List<String> properties = new ArrayList<>();
        List<Object> oldValues = new ArrayList<>();
        List<Object> newValues = new ArrayList<>();
        for (int index : indexes) {
            // add all the properties that are not of catalog type
            properties.add(modifyEvent.getPropertyNames().get(index));
            oldValues.add(modifyEvent.getOldValues().get(index));
            newValues.add(modifyEvent.getNewValues().get(index));
        }
        // crete the new event
        CatalogModifyEventImpl newEvent = new CatalogModifyEventImpl();
        newEvent.setPropertyNames(properties);
        newEvent.setOldValues(oldValues);
        newEvent.setNewValues(newValues);
        newEvent.setSource(modifyEvent.getSource());
        return newEvent;
    }
}
