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

package org.geoserver.wfs;

import javax.xml.namespace.QName;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import org.geoserver.wfs.request.TransactionRequest;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;

/**
 * Event carrying information about a change that happened/that is about to occur.
 *
 * <p>The feature collection may be an in-memory one, or may be based on a real data store with a
 * filter.
 *
 * <p><b>Note</b> that care should be taken when relying on feature identifiers from a {@link
 * TransactionEventType#POST_INSERT} event. Depending on the type of store those identifiers may be
 * reliable. Essentially they can only be relied upon in the case of a spatial dbms (such as
 * PostGIS) is being used.
 */
public class TransactionEvent {
    private TransactionEventType type;
    private SimpleFeatureCollection affectedFeatures;
    private QName layerName;
    private Object source;
    private final TransactionRequest request;

    public TransactionEvent(
            TransactionEventType type,
            TransactionRequest request,
            QName layerName,
            SimpleFeatureCollection affectedFeatures) {
        this(type, request, layerName, affectedFeatures, null);
    }

    public TransactionEvent(
            TransactionEventType type,
            TransactionRequest request,
            QName layerName,
            SimpleFeatureCollection affectedFeatures,
            Object source) {
        this.type = type;
        this.request = request;
        this.layerName = layerName;
        this.affectedFeatures = affectedFeatures;
        this.source = source;
    }

    /** The type of change occurring */
    public TransactionEventType getType() {
        return type;
    }

    /**
     * A collection of the features that are being manipulated. Accessible and usable only when the
     * event is being thrown, if you store the event and try to access the collection later there is
     * no guarantee it will still be usable.
     */
    public SimpleFeatureCollection getAffectedFeatures() {
        return affectedFeatures;
    }

    /** The name of the layer / feature type that this transaction effects. */
    public QName getLayerName() {
        return layerName;
    }

    /** Sets the source of the transction. */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * Returns the source of the transaction.
     *
     * <p>One of:
     *
     * <ul>
     *   <li>{@link InsertElementType}
     *   <li>{@link UpdateElementType}
     *   <li>{@link DeleteElementType}
     * </ul>
     */
    public Object getSource() {
        return source;
    }

    public TransactionType getRequest() {
        return TransactionRequest.WFS11.unadapt(request);
    }

    /**
     * Returns the current GeoTools Data {@link Transaction} associated with this event. May be
     * {@code null} for post-commit events.
     */
    public Transaction getTransaction() {
        return request.getTransaction();
    }
}
