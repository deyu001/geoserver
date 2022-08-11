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

package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.ResultTypeType;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geotools.feature.FeatureIterator;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;

/**
 * WFS output format for a GetFeature operation in which the resultType is "hits".
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class HitsOutputFormat extends WFSResponse {

    /** Xml configuration */
    Configuration configuration;

    public HitsOutputFormat(GeoServer gs, Configuration configuration) {
        super(gs, FeatureCollectionResponse.class);

        this.configuration = configuration;
    }

    /** @return "text/xml"; */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    /** Checks that the resultType is of type "hits". */
    public boolean canHandle(Operation operation) {
        GetFeatureType request =
                OwsUtils.parameter(operation.getParameters(), GetFeatureType.class);

        return (request != null) && (request.getResultType() == ResultTypeType.HITS_LITERAL);
    }

    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        WFSInfo wfs = getInfo();

        FeatureCollectionResponse featureCollection = (FeatureCollectionResponse) value;

        // create a new feautre collcetion type with just the numbers
        FeatureCollectionResponse hits = featureCollection.create();
        if (GML3OutputFormat.isComplexFeature(featureCollection)) {
            // we have to count the number of features here manually because complex feature
            // collection size() now returns 0. In order to count the number of features,
            // we have to build the features to count them and this has great performance
            // impact. Unless we introduce joins in our fetching of
            // data, we will have to count the number of features manually when needed. In
            // GML3Outputformat I use xslt to populate numberOfFeatures attribute.
            hits.setNumberOfFeatures(countFeature(featureCollection));
        } else {
            hits.setNumberOfFeatures(featureCollection.getNumberOfFeatures());
        }

        hits.setTotalNumberOfFeatures(featureCollection.getTotalNumberOfFeatures());
        hits.setNext(featureCollection.getNext());
        hits.setPrevious(featureCollection.getPrevious());
        hits.setTimeStamp(featureCollection.getTimeStamp());

        encode(hits, output, wfs);
    }

    private BigInteger countFeature(FeatureCollectionResponse fct) {
        BigInteger count = BigInteger.valueOf(0);
        for (int fcIndex = 0; fcIndex < fct.getFeature().size(); fcIndex++) {
            FeatureIterator i = null;
            try {
                for (i = (fct.getFeature().get(fcIndex).features()); i.hasNext(); i.next()) {
                    count = count.add(BigInteger.ONE);
                }
            } finally {
                if (i != null) {
                    i.close();
                }
            }
        }
        return count;
    }

    protected void encode(FeatureCollectionResponse hits, OutputStream output, WFSInfo wfs)
            throws IOException {
        XSDSchema result;
        try {
            result = configuration.getXSD().getSchema();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Encoder encoder = new Encoder(configuration, result);
        encoder.setEncoding(Charset.forName(wfs.getGeoServer().getSettings().getCharset()));
        encoder.setSchemaLocation(
                org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                ResponseUtils.appendPath(wfs.getSchemaBaseURL(), "wfs/1.1.0/wfs.xsd"));

        encoder.encode(
                hits.getAdaptee(), org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION, output);
    }
}
