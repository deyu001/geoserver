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

package org.geoserver.wms.animate;

import java.awt.image.RenderedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.RenderedImageMap;
import org.locationtech.jts.geom.Envelope;

/**
 * GIF Animated reflecting service.
 *
 * <p>This is the main entry point to easily create animations.<br>
 * The reflector is able to parse incomplete WMS GetMap requests containing at least:<br>
 *
 * <ul>
 *   <li>A multivalued request supported output format
 *   <li>An "aparam" animation parameter
 *   <li>An "avalues" list of values for the animation parameter
 * </ul>
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S., alessio.fabiani@geo-solutions.it
 * @author Andrea Aime, GeoSolutions S.A.S., andrea.aime@geo-solutions.it
 */
public class Animator {

    /** default 'format' value - used if no output format has been found on the GetMap request */
    public static final String GIF_ANIMATED_FORMAT = "image/gif;subtype=animated";

    /** web map service */
    WebMapService wms;

    /** The WMS configuration */
    WMS wmsConfiguration;

    /** The prototype Constructor */
    public Animator(WebMapService wms, WMS wmsConfiguration) {
        this.wms = wms;
        this.wmsConfiguration = wmsConfiguration;
    }

    /** Produce method. Returns the full animation WebMap request. */
    public static org.geoserver.wms.WebMap produce(
            GetMapRequest request, WebMapService wms, WMS wmsConfiguration) throws Exception {

        // initializing the catalog of frames. The method analyzes the main request looking for
        // 'aparam' and 'avalues' and initializes the list of frames to be produced.
        FrameCatalog frameCatalog = initRequestManager(request, wms, wmsConfiguration);

        if (frameCatalog == null) {
            throw new RuntimeException("Animator initialization error!");
        }

        // Setup AnimGifOUTputFormat as default if not specified
        if (request.getFormat() == null) {
            request.setFormat(GIF_ANIMATED_FORMAT);
        }

        // if we have a case of layers being the param, stick the first value into the request
        if (frameCatalog.getParameter().equalsIgnoreCase("LAYERS")) {
            List<String> layers0 =
                    Arrays.asList(
                            frameCatalog
                                    .getValues()[0]
                                    .replaceAll("\\\\,", ",")
                                    .split("\\s*,\\s*"));
            LayerParser parser = new LayerParser(wmsConfiguration);
            List<MapLayerInfo> layers =
                    parser.parseLayerInfos(
                            layers0, request.getRemoteOwsURL(), request.getRemoteOwsType());
            request.setLayers(layers);
        }

        // set rest of the wms defaults
        request = DefaultWebMapService.autoSetMissingProperties(request);

        // if we have a case of layers being the param, we should also try to get uniform
        // width and height and bbox
        if (frameCatalog.getParameter().equalsIgnoreCase("LAYERS")) {
            Envelope bbox = request.getBbox();
            request.getRawKvp()
                    .put(
                            "BBOX",
                            bbox.getMinX()
                                    + ","
                                    + request.getBbox().getMinY()
                                    + ","
                                    + request.getBbox().getMaxX()
                                    + ","
                                    + request.getBbox().getMaxY());
            request.getRawKvp().put("WIDTH", String.valueOf(request.getWidth()));
            request.getRawKvp().put("HEIGHT", String.valueOf(request.getHeight()));
        }

        // initializing the catalog visitor. This takes care of producing single
        // RenderedImages.
        FrameCatalogVisitor visitor = new FrameCatalogVisitor();
        frameCatalog.getFrames(visitor);
        RenderedImage imageList = visitor.produce(frameCatalog.getWmsConfiguration());

        // run a single getmap to get the right mime type and map context
        WebMap wmsResponse = wms.getMap(request);

        return new RenderedImageMap(
                ((RenderedImageMap) wmsResponse).getMapContext(),
                imageList,
                wmsResponse.getMimeType());
    }

    /** Initializes the Animator engine. */
    private static FrameCatalog initRequestManager(
            GetMapRequest request, WebMapService wms, WMS wmsConfiguration) {
        return new FrameCatalog(request, wms, wmsConfiguration);
    }

    /**
     * A helper that avoids duplicating the code to parse a layer
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class LayerParser extends GetMapKvpRequestReader {

        public LayerParser(WMS wmsConfiguration) {
            super(wmsConfiguration);
        }

        public List<MapLayerInfo> parseLayerInfos(
                List<String> requestedLayerNames, URL remoteOwsUrl, String remoteOwsType)
                throws Exception {
            List requestedLayerInfos =
                    super.parseLayers(requestedLayerNames, remoteOwsUrl, remoteOwsType);

            List<MapLayerInfo> layers = new ArrayList<>();
            for (Object o : requestedLayerInfos) {
                if (o instanceof LayerInfo) {
                    layers.add(new MapLayerInfo((LayerInfo) o));
                } else if (o instanceof LayerGroupInfo) {
                    for (LayerInfo l : ((LayerGroupInfo) o).layers()) {
                        layers.add(new MapLayerInfo(l));
                    }
                } else if (o instanceof MapLayerInfo) {
                    // it was a remote OWS layer, add it directly
                    layers.add((MapLayerInfo) o);
                }
            }

            return layers;
        }
    }
}
