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

package org.geoserver.nsg;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.nsg.versioning.TimeVersioning;
import org.geoserver.util.IOUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.Converters;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;

public final class TestsUtils {

    public static final ProgressListener NULL_PROGRESS_LISTENER = new NullProgressListener();

    public static final Hints EMPTY_HINTS = new Hints();

    private TestsUtils() {}

    public static String readResource(String resourceName) {
        try (InputStream input = TestsUtils.class.getResourceAsStream(resourceName)) {
            return IOUtils.toString(input);
        } catch (Exception exception) {
            throw new RuntimeException(String.format("Error reading resource '%s'.", resourceName));
        }
    }

    public static void updateFeatureTypeTimeVersioning(
            Catalog catalog,
            String featureTypeName,
            boolean enabled,
            String idProperty,
            String timeProperty) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(featureTypeName);
        if (enabled) {
            TimeVersioning.enable(featureType, idProperty, timeProperty);
        } else {
            TimeVersioning.disable(featureType);
        }
        catalog.save(featureType);
    }

    public static List<SimpleFeature> searchFeatures(Catalog catalog, String featureTypeName) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(featureTypeName);
        if (featureType == null) {
            throw new RuntimeException(
                    String.format("Feature type '%s' not found.", featureTypeName));
        }
        FeatureSource source;
        try {
            source = featureType.getFeatureSource(NULL_PROGRESS_LISTENER, EMPTY_HINTS);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error obtaining feature source of feature type '%s'.",
                            featureTypeName),
                    exception);
        }
        FeatureCollection collection;
        try {
            collection = source.getFeatures();
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error obtaining feature collection for feature type '%s'.",
                            featureTypeName),
                    exception);
        }
        try (FeatureIterator iterator = collection.features()) {
            List<SimpleFeature> features = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = (SimpleFeature) iterator.next();
                features.add(feature);
            }
            return features;
        }
    }

    public static List<SimpleFeature> searchFeatures(
            List<SimpleFeature> features,
            String namePropertyName,
            String timePropertyName,
            String expectedName,
            Date expectedTime,
            int toleranceInSeconds) {
        return features.stream()
                .filter(
                        feature -> {
                            String name =
                                    Converters.convert(
                                            feature.getAttribute(namePropertyName), String.class);
                            if (name == null || !name.equals(expectedName)) {
                                return false;
                            }
                            Date time =
                                    Converters.convert(
                                            feature.getAttribute(timePropertyName), Date.class);
                            return dateEqualWitTolerance(time, expectedTime, toleranceInSeconds);
                        })
                .collect(Collectors.toList());
    }

    private static boolean dateEqualWitTolerance(
            Date time, Date expectedTime, int toleranceInSeconds) {
        if (time == null && expectedTime == null) {
            return true;
        }
        if (time == null || expectedTime == null) {
            return false;
        }
        return time.getTime() <= expectedTime.getTime() + toleranceInSeconds * 1000
                && time.getTime() >= expectedTime.getTime() - toleranceInSeconds * 1000;
    }
}
