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


package org.geoserver.nsg.pagination.random;

import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Logger;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.ResultTypeType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wfs.DefaultWebFeatureService20;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.v2_0.WfsXmlReader;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

/**
 * This service supports the PageResults operation and manage it
 *
 * @author sandr
 */
public class PageResultsWebFeatureService extends DefaultWebFeatureService20 {

    static Logger LOGGER = Logging.getLogger(PageResultsWebFeatureService.class);

    private static final String GML32_FORMAT = "application/gml+xml; version=3.2";

    private static final BigInteger DEFAULT_START = new BigInteger("0");

    private static final BigInteger DEFAULT_COUNT = new BigInteger("10");

    private IndexConfigurationManager indexConfiguration;

    private final WfsXmlReader wfsXmlReader;

    public PageResultsWebFeatureService(
            GeoServer geoServer, IndexConfigurationManager indexConfiguration) {
        super(geoServer);
        this.indexConfiguration = indexConfiguration;
        wfsXmlReader = new WfsXmlReader("GetFeature", geoServer);
    }

    /**
     * Recovers the stored request with associated {@link #resultSetID} and overrides the parameters
     * using the ones provided with current operation or the default values:
     *
     * <ul>
     *   <li>{@link net.opengis.wfs20.GetFeatureType#getStartIndex <em>StartIndex</em>}
     *   <li>{@link net.opengis.wfs20.GetFeatureType#getCount <em>Count</em>}
     *   <li>{@link net.opengis.wfs20.GetFeatureType#getOutputFormat <em>OutputFormat</em>}
     *   <li>{@link net.opengis.wfs20.GetFeatureType#getResultType <em>ResultType</em>}
     * </ul>
     *
     * Then executes the GetFeature operation using the WFS 2.0 service implementation and return is
     * result.
     */
    public FeatureCollectionResponse pageResults(GetFeatureType request) throws Exception {
        // Retrieve stored request
        String resultSetId = (String) Dispatcher.REQUEST.get().getKvp().get("resultSetID");
        GetFeatureType gft = getFeature(resultSetId);

        // Update with incoming parameters or index request or defaults
        Method setBaseUrl = OwsUtils.setter(gft.getClass(), "baseUrl", String.class);
        setBaseUrl.invoke(gft, new Object[] {request.getBaseUrl()});
        BigInteger startIndex =
                request.getStartIndex() != null
                        ? request.getStartIndex()
                        : gft.getStartIndex() != null ? gft.getStartIndex() : DEFAULT_START;
        BigInteger count =
                request.getCount() != null
                        ? request.getCount()
                        : gft.getCount() != null ? gft.getCount() : DEFAULT_COUNT;
        String outputFormat =
                request.getOutputFormat() != null ? request.getOutputFormat() : GML32_FORMAT;
        ResultTypeType resultType =
                request.getResultType() != null ? request.getResultType() : ResultTypeType.RESULTS;
        gft.setStartIndex(startIndex);
        gft.setCount(count);
        gft.setOutputFormat(outputFormat);
        gft.setResultType(resultType);
        // Execute as getFeature
        return super.getFeature(gft);
    }

    /** Helper method that deserializes GetFeature request and updates its last utilization */
    private GetFeatureType getFeature(String resultSetId) throws IOException {
        GetFeatureType feature = null;
        Transaction transaction = new DefaultTransaction("Update");
        try {
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().lock();
            // Update GetFeature utilization
            DataStore currentDataStore = this.indexConfiguration.getCurrentDataStore();
            SimpleFeatureStore store =
                    (SimpleFeatureStore)
                            currentDataStore.getFeatureSource(
                                    IndexConfigurationManager.STORE_SCHEMA_NAME);
            store.setTransaction(transaction);
            Filter filter = CQL.toFilter("ID = '" + resultSetId + "'");
            store.modifyFeatures("updated", new Date().getTime(), filter);
            // Retrieve GetFeature from file
            Resource storageResource = this.indexConfiguration.getStorageResource();

            try (ObjectInputStream is =
                    new ObjectInputStream(
                            new FileInputStream(
                                    new File(storageResource.dir(), resultSetId + ".feature")))) {
                RequestData data = (RequestData) is.readObject();
                if (data == null) {
                    KvpRequestReader kvpReader =
                            Dispatcher.findKvpRequestReader(GetFeatureType.class);
                    Object requestBean = kvpReader.createRequest();
                    feature =
                            (GetFeatureType)
                                    kvpReader.read(requestBean, data.getKvp(), data.getRawKvp());
                } else {
                    byte[] bytes = data.getPostRequest().getBytes(StandardCharsets.UTF_8.name());
                    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                    feature =
                            (GetFeatureType)
                                    wfsXmlReader.read(
                                            null,
                                            new InputStreamReader(input),
                                            Collections.emptyMap());
                }
            }

        } catch (Exception t) {
            transaction.rollback();
            throw new RuntimeException("Error on retrive feature", t);
        } finally {
            transaction.close();
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().unlock();
        }
        return feature;
    }
}
