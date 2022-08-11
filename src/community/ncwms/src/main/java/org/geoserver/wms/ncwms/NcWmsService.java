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


package org.geoserver.wms.ncwms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.LayerIdentifier;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.DateRange;
import org.geotools.util.SimpleInternationalString;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Implements the methods of the NcWMS service which are not included on the WMS standard. For the
 * moment, only GetTimeSeries method is supported.
 */
public class NcWmsService {

    public static final String TIME_SERIES_INFO_FORMAT_PARAM_NAME = "TIME_SERIES_INFO_FORMAT";

    public static final String GET_TIME_SERIES_REQUEST = "GetTimeSeries";

    private WMS wms;

    /** Simple helper to enforce rendering timeout */
    private static class CountdownClock {
        long end;

        long maxRenderingTime;

        CountdownClock(long maxRenderingTime) {
            this.maxRenderingTime = maxRenderingTime;
            if (maxRenderingTime > 0) {
                this.end = System.currentTimeMillis() + maxRenderingTime;
            }
        }

        public void checkTimeout() {
            if (maxRenderingTime > 0 && System.currentTimeMillis() > end) {
                throw new ServiceException(
                        "This request used more time than allowed and has been forcefully stopped. "
                                + "Max rendering time is "
                                + (maxRenderingTime / 1000.0)
                                + "s");
            }
        }
    }

    enum DateFinder {
        NEAREST {
            // This is used when nearest match is supported.
            @Override
            List<DateRange> findDates(WMS wms, CoverageInfo coverage, List<Object> times)
                    throws IOException {
                TreeSet<Date> availableDates = wms.queryCoverageNearestMatchTimes(coverage, times);
                return availableDates
                        .stream()
                        .map(date -> new DateRange(date, date))
                        .collect(Collectors.toList());
            }
        },
        QUERY {
            // When nearest match isn't enabled, let's query the coverage to identify
            // available dates only
            @Override
            List<DateRange> findDates(WMS wms, CoverageInfo coverage, List<Object> times)
                    throws IOException {
                TreeSet<Date> availableDates = new TreeSet<Date>();
                final boolean isRange = times.get(0) instanceof DateRange;
                for (Object time : times) {
                    TreeSet<Object> foundTimes =
                            wms.queryCoverageTimes(
                                    coverage, getAsRange(time, isRange), Query.DEFAULT_MAX);
                    foundTimes
                            .stream()
                            .forEach(
                                    d -> {
                                        Date date = (Date) d;
                                        availableDates.add(date);
                                    });
                }
                return availableDates
                        .stream()
                        .map(date -> new DateRange(date, date))
                        .collect(Collectors.toList());
            }
        };

        // times should be a not null List. (Null Check is made before calling this method)
        abstract List<DateRange> findDates(WMS wms, CoverageInfo coverage, List<Object> times)
                throws IOException;

        protected DateRange getAsRange(Object time, boolean isRange) {
            return isRange ? (DateRange) time : new DateRange((Date) time, (Date) time);
        }
    }

    public NcWmsService(final WMS wms) {
        this.wms = wms;
    }

    private LayerIdentifier getLayerIdentifier(MapLayerInfo layer) {
        List<LayerIdentifier> identifiers = GeoServerExtensions.extensions(LayerIdentifier.class);
        for (LayerIdentifier identifier : identifiers) {
            if (identifier.canHandle(layer)) {
                return identifier;
            }
        }

        throw new ServiceException(
                "Could not find any identifier that can handle layer "
                        + layer.getLayerInfo().prefixedName()
                        + " among these identifiers: "
                        + identifiers);
    }

    /**
     * Implements the GetTimeSeries method, which can retrieve a time series of values on a certain
     * point, using a syntax similar to the GetFeatureInfo operation.
     */
    @SuppressWarnings("rawtypes")
    public FeatureCollectionType getTimeSeries(GetFeatureInfoRequest request) {
        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());

        // Process the request only if we have a time range
        List<Object> times = request.getGetMapRequest().getTime();
        if (times == null || times.size() == 0) {
            throw new ServiceException("The TIME parameter was missing");
        }

        final List<MapLayerInfo> requestedLayers = request.getQueryLayers();
        // Process the request only if we have single layer
        if (requestedLayers.size() != 1) {
            throw new ServiceException(
                    "The QUERY_LAYERS parameter must specify a single coverage layer for the GetTimeSeries operation");
        }

        final MapLayerInfo layer = requestedLayers.get(0);
        CoverageInfo coverage;
        try {
            coverage = layer.getCoverage();
        } catch (Exception cex) {
            throw new ServiceException(
                    "The GetTimeSeries operation is only defined for coverage layers");
        }

        // we'll just pick the first band anyways, no need to read them all
        if (request.getPropertyNames() == null
                || request.getPropertyNames().size() == 0
                || request.getPropertyNames().get(0).isEmpty()) {
            String firstBand = coverage.getDimensions().get(0).getName();
            request.setPropertyNames(Arrays.asList(Arrays.asList(firstBand)));
        }

