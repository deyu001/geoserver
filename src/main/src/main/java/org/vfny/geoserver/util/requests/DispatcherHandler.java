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

package org.vfny.geoserver.util.requests;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Uses SAX to extact a GetFeature query from and incoming GetFeature request XML stream.
 *
 * <p>Note that this Handler extension ignores Filters completely and must be chained as a parent to
 * the PredicateFilter method in order to recognize them. If it is not chained, it will still
 * generate valid queries, but with no filtering whatsoever.
 *
 * @author Chris Holmes, TOPP
 * @version $Id$
 */
public class DispatcherHandler extends XMLFilterImpl implements ContentHandler {

    /** Stores the internal request type as string */
    private String request = null;

    /** Stores hte internal service type as string */
    private String service = null;

    /** Flags whether or not type has been set */
    private boolean gotType = false;

    /** @return the service type. */
    public String getService() {
        return service;
    }

    /** @return The request type. */
    public String getRequest() {
        return request;
    }

    // JD: kill these methods
    /**
     * Gets the request type. See Dispatcher for the available types.
     *
     * @return an int of the request type.
     */

    //    public int getRequestType() {
    //        return requestType;
    //    }

    /**
     * Gets the service type, for now either WMS or WFS types of Dispatcher.
     *
     * @return an int of the service type.
     */

    //    public int getServiceType() {
    //        return serviceType;
    //    }

    /**
     * Notes the start of the element and checks for request type.
     *
     * @param namespaceURI URI for namespace appended to element.
     * @param localName Local name of element.
     * @param rawName Raw name of element.
     * @param atts Element attributes.
     */
    public void startElement(String namespaceURI, String localName, String rawName, Attributes atts)
            throws SAXException {
        if (gotType) {
            return;
        }

        this.request = localName;

        // JD: kill this
        //            if (localName.equals("GetCapabilities")) {
        //                this.requestType = Dispatcher.GET_CAPABILITIES_REQUEST;
        //            } else if (localName.equals("DescribeFeatureType")) {
        //                this.requestType = Dispatcher.DESCRIBE_FEATURE_TYPE_REQUEST;
        //            } else if (localName.equals("GetFeature")) {
        //                this.requestType = Dispatcher.GET_FEATURE_REQUEST;
        //            } else if (localName.equals("Transaction")) {
        //                this.requestType = Dispatcher.TRANSACTION_REQUEST;
        //            } else if (localName.equals("GetFeatureWithLock")) {
        //                this.requestType = Dispatcher.GET_FEATURE_LOCK_REQUEST;
        //            } else if (localName.equals("LockFeature")) {
        //                this.requestType = Dispatcher.LOCK_REQUEST;
        //            } else if (localName.equals("GetMap")) {
        //                this.requestType = Dispatcher.GET_MAP_REQUEST;
        //            } else if (localName.equals("GetFeatureInfo")) {
        //                this.requestType = Dispatcher.GET_FEATURE_INFO_REQUEST;
        //            } else {
        //                this.requestType = Dispatcher.UNKNOWN;
        //            }
        for (int i = 0, n = atts.getLength(); i < n; i++) {
            if (atts.getLocalName(i).equals("service")) {
                this.service = atts.getValue(i);

                // JD: kill this
                //                if (service.equals("WFS")) {
                //                    this.serviceType = Dispatcher.WFS_SERVICE;
                //                } else if (service.equals("WMS")) {
                //                    this.serviceType = Dispatcher.WMS_SERVICE;
                //                }
                //            } else {
                //                this.serviceType = Dispatcher.UNKNOWN;
            }
        }

        gotType = true;
    }
}
