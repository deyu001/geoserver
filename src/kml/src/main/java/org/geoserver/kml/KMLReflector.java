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

package org.geoserver.kml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSRequests;
import org.geoserver.wms.WebMapService;

/**
 * KML reflecting service.
 *
 * <p>This
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class KMLReflector {

    /** default 'format' value */
    public static final String FORMAT = KMLMapOutputFormat.MIME_TYPE;

    private static Map<String, Map<String, Object>> MODES;

    /** Default ground overlay size */
    private static int DEFAULT_OVERLAY_SIZE;

    static {
        Map<String, Map<String, Object>> temp = new HashMap<>();
        Map<String, Object> options;

        options = new HashMap<>();
        options.put("superoverlay", true);
        options.put("mode", "superoverlay");
        temp.put("superoverlay", options);

        options = new HashMap<>();
        options.put("superoverlay", false);
        options.put("kmscore", 100); // download -> really download vectors
        options.put("mode", "download");
        temp.put("download", options);

        options = new HashMap<>();
        options.put("superoverlay", false);
        options.put("mode", "refresh");
        temp.put("refresh", options);

        MODES = temp;

        DEFAULT_OVERLAY_SIZE = Integer.getInteger("org.geoserver.kml.defaultOverlaySize", 2048);
    }

    /** web map service */
    WebMapService wms;

    /** The WMS configuration */
    WMS wmsConfiguration;

    public KMLReflector(WebMapService wms, WMS wmsConfiguration) {
        this.wms = wms;
        this.wmsConfiguration = wmsConfiguration;
    }

    // public void wms(GetMapRequest request, HttpServletResponse response) throws Exception {
    // doWms(request, response, wms, wmsConfiguration);
    // }

    public static org.geoserver.wms.WebMap doWms(
            GetMapRequest request, WebMapService wms, WMS wmsConfiguration) throws Exception {
        // set the content disposition
        StringBuffer filename = new StringBuffer();
        boolean containsRasterData = false;
        boolean isRegionatingFriendly = true;
        for (int i = 0; i < request.getLayers().size(); i++) {
            MapLayerInfo layer = request.getLayers().get(i);
            String name = layer.getName();

            containsRasterData =
                    containsRasterData || (layer.getType() == MapLayerInfo.TYPE_RASTER);

            if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                isRegionatingFriendly =
                        isRegionatingFriendly
                                && layer.getFeature()
                                        .getFeatureSource(null, null)
                                        .getQueryCapabilities()
                                        .isReliableFIDSupported();
            } else if (layer.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
                isRegionatingFriendly =
                        isRegionatingFriendly
                                && layer.getRemoteFeatureSource()
                                        .getQueryCapabilities()
                                        .isReliableFIDSupported();
            }

            // strip off prefix
            int j = name.indexOf(':');
            if (j > -1) {
                name = name.substring(j + 1);
            }

            filename.append(name + "_");
        }

        // setup the default mode
        Map<String, String> rawKvp = request.getRawKvp();
        String mode =
                KvpUtils.caseInsensitiveParam(
                        rawKvp, "mode", wmsConfiguration.getKmlReflectorMode());

        if (!MODES.containsKey(mode)) {
            throw new ServiceException("Unknown KML mode: " + mode);
        }

        Map<String, Object> modeOptions = new HashMap<>(MODES.get(mode));

        if ("superoverlay".equals(mode)) {
            String submode =
                    KvpUtils.caseInsensitiveParam(
                            request.getRawKvp(),
                            "superoverlay_mode",
                            wmsConfiguration.getKmlSuperoverlayMode());

            if ("raster".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "raster");
            } else if ("overview".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "overview");
            } else if ("hybrid".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "hybrid");
            } else if ("auto".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "auto");
            } else if ("cached".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "cached");
            } else {
                throw new ServiceException("Unknown overlay mode: " + submode);
            }
        }

        // first set up some of the normal wms defaults
        Map<String, Object> fo = request.getFormatOptions();
        boolean refreshMode = mode.equals("refresh");
        if (request.getWidth() < 1) {
            request.setWidth(refreshMode || containsRasterData ? DEFAULT_OVERLAY_SIZE : 256);
            fo.put("autofit", "true");
        }

        if (request.getHeight() < 1) {
            request.setHeight(refreshMode || containsRasterData ? DEFAULT_OVERLAY_SIZE : 256);
            fo.put("autofit", "true");
        }

        // Force srs to lat/lon for KML output.
        request.setSRS("EPSG:4326");

        // set rest of the wms defaults
        request = DefaultWebMapService.autoSetMissingProperties(request);

        // grab the format options
        // merge the direct params that people can add in the kml reflector call
        organizeFormatOptionsParams(request.getRawKvp(), fo);
        // fill in the blanks with some defaults based on the current mode
        mergeDefaults(fo, modeOptions);

        if (fo.get("kmattr") == null) {
            fo.put("kmattr", wmsConfiguration.getKmlKmAttr());
        }
        if (fo.get("kmscore") == null) {
            fo.put("kmscore", wmsConfiguration.getKmScore());
        }
        if (fo.get("kmplacemark") == null) {
            fo.put("kmplacemark", wmsConfiguration.getKmlPlacemark());
        }

        // set the format
        // TODO: create a subclass of GetMapRequest to store these values

        Boolean superoverlay = (Boolean) fo.get("superoverlay");
        if (superoverlay == null) {
            superoverlay = Boolean.FALSE;
        }
        if (superoverlay || refreshMode || containsRasterData) {
            request.setFormat(NetworkLinkMapOutputFormat.KML_MIME_TYPE);
        } else if (!Arrays.asList(KMZMapOutputFormat.OUTPUT_FORMATS)
                .contains(request.getFormat())) {
            request.setFormat(KMLMapOutputFormat.MIME_TYPE);
        }

        org.geoserver.wms.WebMap wmsResponse = wms.getMap(request);

        return wmsResponse;
    }

    private static void mergeDefaults(Map<String, Object> fo, Map<String, Object> defaults) {
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (fo.get(entry.getKey()) == null) {
                fo.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Copy all the format_options parameters from the kvp map and put them into the formatOptions
     * map. If a parameter is already present in formatOption map it will be preserved.
     */
    public static void organizeFormatOptionsParams(
            Map<String, String> kvp, Map<String, Object> formatOptions) throws Exception {
        WMSRequests.mergeEntry(kvp, formatOptions, "legend");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmscore");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmattr");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmltitle");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmlrefresh");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmlvisible");
        WMSRequests.mergeEntry(kvp, formatOptions, "extendeddata");
        WMSRequests.mergeEntry(kvp, formatOptions, "extrude");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmplacemark");
        WMSRequests.mergeEntry(kvp, formatOptions, "superoverlay_mode");
        WMSRequests.mergeEntry(kvp, formatOptions, "overlay_mode");
    }
}
