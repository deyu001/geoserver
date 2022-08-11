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

package org.geoserver.wms.map;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

/**
 * Subclass of {@link OpenLayersMapOutputFormat} allowing to explictly request a OpenLayers 3 client
 */
public class OpenLayers3MapOutputFormat extends AbstractOpenLayersMapOutputFormat {

    /** The freemarker template for OL3 */
    static final String OL3_TEMPLATE_FTL = "OpenLayers3MapTemplate.ftl";

    /** Format name for OL3 preview */
    public static final String OL3_FORMAT = "application/openlayers3";

    /** The mime type for the response header */
    public static final String MIME_TYPE = "text/html; subtype=openlayers3";

    /** The formats accepted in a GetMap request for this producer and stated in getcaps */
    private static final Set<String> OUTPUT_FORMATS =
            new HashSet<>(Arrays.asList(OL3_FORMAT, MIME_TYPE));

    public OpenLayers3MapOutputFormat(WMS wms) {
        super(wms);
    }

    @Override
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    protected String getTemplateName(WMSMapContent mapContent) {
        if (!browserSupportsOL3(mapContent)) {
            throw new ServiceException("OpenLayers 3 is not supported on the current browser");
        }
        return OL3_TEMPLATE_FTL;
    }

    protected boolean browserSupportsOL3(WMSMapContent mc) {
        String agent = mc.getRequest().getHttpRequestHeader("USER-AGENT");
        if (agent == null) {
            // play it safe
            return false;
        }

        Pattern MSIE_PATTERN = Pattern.compile(".*MSIE (\\d+)\\..*");
        Matcher matcher = MSIE_PATTERN.matcher(agent);
        if (!matcher.matches()) {
            return true;
        } else {
            return Integer.valueOf(matcher.group(1)) > 8;
        }
    }

    /**
     * OL3 does support a very limited set of unit types, we have to try and return one of those,
     * otherwise the scale won't be shown.
     */
    @Override
    protected String getUnits(WMSMapContent mapContent) {
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        // first rough approximation, meters for projected CRS, degrees for the
        // others
        String result = crs instanceof ProjectedCRS ? "m" : "degrees";
        try {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            // use the unicode escape sequence for the degree sign so its not
            // screwed up by different local encodings
            if ("ft".equals(unit) || "feets".equals(unit)) result = "feet";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }
        return result;
    }
}
