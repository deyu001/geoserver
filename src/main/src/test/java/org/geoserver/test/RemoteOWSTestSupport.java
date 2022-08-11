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

package org.geoserver.test;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WebMapServer;
import org.opengis.filter.FilterFactory;

/**
 * Utility class used to check wheter REMOTE_OWS_XXX related tests can be run against the demo
 * server or not or not.
 *
 * @author Andrea Aime - TOPP
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class RemoteOWSTestSupport {

    // support for remote OWS layers
    public static final String TOPP_STATES = "topp:states";

    public static final String WFS_SERVER_URL = "http://demo.opengeo.org/geoserver/wfs?";

    public static final String WMS_SERVER_URL = "http://demo.opengeo.org/geoserver/wms?";

    static Boolean remoteWMSStatesAvailable;

    static Boolean remoteWFSStatesAvailable;

    public static boolean isRemoteWFSStatesAvailable(Logger logger) {
        if (remoteWFSStatesAvailable == null) {
            // let's see if the remote ows tests are enabled to start with
            String value = System.getProperty("remoteOwsTests");
            if (value == null || !"TRUE".equalsIgnoreCase(value)) {
                logger.log(
                        Level.WARNING,
                        "Skipping remote WFS test because they were not enabled via -DremoteOwsTests=true");
                remoteWFSStatesAvailable = Boolean.FALSE;
            } else {
                // let's check if the remote WFS tests are runnable
                try {
                    WFSDataStoreFactory factory = new WFSDataStoreFactory();
                    @SuppressWarnings("unchecked") // incompatible generics here
                    Map<String, Serializable> params =
                            new HashMap(factory.getImplementationHints());
                    URL url =
                            new URL(
                                    WFS_SERVER_URL
                                            + "service=WFS&request=GetCapabilities&version=1.1.0");
                    params.put(WFSDataStoreFactory.URL.key, url);
                    params.put(WFSDataStoreFactory.TRY_GZIP.key, Boolean.TRUE);
                    // give it five seconds to respond...
                    params.put(WFSDataStoreFactory.TIMEOUT.key, Integer.valueOf(5000));
                    DataStore remoteStore = factory.createDataStore(params);
                    FeatureSource fs = remoteStore.getFeatureSource(TOPP_STATES);
                    remoteWFSStatesAvailable = Boolean.TRUE;
                    // check a basic response can be answered correctly
                    Query dq = new Query(TOPP_STATES);
                    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
                    dq.setFilter(ff.greater(ff.property("PERSONS"), ff.literal(20000000)));
                    FeatureCollection fc = fs.getFeatures(dq);
                    if (fc.size() != 1) {
                        logger.log(
                                Level.WARNING,
                                "Remote database status invalid, there should be one and only one "
                                        + "feature with more than 20M persons in topp:states");
                        remoteWFSStatesAvailable = Boolean.FALSE;
                    }

                    logger.log(
                            Level.WARNING,
                            "Remote WFS tests are enabled, remote server appears to be up");
                } catch (IOException e) {
                    logger.log(
                            Level.WARNING,
                            "Skipping remote wms test, either demo  "
                                    + "is down or the topp:states layer is not there",
                            e);
                    remoteWFSStatesAvailable = Boolean.FALSE;
                }
            }
        }
        return remoteWFSStatesAvailable.booleanValue();
    }

    public static boolean isRemoteWMSStatesAvailable(Logger logger) {
        if (remoteWMSStatesAvailable == null) {
            // let's see if the remote ows tests are enabled to start with
            String value = System.getProperty("remoteOwsTests");
            if (value == null || !"TRUE".equalsIgnoreCase(value)) {
                logger.log(
                        Level.WARNING,
                        "Skipping remote OWS test because they were not enabled via -DremoteOwsTests=true");
                remoteWMSStatesAvailable = Boolean.FALSE;
            } else {
                // let's check if the remote WFS tests are runnable
                try {
                    remoteWMSStatesAvailable = Boolean.FALSE;
                    WebMapServer server =
                            new WebMapServer(
                                    new URL(WMS_SERVER_URL + "service=WMS&request=GetCapabilities"),
                                    5000);
                    for (Layer l : server.getCapabilities().getLayerList()) {
                        if ("topp:states".equals(l.getName())) {
                            remoteWMSStatesAvailable = Boolean.TRUE;
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.log(
                            Level.WARNING,
                            "Skipping remote WMS test, either demo  "
                                    + "is down or the topp:states layer is not there",
                            e);
                    remoteWMSStatesAvailable = Boolean.FALSE;
                }
            }
        }
        return remoteWMSStatesAvailable.booleanValue();
    }
}
