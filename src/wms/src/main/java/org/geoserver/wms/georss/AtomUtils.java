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

package org.geoserver.wms.georss;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.map.Layer;
import org.opengis.feature.simple.SimpleFeature;

/**
 * The AtomUtils class provides some static methods useful in producing atom metadata related to
 * GeoServer features.
 *
 * @author David Winslow
 */
public final class AtomUtils {

    /** A number formatting object to format the the timezone offset info in RFC3339 output. */
    private static NumberFormat doubleDigit = new DecimalFormat("00");

    /** A FeatureTemplate used for formatting feature info. @TODO: Are these things threadsafe? */
    private static FeatureTemplate featureTemplate = new FeatureTemplate();

    /** This is a utility class so don't allow instantiation. */
    private AtomUtils() {
        /* Nothing to do */
    }

    /**
     * Format dates as specified in rfc3339 (required for Atom dates)
     *
     * @param d the Date to be formatted
     * @return the formatted date
     */
    public static String dateToRFC3339(Date d) {
        StringBuilder result = new StringBuilder(formatRFC3339(d));
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        cal.setTimeZone(TimeZone.getDefault());
        int offset_millis = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        int offset_hours = Math.abs(offset_millis / (1000 * 60 * 60));
        int offset_minutes = Math.abs((offset_millis / (1000 * 60)) % 60);

        if (offset_millis == 0) {
            result.append("Z");
        } else {
            result.append((offset_millis > 0) ? "+" : "-")
                    .append(doubleDigit.format(offset_hours))
                    .append(":")
                    .append(doubleDigit.format(offset_minutes));
        }

        return result.toString();
    }

    /**
     * A date formatting object that does most of the formatting work for RFC3339. Note that since
     * Java's SimpleDateFormat does not provide all the facilities needed for RFC3339 there is still
     * some custom code to finish the job.
     */
    private static String formatRFC3339(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(d);
    }

    // TODO: use an html based output format
    public static String getEntryURL(WMS wms, SimpleFeature feature, WMSMapContent context) {
        try {
            return featureTemplate.link(feature);
        } catch (IOException ioe) {
            String nsUri = feature.getType().getName().getNamespaceURI();
            String nsPrefix = wms.getNameSpacePrefix(nsUri);

            HashMap<String, String> params = new HashMap<>();
            params.put("format", "application/atom+xml");
            params.put("layers", nsPrefix + ":" + feature.getType().getTypeName());
            params.put("featureid", feature.getID());

            return ResponseUtils.buildURL(
                    context.getRequest().getBaseUrl(), "wms/reflect", params, URLType.SERVICE);
        }
    }

    public static String getEntryURI(WMS wms, SimpleFeature feature, WMSMapContent context) {
        return getEntryURL(wms, feature, context);
    }

    public static String getFeatureTitle(SimpleFeature feature) {
        try {
            return featureTemplate.title(feature);
        } catch (IOException ioe) {
            return feature.getID();
        }
    }

    public static String getFeatureDescription(SimpleFeature feature) {
        try {
            return featureTemplate.description(feature);
        } catch (IOException ioe) {
            return feature.getID();
        }
    }

    public static String getFeedURL(WMSMapContent context) {
        return WMSRequests.getGetMapUrl(context.getRequest(), null, 0, null, null)
                .replace(' ', '+');
    }

    public static String getFeedURI(WMSMapContent context) {
        return getFeedURL(context);
    }

    public static String getFeedTitle(WMSMapContent context) {
        StringBuffer title = new StringBuffer();
        for (Layer layer : context.layers()) {
            title.append(layer.getTitle()).append(",");
        }
        title.setLength(title.length() - 1);
        return title.toString();
    }

    public static String getFeedDescription(WMSMapContent context) {
        StringBuffer description = new StringBuffer();
        for (Layer layer : context.layers()) {
            description.append(layer.getUserData().get("abstract")).append("\n");
        }
        if (description.length() == 0) {
            return "Auto-generated by geoserver";
        } else {
            description.setLength(description.length() - 1);
            return description.toString();
        }
    }
}
