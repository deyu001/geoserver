/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

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

package org.geoserver.gwc.wmts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ResourceAccessManagerVectorTimeTest extends VectorTimeTestSupport {

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the users */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_high", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_low", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // ------

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo vector = catalog.getFeatureTypeByName(getLayerId(VECTOR_ELEVATION));

        // Give cite_high only records with higher elevation
        Filter elevationHigh = ff.greater(ff.property("startElevation"), ff.literal(2));
        tam.putLimits(
                "cite_high",
                vector,
                new VectorAccessLimits(CatalogMode.HIDE, null, elevationHigh, null, null));

        // And cite_low the opposite
        Filter elevationLow = ff.lessOrEqual(ff.property("startElevation"), ff.literal(2));
        tam.putLimits(
                "cite_low",
                vector,
                new VectorAccessLimits(CatalogMode.HIDE, null, elevationLow, null, null));
    }

    @Test
    public void testGetDomainsValuesCiteHigh() throws Exception {
        login("cite_high", "cite_high");
        // high elevations are associated only to the 11th (start date)
        testDomainsValuesRepresentation(DimensionsUtils.NO_LIMIT, "2012-02-11T00:00:00.000Z");
        testDomainsValuesRepresentation(0, "2012-02-11T00:00:00.000Z--2012-02-11T00:00:00.000Z");
    }

    @Test
    public void testGetDomainsValuesCiteLow() throws Exception {
        login("cite_low", "cite_low");
        // high elevations are associated only to both start dates
        testDomainsValuesRepresentation(
                DimensionsUtils.NO_LIMIT, "2012-02-11T00:00:00.000Z", "2012-02-12T00:00:00.000Z");
        testDomainsValuesRepresentation(0, "2012-02-11T00:00:00.000Z--2012-02-12T00:00:00.000Z");
    }

    @Test
    public void testGetHistogramHigh() {
        login("cite_high", "cite_high");
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1D"));
        // both in the same day
        assertThat(histogram.second, equalTo(Arrays.asList(2)));
    }

    @Test
    public void testGetHistogramLow() {
        login("cite_low", "cite_low");
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-13T00:00:00.000Z/P1D"));
        // one per day
        assertThat(histogram.second, equalTo(Arrays.asList(1, 1)));
    }
}
