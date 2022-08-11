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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.xml.v1_0_0.WFSConfiguration;
import org.junit.After;

/**
 * New Base support class for wfs tests.
 *
 * <p>Deriving from this test class provides the test case with preconfigured geoserver and wfs
 * objects.
 *
 * @author Niels Charlier
 */
public abstract class WFSTestSupport extends GeoServerSystemTestSupport {
    /** @return The global wfs instance from the application context. */
    protected WFSInfo getWFS() {
        return getGeoServer().getService(WFSInfo.class);
    }

    /** @return The 1.0 service descriptor. */
    protected Service getServiceDescriptor10() {
        return (Service) GeoServerExtensions.bean("wfsService-1.0.0");
    }

    /** @return The 1.1 service descriptor. */
    protected Service getServiceDescriptor11() {
        return (Service) GeoServerExtensions.bean("wfsService-1.1.0");
    }

    /** @return The 1.0 xml configuration. */
    protected WFSConfiguration getXmlConfiguration10() {
        return (WFSConfiguration) applicationContext.getBean("wfsXmlConfiguration-1.0");
    }

    /** @return The 1.1 xml configuration. */
    protected org.geoserver.wfs.xml.v1_1_0.WFSConfiguration getXmlConfiguration11() {
        return (org.geoserver.wfs.xml.v1_1_0.WFSConfiguration)
                applicationContext.getBean("wfsXmlConfiguration-1.1");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("gs", "http://geoserver.org");
        namespaces.put("soap12", "http://www.w3.org/2003/05/soap-envelope");

        CiteTestData.registerNamespaces(namespaces);

        setUpNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        setUpInternal(testData);
    }

    protected void setUpInternal(SystemTestData testData) throws Exception {}

    protected void setUpNamespaces(Map<String, String> namespaces) {}

    protected List<String> getSupportedSpatialOperatorsList(boolean wfs1_0_0) {
        return Arrays.asList(
                new String[] {
                    "Disjoint",
                    "Equals",
                    "DWithin",
                    "Beyond",
                    "Intersect" + (wfs1_0_0 ? "" : "s"),
                    "Touches",
                    "Crosses",
                    "Within",
                    "Contains",
                    "Overlaps",
                    "BBOX"
                });
    }

    protected Boolean citeCompliant;

    protected void setCiteCompliant(boolean value) {
        WFSInfo wfs = getWFS();
        citeCompliant = wfs.isCiteCompliant();
        wfs.setCiteCompliant(value);
        getGeoServer().save(wfs);
    }

    @After
    public void resetCiteCompliant() {
        if (Objects.nonNull(citeCompliant)) {
            WFSInfo wfs = getWFS();
            wfs.setCiteCompliant(citeCompliant);
            getGeoServer().save(wfs);
        }
    }

    /**
     * Helper method that activates or deactivates geometries measures encoding for the feature type
     * matching the provided name.
     */
    protected static void setMeasuresEncoding(
            Catalog catalog, String featureTypeName, boolean encodeMeasures) {
        // get the feature type from the catalog
        FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByName(featureTypeName);
        if (featureTypeInfo == null) {
            // ouch, feature type not found
            throw new RuntimeException(
                    String.format(
                            "No feature type matching the provided name '%s' found.",
                            featureTypeName));
        }
        // set encode measures and save
        featureTypeInfo.setEncodeMeasures(encodeMeasures);
        catalog.save(featureTypeInfo);
    }
}
