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

package org.geoserver.wfs.xml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.common.util.EList;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;

public class GML3FeatureProducerTest extends WFSTestSupport {

    GML3OutputFormat producer() {
        FeatureTypeSchemaBuilder sb = new FeatureTypeSchemaBuilder.GML3(getGeoServer());
        WFSConfiguration configuration = new WFSConfiguration(getGeoServer(), sb, new WFS(sb));
        return new GML3OutputFormat(getGeoServer(), configuration);
    }

    /**
     * Build a GetFeature operation to request the named types.
     *
     * @param names type names for which queries are present in the returned request
     * @return GetFeature operation to request the named types
     */
    Operation request(QName... names) {
        Service service = getServiceDescriptor10();
        GetFeatureType type = WfsFactory.eINSTANCE.createGetFeatureType();
        type.setBaseUrl("http://localhost:8080/geoserver");
        for (QName name : names) {
            QueryType queryType = WfsFactory.eINSTANCE.createQueryType();
            queryType.setTypeName(Collections.singletonList(name));
            @SuppressWarnings("unchecked")
            EList<QueryType> query = type.getQuery();
            query.add(queryType);
        }
        Operation request = new Operation("wfs", service, null, new Object[] {type});
        return request;
    }

    @Test
    public void testSingle() throws Exception {
        FeatureSource<? extends FeatureType, ? extends Feature> source =
                getFeatureSource(MockData.SEVEN);
        FeatureCollection<? extends FeatureType, ? extends Feature> features = source.getFeatures();

        FeatureCollectionResponse fcType =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        fcType.getFeature().add(features);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        producer().write(fcType, output, request(MockData.SEVEN));

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(7, document.getElementsByTagName("cdf:Seven").getLength());
    }

    @Test
    public void testMultipleSameNamespace() throws Exception {
        FeatureCollectionResponse fcType =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fcType.getFeature().add(getFeatureSource(MockData.SEVEN).getFeatures());
        fcType.getFeature().add(getFeatureSource(MockData.FIFTEEN).getFeatures());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        producer().write(fcType, output, request(MockData.SEVEN, MockData.FIFTEEN));

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(
                7 + 15,
                document.getElementsByTagName("cdf:Seven").getLength()
                        + document.getElementsByTagName("cdf:Fifteen").getLength());
    }

    @Test
    public void testMultipleDifferentNamespace() throws Exception {
        FeatureCollectionResponse fcType =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fcType.getFeature().add(getFeatureSource(MockData.SEVEN).getFeatures());
        fcType.getFeature().add(getFeatureSource(MockData.POLYGONS).getFeatures());

        int npolys = getFeatureSource(MockData.POLYGONS).getFeatures().size();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        producer().write(fcType, output, request(MockData.SEVEN, MockData.POLYGONS));

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(
                7 + npolys,
                document.getElementsByTagName("cdf:Seven").getLength()
                        + document.getElementsByTagName("cgf:Polygons").getLength());
    }
}
