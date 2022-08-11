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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;

public class WMSLoader extends LegacyServiceLoader<WMSInfo> {

    static Logger LOGGER = Logging.getLogger("org.geoserver.wms");

    public Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }

    @SuppressWarnings("unchecked")
    public WMSInfo load(LegacyServicesReader reader, GeoServer geoServer) throws Exception {
        WMSInfoImpl wms = new WMSInfoImpl();
        wms.setId("wms");

        Map<String, Object> props = reader.wms();
        readCommon(wms, props, geoServer);

        WatermarkInfo wm = new WatermarkInfoImpl();
        wm.setEnabled((Boolean) props.get("globalWatermarking"));
        wm.setURL((String) props.get("globalWatermarkingURL"));
        wm.setTransparency((Integer) props.get("globalWatermarkingTransparency"));
        wm.setPosition(Position.get((Integer) props.get("globalWatermarkingPosition")));
        wms.setWatermark(wm);
        wms.setDynamicStylingDisabled(
                props.containsKey("dynamicStylingDisabled")
                        ? (Boolean) props.get("dynamicStylingDisabled")
                        : false);

        try {
            wms.setInterpolation(
                    WMSInterpolation.valueOf((String) props.get("allowInterpolation")));
        } catch (Exception e) {
            // fallback on the default value if loading failed
            wms.setInterpolation(WMSInterpolation.Nearest);
        }
        wms.getMetadata().put("svgRenderer", (Serializable) props.get("svgRenderer"));
        wms.getMetadata().put("svgAntiAlias", (Serializable) props.get("svgAntiAlias"));

        // max GetFeatureInfo search radius
        wms.setMaxBuffer((Integer) props.get("maxBuffer"));

        // max memory usage
        wms.setMaxRequestMemory((Integer) props.get("maxRequestMemory"));

        // the max rendering time
        wms.setMaxRenderingTime((Integer) props.get("maxRenderingTime"));

        // the max number of rendering errors
        wms.setMaxRenderingErrors((Integer) props.get("maxRenderingErrors"));

        // base maps
        Catalog catalog = geoServer.getCatalog();
        // ... we need access to the actual catalog, not a filtered out view of the
        // layers accessible to the current user
        if (catalog instanceof Wrapper) catalog = ((Wrapper) catalog).unwrap(Catalog.class);
        CatalogFactory factory = catalog.getFactory();

        List<Map> baseMaps = (List<Map>) props.get("BaseMapGroups");
        if (baseMaps != null) {
            O:
            for (Map baseMap : baseMaps) {
                LayerGroupInfo bm = factory.createLayerGroup();
                bm.setName((String) baseMap.get("baseMapTitle"));

                // process base map layers
                List<String> layerNames = (List) baseMap.get("baseMapLayers");
                for (String layerName : layerNames) {
                    ResourceInfo resource = null;
                    if (layerName.contains(":")) {
                        String[] qname = layerName.split(":");
                        resource =
                                catalog.getResourceByName(qname[0], qname[1], ResourceInfo.class);
                    } else {
                        resource = catalog.getResourceByName(layerName, ResourceInfo.class);
                    }

                    if (resource == null) {
                        LOGGER.warning(
                                "Ignoring layer group '"
                                        + bm.getName()
                                        + "', resource '"
                                        + layerName
                                        + "' does not exist");
                        continue O;
                    }

                    List<LayerInfo> layers = catalog.getLayers(resource);
                    if (layers.isEmpty()) {
                        LOGGER.warning(
                                "Ignoring layer group '"
                                        + bm.getName()
                                        + "', no layer found for resource '"
                                        + layerName
                                        + "'");
                        continue O;
                    }

                    bm.getLayers().add(layers.get(0));
                }

                // process base map styles
                List<String> styleNames = (List) baseMap.get("baseMapStyles");
                if (styleNames.isEmpty()) {
                    // use defaults
                    bm.getStyles().addAll(Collections.nCopies(bm.getLayers().size(), null));
                } else {
                    for (String styleName : styleNames) {
                        styleName = styleName.trim();

                        StyleInfo style = null;
                        if ("".equals(styleName)) {
                            style = null;
                        } else {
                            style = catalog.getStyleByName(styleName);
                        }
                        bm.getStyles().add(style);
                    }
                }
                bm.getMetadata().put("rawStyleList", (String) baseMap.get("rawBaseMapStyles"));

                // base map enveloper
                ReferencedEnvelope e = (ReferencedEnvelope) baseMap.get("baseMapEnvelope");
                if (e == null) {
                    e = new ReferencedEnvelope();
                    e.setToNull();
                }
                bm.setBounds(e);

                LOGGER.info("Processed layer group '" + bm.getName() + "'");
                catalog.add(bm);
            }
        }

        wms.getVersions().add(WMS.VERSION_1_1_1);
        wms.getVersions().add(WMS.VERSION_1_3_0);
        return wms;
    }
}
