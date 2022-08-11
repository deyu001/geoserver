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

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.kml.builder.SimpleNetworkLinkBuilder;
import org.geoserver.kml.builder.SuperOverlayNetworkLinkBuilder;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geotools.util.logging.Logging;

/**
 * TODO: - handle superoverlay and caching
 *
 * @author Andrea Aime - GeoSolutions
 */
public class NetworkLinkMapOutputFormat extends AbstractMapOutputFormat {
    static final Logger LOGGER = Logging.getLogger(NetworkLinkMapOutputFormat.class);

    /** Official KMZ mime type, tweaked to output NetworkLink */
    public static final String KML_MIME_TYPE = KMLMapOutputFormat.MIME_TYPE + ";mode=networklink";

    public static final String KMZ_MIME_TYPE = KMZMapOutputFormat.MIME_TYPE + ";mode=networklink";

    public static final String[] OUTPUT_FORMATS = {KML_MIME_TYPE, KMZ_MIME_TYPE};

    private WMS wms;

    public NetworkLinkMapOutputFormat(WMS wms) {
        super(KML_MIME_TYPE, OUTPUT_FORMATS);
        this.wms = wms;
    }

    /**
     * Initializes the KML encoder. None of the map production is done here, it is done in
     * writeTo(). This way the output can be streamed directly to the output response and not
     * written to disk first, then loaded in and then sent to the response.
     *
     * @param mapContent WMSMapContext describing what layers, styles, area of interest etc are to
     *     be used when producing the map.
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    @SuppressWarnings("rawtypes")
    public KMLMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        GetMapRequest request = mapContent.getRequest();

        // restore normal kml types (no network link mode)
        boolean kmz = false;
        if (NetworkLinkMapOutputFormat.KML_MIME_TYPE.equals(request.getFormat())) {
            request.setFormat(KMLMapOutputFormat.MIME_TYPE);
        } else {
            kmz = true;
            request.setFormat(KMZMapOutputFormat.MIME_TYPE);
        }

        // check the superoverlay modes
        Boolean superoverlay = (Boolean) request.getFormatOptions().get("superoverlay");
        if (superoverlay == null) {
            superoverlay = Boolean.FALSE;
        }

        // build the kml according to the building mode
        Kml kml = null;
        KmlEncodingContext context = new KmlEncodingContext(mapContent, wms, kmz);
        if (superoverlay) {
            kml = new SuperOverlayNetworkLinkBuilder(context).buildKMLDocument();
        } else {
            kml = new SimpleNetworkLinkBuilder(context).buildKMLDocument();
        }

        // build the output map
        String mime = kmz ? KMZMapOutputFormat.MIME_TYPE : KMLMapOutputFormat.MIME_TYPE;
        KMLMap map = new KMLMap(mapContent, null, kml, mime);
        map.setContentDispositionHeader(mapContent, kmz ? ".kmz" : ".kml");
        return map;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KMLMapOutputFormat.KML_CAPABILITIES;
    }
}
