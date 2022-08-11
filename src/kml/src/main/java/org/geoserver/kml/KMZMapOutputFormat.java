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
import org.geoserver.kml.builder.StreamingKMLBuilder;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;

/**
 * Handles a GetMap request that spects a map in KMZ format.
 *
 * <p>KMZ files are a zipped KML file. The KML file must have an emcompasing <document> or <folder>
 * element. So if you have many different placemarks or ground overlays, they all need to be
 * contained within one <document> element, then zipped up and sent off with the extension "kmz".
 *
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $
 * @author $Author: Brent Owens
 * @author Justin Deoliveira
 */
public class KMZMapOutputFormat extends AbstractMapOutputFormat {
    /** Official KMZ mime type */
    public static final String MIME_TYPE = "application/vnd.google-earth.kmz";

    public static final String NL_KMZ_MIME_TYPE =
            KMZMapOutputFormat.MIME_TYPE + ";mode=networklink";

    public static final String[] OUTPUT_FORMATS = {
        MIME_TYPE, /* NL_KMZ_MIME_TYPE , */
        "application/vnd.google-earth.kmz+xml",
        "kmz",
        "application/vnd.google-earth.kmz xml"
    };

    private WMS wms;

    private StreamingKMLBuilder builder = new StreamingKMLBuilder();

    public KMZMapOutputFormat(WMS wms) {
        super(MIME_TYPE, OUTPUT_FORMATS);
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
    public KMLMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        // initialize the kml encoding context
        KmlEncodingContext context = new KmlEncodingContext(mapContent, wms, true);

        // build the kml document
        Kml kml = builder.buildKMLDocument(context);

        // return the map
        KMLMap map = new KMLMap(mapContent, context, kml, MIME_TYPE);
        map.setContentDispositionHeader(mapContent, ".kmz");
        return map;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KMLMapOutputFormat.KML_CAPABILITIES;
    }
}