        // control how much time we spend doing queries to gather times and values
        int maxRenderingTime = wms.getMaxRenderingTime(request.getGetMapRequest());
        CountdownClock countdownClock = new CountdownClock(maxRenderingTime);
        LayerIdentifier identifier = getLayerIdentifier(layer);
        SimpleFeatureBuilder featureBuilder =
                getResultFeatureBuilder(layer.getName(), buildTypeDescription(layer));
        try {
            // Get available dates, then perform an identify operation for each date in the range
            List<DateRange> availableDates = getAvailableDates(coverage, times);
            ListFeatureCollection features =
                    new ListFeatureCollection(featureBuilder.getFeatureType());
            TreeSet<Date> addedDates = new TreeSet<Date>();
            request.setExcludeNodataResults(true);
            for (DateRange date : availableDates) {
                // check timeout
                countdownClock.checkTimeout();

                // run query
                request.getGetMapRequest().setTime(Collections.singletonList(date));
                FeatureInfoRequestParameters requestParams =
                        new FeatureInfoRequestParameters(request);
                List<FeatureCollection> identifiedCollections =
                        identifier.identify(requestParams, 1);

                // collect the data
                if (identifiedCollections != null) {
                    for (FeatureCollection c : identifiedCollections) {
                        try (FeatureIterator featIter = c.features()) {
                            if (featIter.hasNext()) { // no need to loop, we just want one value
                                Feature inFeat = featIter.next();
                                Iterator<Property> propIter = inFeat.getProperties().iterator();
                                if (propIter.hasNext()) {
                                    Property prop = propIter.next();
                                    Date dateValue = date.getMinValue();
                                    // check for duplicates while updating the collection
                                    if (!addedDates.contains(dateValue)) {
                                        featureBuilder.add(dateValue);
                                        featureBuilder.add(prop.getValue());
                                        SimpleFeature newFeat = featureBuilder.buildFeature(null);
                                        features.add(newFeat);
                                        addedDates.add(dateValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            result.getFeature().add(features);
        } catch (Exception e) {
            throw new ServiceException("Error processing the operation", e);
        } finally {
            // restore the original times
            request.getGetMapRequest().setTime(times);
        }

        return result;
    }

    private List<DateRange> getAvailableDates(CoverageInfo coverage, List<Object> times)
            throws IOException {

        // We have 3 input cases here:
        // (A) Simple range: e.g. 2018-01-01/2020-01-01
        // (B) Single times list: e.g. 2018-01,2018-02,2018-03,2018-04
        // (C) Range with period: e.g. 2018-01-01/2020-01-01/P1D

        // (A) and (B) will result into a list of DateRange but (A) will only have 1 element
        // (C) will result into a list of Dates

        DimensionInfo timeDimension =
                coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (timeDimension == null || !timeDimension.isEnabled()) {
            throw new ServiceException(
                    "Layer " + coverage.prefixedName() + " does not have time support enabled");
        }

        // We have already checked before invoking this method that the list isn't null nor empty
        final boolean nearestMatch = timeDimension.isNearestMatchEnabled();

        final boolean simpleRange = times.size() == 1 && times.get(0) instanceof DateRange;
        List<DateRange> results;
        if (nearestMatch && simpleRange) {
            results = handleSimpleInterval(coverage, times);
        } else {
            DateFinder finder = nearestMatch ? DateFinder.NEAREST : DateFinder.QUERY;
            results = finder.findDates(wms, coverage, times);
        }
        return results;
    }

    private List<DateRange> handleSimpleInterval(CoverageInfo coverage, List<Object> times)
            throws IOException {
        DateFinder finder = DateFinder.QUERY;
        List<DateRange> results = finder.findDates(wms, coverage, times);
        if (results.size() == 0) results = DateFinder.NEAREST.findDates(wms, coverage, times);
        return results;
    }

    public String buildTypeDescription(MapLayerInfo layer) {
        String name = layer.getName();
        if (layer.getCoverage() != null
                && layer.getCoverage().getDimensions().size() == 1
                && layer.getCoverage().getDimensions().get(0).getName() != null
                && layer.getCoverage().getDimensions().get(0).getUnit() != null) {
            name =
                    layer.getCoverage().getDimensions().get(0).getName()
                            + " ("
                            + layer.getCoverage().getDimensions().get(0).getUnit()
                            + ")";
        }
        return name;
    }

    private SimpleFeatureBuilder getResultFeatureBuilder(String name, String description) {
        // create the builder
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        // set global state
        builder.setName(name);
        builder.setDescription(new SimpleInternationalString(description));
        builder.setNamespaceURI("http://www.geoserver.org/");
        builder.setSRS("EPSG:4326");

        // add attributes
        builder.add("date", Date.class);
        builder.add("value", Double.class);
        SimpleFeatureType featureType = builder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        return featureBuilder;
    }
}
