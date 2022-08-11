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

package org.geoserver.data.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * A collection of utilties for dealing with GeotTools Format.
 *
 * @author Richard Gould, Refractions Research, Inc.
 * @author cholmesny
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 * @version $Id: CoverageStoreUtils.java,v 1.12 2004/09/21 21:14:48 cholmesny Exp $
 */
public final class CoverageStoreUtils {
    public static final Format[] formats = GridFormatFinder.getFormatArray();

    private CoverageStoreUtils() {}

    public static Format acquireFormat(String type) throws IOException {
        Format format = null;
        for (Format value : GridFormatFinder.getFormatArray()) {
            if (value.getName().equals(type)) {
                format = value;

                break;
            }
        }

        if (format == null) {
            throw new IOException("Cannot handle format: " + type);
        } else {
            return format;
        }
    }

    /** Utility method for finding Params */
    public static ParameterValue find(Format format, String key) {
        return find(format.getReadParameters(), key);
    }

    /** Utility methods for find param by key */
    public static ParameterValue find(ParameterValueGroup params, String key) {
        List list = params.values();
        Iterator it = list.iterator();
        ParameterDescriptor descr;
        ParameterValue val;

        while (it.hasNext()) {
            val = (ParameterValue) it.next();
            descr = val.getDescriptor();

            if (key.equalsIgnoreCase(descr.getName().toString())) {
                return val;
            }
        }

        return null;
    }

    /**
     * When loading from DTO use the params to locate factory.
     *
     * <p>bleck
     */
    public static Format aquireFactoryByType(String type) {
        final Format[] formats = GridFormatFinder.getFormatArray();
        Format format = null;

        for (Format value : formats) {
            format = value;

            if (format.getName().equals(type)) {
                return format;
            }
        }

        return null;
    }

    /** After user has selected Description can aquire Format based on description. */
    public static Format aquireFactory(String description) {
        Format[] formats = GridFormatFinder.getFormatArray();
        Format format = null;

        for (Format value : formats) {
            format = value;

            if (format.getDescription().equals(description)) {
                return format;
            }
        }

        return null;
    }

    /**
     * Returns the descriptions for the available DataFormats.
     *
     * <p>Arrrg! Put these in the select box.
     *
     * @return Descriptions for user to choose from
     */
    public static List<String> listDataFormatsDescriptions() {
        List<String> list = new ArrayList<>();
        Format[] formats = GridFormatFinder.getFormatArray();
        for (Format format : formats) {
            if (!list.contains(format.getDescription())) {
                list.add(format.getDescription());
            }
        }

        return Collections.synchronizedList(list);
    }

    public static List<Format> listDataFormats() {
        List<Format> list = new ArrayList<>();
        Format[] formats = GridFormatFinder.getFormatArray();
        for (Format format : formats) {
            if (!list.contains(format)) {
                list.add(format);
            }
        }

        return Collections.synchronizedList(list);
    }

    public static Map defaultParams(String description) {
        return Collections.synchronizedMap(defaultParams(aquireFactory(description)));
    }

    public static Map<String, Object> defaultParams(Format factory) {
        Map<String, Object> defaults = new HashMap<>();
        ParameterValueGroup params = factory.getReadParameters();

        if (params != null) {
            List list = params.values();
            Iterator it = list.iterator();
            ParameterDescriptor descr = null;
            ParameterValue val = null;
            String key;
            Object value;

            while (it.hasNext()) {
                val = (ParameterValue) it.next();
                descr = val.getDescriptor();

                key = descr.getName().toString();
                value = null;

                if (val.getValue() != null) {
                    // Required params may have nice sample values
                    //
                    if ("values_palette".equalsIgnoreCase(key)) {
                        value = val.getValue();
                    } else {
                        value = val.getValue().toString();
                    }
                }

                if (value == null) {
                    // or not
                    value = "";
                }

                if (value != null) {
                    defaults.put(key, value);
                }
            }
        }

        return Collections.synchronizedMap(defaults);
    }

    /**
     * Convert map to real values based on factory Params.
     *
     * @return Map with real values that may be acceptable to GDSFactory
     */
    public static Map<String, Object> toParams(GridFormatFactorySpi factory, Map<String, ?> params)
            throws IOException {
        final Map<String, Object> map = new HashMap<>(params.size());

        final ParameterValueGroup info = factory.createFormat().getReadParameters();
        // Convert Params into the kind of Map we actually need
        for (String key : params.keySet()) {
            Object value = find(info, key).getValue();
            if (value != null) {
                map.put(key, value);
            }
        }

        return Collections.synchronizedMap(map);
    }

    /** Retrieve a WGS84 lon,lat envelope from the provided one. */
    public static GeneralEnvelope getWGS84LonLatEnvelope(final GeneralEnvelope envelope)
            throws IndexOutOfBoundsException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = envelope.getCoordinateReferenceSystem();

        ////
        //
        // Do we need to transform?
        //
        ////
        if (CRS.equalsIgnoreMetadata(sourceCRS, DefaultGeographicCRS.WGS84)) {
            return new GeneralEnvelope(envelope);
        }

        ////
        //
        // transform
        //
        ////
        final CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        final GeneralEnvelope targetEnvelope;
        if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            targetEnvelope = CRS.transform(envelope, targetCRS);
        } else {
            targetEnvelope = new GeneralEnvelope(envelope);
        }

        targetEnvelope.setCoordinateReferenceSystem(targetCRS);

        return targetEnvelope;
    }
}
