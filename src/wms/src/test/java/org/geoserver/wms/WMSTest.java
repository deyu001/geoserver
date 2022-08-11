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

package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.renderer.style.ImageGraphicFactory;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class WMSTest extends WMSTestSupport {

    static final QName TIME_WITH_START_END =
            new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);
    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addVectorLayer(
                TIME_WITH_START_END,
                Collections.emptyMap(),
                "TimeElevationWithStartEnd.properties",
                getClass(),
                getCatalog());
    }

    protected void setupStartEndTimeDimension(
            String featureTypeName, String dimension, String start, String end) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(featureTypeName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(dimension, di);
        getCatalog().save(info);
    }

    @Before
    public void setWMS() throws Exception {
        wms = new WMS(getGeoServer());
    }

    @Test
    public void testGetTimeElevationToFilterStartEndDate() throws Exception {

        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "elevation", "startElevation", "endElevation");

        /* Reference for test assertions
        TimeElevation.0=0|2012-02-11|2012-02-12|1|2
        TimeElevation.1=1|2012-02-12|2012-02-13|2|3
        TimeElevation.2=2|2012-02-11|2012-02-14|1|3
         */

        doTimeElevationFilter(Date.valueOf("2012-02-10"), null);
        doTimeElevationFilter(Date.valueOf("2012-02-11"), null, 0, 2);
        doTimeElevationFilter(Date.valueOf("2012-02-12"), null, 0, 1, 2);
        doTimeElevationFilter(Date.valueOf("2012-02-13"), null, 1, 2);
        doTimeElevationFilter(Date.valueOf("2012-02-15"), null);

        // Test start and end before all ranges.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-10")), null);
        // Test start before and end during a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-11")), null, 0, 2);
        // Test start on and end after or during a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-11"), Date.valueOf("2012-02-13")),
                null,
                0,
                1,
                2);
        // Test start before and end after all ranges.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-14")),
                null,
                0,
                1,
                2);
        // Test start during and end after a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-13"), Date.valueOf("2012-02-14")), null, 1, 2);
        // Test start during and end during a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-12"), Date.valueOf("2012-02-13")),
                null,
                0,
                1,
                2);
        // Test start and end after all ranges.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-15"), Date.valueOf("2012-02-16")), null);

        doTimeElevationFilter(null, 0);
        doTimeElevationFilter(null, 1, 0, 2);
        doTimeElevationFilter(null, 2, 0, 1, 2);
        doTimeElevationFilter(null, 3, 1, 2);
        doTimeElevationFilter(null, 4);

        doTimeElevationFilter(null, new NumberRange<>(Integer.class, -1, 0));
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, -1, 1), 0, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, 1, 3), 0, 1, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, -1, 4), 0, 1, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, 3, 4), 1, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, 4, 5));

        // combined date/elevation - this should be an 'and' filter
        doTimeElevationFilter(Date.valueOf("2012-02-12"), 2, 0, 1, 2);
        // disjunct verification
        doTimeElevationFilter(Date.valueOf("2012-02-11"), 3, 2);
    }

    public void doTimeElevationFilter(Object time, Object elevation, Integer... expectedIds)
            throws Exception {

        FeatureTypeInfo timeWithStartEnd =
                getCatalog().getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        FeatureSource fs = timeWithStartEnd.getFeatureSource(null, null);

        List<Object> times = time == null ? null : Arrays.asList(time);
        List<Object> elevations = elevation == null ? null : Arrays.asList(elevation);

        Filter filter = wms.getTimeElevationToFilter(times, elevations, timeWithStartEnd);
        FeatureCollection features = fs.getFeatures(filter);

        Set<Integer> results = new HashSet<>();
        try (FeatureIterator it = features.features()) {
            while (it.hasNext()) {
                results.add((Integer) it.next().getProperty("id").getValue());
            }
        }
        assertTrue(
                "expected " + Arrays.toString(expectedIds) + " but got " + results,
                results.containsAll(Arrays.asList(expectedIds)));
        assertTrue(
                "expected " + Arrays.toString(expectedIds) + " but got " + results,
                Arrays.asList(expectedIds).containsAll(results));
    }

    @Test
    public void testWMSLifecycleHandlerGraphicCacheReset() throws Exception {

        Iterator<ExternalGraphicFactory> it =
                DynamicSymbolFactoryFinder.getExternalGraphicFactories();
        Map<URL, BufferedImage> imageCache = null;
        while (it.hasNext()) {
            ExternalGraphicFactory egf = it.next();
            if (egf instanceof ImageGraphicFactory) {
                Field cache = egf.getClass().getDeclaredField("imageCache");
                cache.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<URL, BufferedImage> cast = (Map) cache.get(egf);
                imageCache = cast;
                URL u = new URL("http://boundless.org");
                BufferedImage b = new BufferedImage(6, 6, 8);
                imageCache.put(u, b);
            }
        }
        assertNotEquals(0, imageCache.size());
        getGeoServer().reload();
        assertEquals(0, imageCache.size());
    }

    @Test
    public void testCacheConfiguration() {
        assertFalse(wms.isRemoteStylesCacheEnabled());

        WMSInfo info = wms.getServiceInfo();
        info.setCacheConfiguration(new CacheConfiguration(true));
        getGeoServer().save(info);
        assertTrue(wms.isRemoteStylesCacheEnabled());
    }

    @Test
    public void testProjectionDensification() {
        assertFalse(wms.isAdvancedProjectionDensificationEnabled());

        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY, true);
        getGeoServer().save(info);
        assertTrue(wms.isAdvancedProjectionDensificationEnabled());
    }

    @Test
    public void testWrappingHeuristic() {
        assertFalse(wms.isDateLineWrappingHeuristicDisabled());

        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.DATELINE_WRAPPING_HEURISTIC_KEY, true);
        getGeoServer().save(info);
        assertTrue(wms.isDateLineWrappingHeuristicDisabled());
    }

    @Test
    public void testRootLayerInCapabilitiesEanbled() {
        assertTrue(wms.isRootLayerInCapabilitesEnabled());

        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY, false);
        getGeoServer().save(info);
        assertFalse(wms.isRootLayerInCapabilitesEnabled());
    }
}
