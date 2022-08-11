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

import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.GetGmlObjectType;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.xml.transform.TransformerBase;

/**
 * Web Feature Service implementation, versions 1.0, and 1.1.
 *
 * <p>Each of the methods on this class corresponds to an operation as defined by the Web Feature
 * Specification. See {@link "http://www.opengeospatial.org/standards/wfs"} for more details.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WebFeatureService {
    /** The configuration of the service. */
    WFSInfo getServiceInfo();

    /**
     * WFS GetCapabilities operation.
     *
     * @param request The get capabilities request.
     * @return A transformer instance capable of serializing a wfs capabilities document.
     * @throws WFSException Any service exceptions.
     */
    TransformerBase getCapabilities(GetCapabilitiesType request) throws WFSException;

    /**
     * WFS DescribeFeatureType operation.
     *
     * @param request The describe feature type request.
     * @return A set of feature type metadata objects.
     * @throws WFSException Any service exceptions.
     */
    FeatureTypeInfo[] describeFeatureType(DescribeFeatureTypeType request) throws WFSException;

    /**
     * WFS GetFeature operation.
     *
     * @param request The get feature request.
     * @return A feature collection type instance.
     * @throws WFSException Any service exceptions.
     */
    FeatureCollectionResponse getFeature(GetFeatureType request) throws WFSException;

    /**
     * WFS GetFeatureWithLock operation.
     *
     * @param request The get feature with lock request.
     * @return A feature collection type instance.
     * @throws WFSException Any service exceptions.
     */
    FeatureCollectionResponse getFeatureWithLock(GetFeatureWithLockType request)
            throws WFSException;

    /**
     * WFS LockFeatureType operation.
     *
     * @param request The lock feature request.
     * @return A lock feture response type.
     * @throws WFSException An service exceptions.
     */
    LockFeatureResponseType lockFeature(LockFeatureType request) throws WFSException;

    /**
     * WFS transaction operation.
     *
     * @param request The transaction request.
     * @return A transaction response instance.
     * @throws WFSException Any service exceptions.
     */
    TransactionResponseType transaction(TransactionType request) throws WFSException;

    /**
     * WFS GetGmlObject operation.
     *
     * @param request The GetGmlObject request.
     * @return The gml object request.
     * @throws WFSException Any service exceptions.
     */
    Object getGmlObject(GetGmlObjectType request) throws WFSException;

    /**
     * Release lock operation.
     *
     * <p>This is not an official operation of the spec.
     *
     * @param lockId A prefiously held lock id.
     */
    void releaseLock(String lockId) throws WFSException;
}
