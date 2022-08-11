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

package org.geoserver.wcs.responses;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geotools.util.DateRange;

/** Returns a GHRSST custom encoder if the configuration has GHRSST encoding enabled */
public class GHRSSTEncoderFactory implements NetCDFEncoderFactory {

    @Override
    public NetCDFEncoder getEncoderFor(
            GranuleStack granuleStack,
            File file,
            Map<String, String> encodingParameters,
            String outputFormat)
            throws IOException {
        NetCDFLayerSettingsContainer settings = NetCDFEncoder.getSettings(encodingParameters);
        if (settings != null
                && Boolean.TRUE.equals(
                        settings.getMetadata().get(GHRSSTEncoder.SETTINGS_KEY, Boolean.class))) {
            return new GHRSSTEncoder(granuleStack, file, encodingParameters, outputFormat);
        }

        // if no GHRSST settings, or disabled, then look for some other encoder
        return null;
    }

    @Override
    public String getOutputFileName(GranuleStack granuleStack, String coverageId, String format) {
        // is the layer configured to have a GHRSST format?
        NetCDFLayerSettingsContainer settings = NetCDFEncoder.getSettings(coverageId);
        MetadataMap metadata = settings.getMetadata();
        if (settings == null
                || !Boolean.TRUE.equals(metadata.get(GHRSSTEncoder.SETTINGS_KEY, Boolean.class))) {
            // nope;
            return null;
        }

        // grab reference date/time
        NetCDFDimensionsManager dimensionsManager = new NetCDFDimensionsManager();
        dimensionsManager.collectCoverageDimensions(granuleStack);
        Date referenceDate = null;
        for (NetCDFDimensionsManager.NetCDFDimensionMapping dimension :
                dimensionsManager.getDimensions()) {
            if ("time".equalsIgnoreCase(dimension.getName())) {
                TreeSet<Object> values =
                        (TreeSet<Object>) dimension.getDimensionValues().getValues();
                Object first = values.first();
                if (first instanceof Date) {
                    referenceDate = (Date) first;
                } else if (first instanceof DateRange) {
                    referenceDate = ((DateRange) first).getMinValue();
                } else {
                    throw new IllegalArgumentException(
                            "Unrecognized data type for reference date: " + first);
                }
            }
        }

        if (referenceDate == null) {
            throw new IllegalArgumentException(
                    "Could not locate a reference date in the input data, a GHRSST file "
                            + "should have a time dimension");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
        String formattedDate = dateFormat.format(referenceDate);

        // yessir, the pattern is
        // <Indicative Date><Indicative Time>-<RDAC>-<Processing Level>_GHRSST-<SST Type>-
        // <Product String>-<Additional Segregator>-v<GDS Version>-fv<File Version>.<File Type>
        // The default values in the following are setup to build a valid filename using a specific
        // case,
        // unfortunately there are not valid entries for unkonwns
        StringBuilder sb = new StringBuilder();
        sb.append(formattedDate);
        sb.append("-");
        sb.append(getConfiguration(metadata, GHRSSTEncoder.SETTINGS_RDAC_KEY, "EUR"));
        sb.append("-");
        sb.append(getConfiguration(metadata, GHRSSTEncoder.SETTINGS_PROCESSING_LEVEL_KEY, "L3U"));
        sb.append("_GHRSST-");
        sb.append(getConfiguration(metadata, GHRSSTEncoder.SETTINGS_SST_TYPE, "SSTint"));
        sb.append("-");
        sb.append(
                getConfiguration(metadata, GHRSSTEncoder.SETTINGS_PRODUCT_STRING, "AVHRR_METOP_A"));
        // additional segregator is optional, not needed here
        sb.append("-v02.0"); // GHRSST specification version
        sb.append("-fv01.0.nc");

        return sb.toString();
    }

    private String getConfiguration(MetadataMap metadata, String key, String defaultValue) {
        String value = metadata.get(key, String.class);
        return value == null ? defaultValue : value;
    }
}
