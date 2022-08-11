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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.TransactionRequest;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Web Feature Service implementation.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class DefaultWebFeatureService implements WebFeatureService, ApplicationContextAware {
    /** GeoServer configuration */
    protected GeoServer geoServer;
    /** The catalog */
    protected Catalog catalog;

    /** Filter factory */
    protected FilterFactory2 filterFactory;

    /**
     * The spring application context, used to look up transaction listeners, plugins and element
     * handlers
     */
    protected ApplicationContext context;

    public DefaultWebFeatureService(GeoServer gs) {
        this.geoServer = gs;
        this.catalog = gs.getCatalog();
    }

    /** Sets the fitler factory. */
    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }

    public WFSInfo getServiceInfo() {
        return geoServer.getService(WFSInfo.class);
    }

    /**
     * WFS GetCapabilities operation.
     *
     * @param request The get capabilities request.
     * @return A transformer instance capable of serializing a wfs capabilities document.
     * @throws WFSException Any service exceptions.
     */
    public TransformerBase getCapabilities(GetCapabilitiesType request) throws WFSException {
        return new GetCapabilities(
                        getServiceInfo(),
                        catalog,
                        WFSExtensions.findExtendedCapabilitiesProviders(context))
                .run(new GetCapabilitiesRequest.WFS11(request));
    }

    /**
     * WFS DescribeFeatureType operation.
     *
     * @param request The describe feature type request.
     * @return A set of feature type metadata objects.
     * @throws WFSException Any service exceptions.
     */
    public FeatureTypeInfo[] describeFeatureType(DescribeFeatureTypeType request)
            throws WFSException {
        return new DescribeFeatureType(getServiceInfo(), catalog)
                .run(new DescribeFeatureTypeRequest.WFS11(request));
    }

    /**
     * WFS GetFeature operation.
     *
     * @param request The get feature request.
     * @return A feature collection type instance.
     * @throws WFSException Any service exceptions.
     */
    public FeatureCollectionResponse getFeature(GetFeatureType request) throws WFSException {
        GetFeature getFeature = new GetFeature(getServiceInfo(), catalog);
        getFeature.setFilterFactory(filterFactory);

        return getFeature.run(new GetFeatureRequest.WFS11(request));
    }

    /**
     * WFS GetFeatureWithLock operation.
     *
     * @param request The get feature with lock request.
     * @return A feature collection type instance.
     * @throws WFSException Any service exceptions.
     */
    public FeatureCollectionResponse getFeatureWithLock(GetFeatureWithLockType request)
            throws WFSException {
        return getFeature(request);
    }

    /**
     * WFS LockFeatureType operation.
     *
     * @param request The lock feature request.
     * @return A lock feature response type.
     * @throws WFSException An service exceptions.
     */
    public LockFeatureResponseType lockFeature(LockFeatureType request) throws WFSException {
        LockFeature lockFeature = new LockFeature(getServiceInfo(), catalog);
        lockFeature.setFilterFactory(filterFactory);

        return (LockFeatureResponseType)
                lockFeature.lockFeature(new LockFeatureRequest.WFS11(request)).getAdaptee();
    }

    /**
     * WFS transaction operation.
     *
     * @param request The transaction request.
     * @return A transaction response instance.
     * @throws WFSException Any service exceptions.
     */
    public TransactionResponseType transaction(TransactionType request) throws WFSException {
        Transaction transaction = new Transaction(getServiceInfo(), catalog, context);
        transaction.setFilterFactory(filterFactory);

        return (TransactionResponseType)
                transaction.transaction(new TransactionRequest.WFS11(request)).getAdaptee();
    }

    /**
     * WFS GetGmlObject operation.
     *
     * @param request The GetGmlObject request.
     * @return The gml object request.
     * @throws WFSException Any service exceptions.
     */
    public Object getGmlObject(GetGmlObjectType request) throws WFSException {

        GetGmlObject getGmlObject = new GetGmlObject(getServiceInfo(), catalog);
        getGmlObject.setFilterFactory(filterFactory);

        return getGmlObject.run(request);
    }

    // the following operations are not part of the spec
    public void releaseLock(String lockId) throws WFSException {
        new LockFeature(getServiceInfo(), catalog).release(lockId);
    }

    public void releaseAllLocks() throws WFSException {
        new LockFeature(getServiceInfo(), catalog).releaseAll();
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
