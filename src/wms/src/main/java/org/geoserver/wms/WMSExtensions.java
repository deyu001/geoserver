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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.springframework.context.ApplicationContext;

/**
 * Utility class uses to process GeoServer WMS extension points.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class WMSExtensions {

    /** Finds out the registered GetMapOutputFormats in the application context. */
    public static List<GetMapOutputFormat> findMapProducers(final ApplicationContext context) {
        return GeoServerExtensions.extensions(GetMapOutputFormat.class, context);
    }

    /**
     * Finds out a {@link GetMapOutputFormat} specialized in generating the requested map format,
     * registered in the spring context.
     *
     * @param outputFormat a request parameter object wich holds the processed request objects, such
     *     as layers, bbox, outpu format, etc.
     * @return A specialization of <code>GetMapDelegate</code> wich can produce the requested output
     *     map format, or {@code null} if none is found
     */
    public static GetMapOutputFormat findMapProducer(
            final String outputFormat, final ApplicationContext applicationContext) {

        final Collection<GetMapOutputFormat> producers;
        producers = WMSExtensions.findMapProducers(applicationContext);

        return findMapProducer(outputFormat, producers);
    }

    /** @return {@link GetMapOutputFormat} for the requested outputFormat, or {@code null} */
    public static GetMapOutputFormat findMapProducer(
            String outputFormat, Collection<GetMapOutputFormat> producers) {

        Set<String> producerFormats;
        for (GetMapOutputFormat producer : producers) {
            producerFormats = producer.getOutputFormatNames();
            Set<String> caseInsensitiveFormats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveFormats.addAll(producerFormats);
            if (caseInsensitiveFormats.contains(outputFormat)) {
                return producer;
            }
        }
        return null;
    }

    /** @return the configured {@link GetFeatureInfoOutputFormat}s */
    public static List<GetFeatureInfoOutputFormat> findFeatureInfoFormats(
            ApplicationContext applicationContext) {
        return GeoServerExtensions.extensions(GetFeatureInfoOutputFormat.class, applicationContext);
    }

    public static GetLegendGraphicOutputFormat findLegendGraphicFormat(
            final String outputFormat, final ApplicationContext applicationContext) {

        List<GetLegendGraphicOutputFormat> formats = findLegendGraphicFormats(applicationContext);

        for (GetLegendGraphicOutputFormat format : formats) {
            if (format.getContentType().startsWith(outputFormat)) {
                return format;
            }
        }
        return null;
    }

    public static List<GetLegendGraphicOutputFormat> findLegendGraphicFormats(
            final ApplicationContext applicationContext) {
        List<GetLegendGraphicOutputFormat> formats =
                GeoServerExtensions.extensions(
                        GetLegendGraphicOutputFormat.class, applicationContext);
        return formats;
    }

    /** Looks up {@link ExtendedCapabilitiesProvider} extensions. */
    public static List<ExtendedCapabilitiesProvider> findExtendedCapabilitiesProviders(
            final ApplicationContext applicationContext) {
        return GeoServerExtensions.extensions(
                ExtendedCapabilitiesProvider.class, applicationContext);
    }

    /**
     * Looks up all the {@link RenderedImageMapResponse} registered in the Spring application
     * context
     */
    public static Collection<RenderedImageMapResponse> findMapResponses(
            ApplicationContext applicationContext) {
        return GeoServerExtensions.extensions(RenderedImageMapResponse.class, applicationContext);
    }
}
