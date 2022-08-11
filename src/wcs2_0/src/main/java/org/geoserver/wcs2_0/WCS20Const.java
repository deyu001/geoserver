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

package org.geoserver.wcs2_0;

import org.geotools.ows.v2_0.OWS;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Some consts useful through the module.
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public class WCS20Const {
    public static final String V201 = "2.0.1"; // current
    public static final String V20 = "2.0.0"; // old and deprecated, but tested by CITE
    public static final String V111 = "1.1.1";
    public static final String V110 = "1.1.0";

    public static final String CUR_VERSION = WCS20Const.V201;

    public static final String SERVICE_NAME = "WCS";

    protected static final String URI_WCS = "http://www.opengis.net/wcs/2.0";

    public static final String OVERVIEW_POLICY_EXTENSION = "OverviewPolicy";

    public static final String OVERVIEW_POLICY_EXTENSION_LOWERCASE =
            OVERVIEW_POLICY_EXTENSION.toLowerCase();

    public static final String OVERVIEW_POLICY_EXTENSION_NAMESPACE =
            "http://www.geoserver.org/WCS_service-extension_overviewpolicy/1.0";

    public static AttributesImpl getDefaultNamespaces() {

        final AttributesImpl attributes = new AttributesImpl();

        attributes.addAttribute("", "xmlns:wcs", "xmlns:wcs", "", URI_WCS);
        attributes.addAttribute("", "xmlns:ows", "xmlns:ows", "", OWS.NAMESPACE);

        //            attributes.addAttribute("", "xmlns:ogc", "xmlns:ogc", "",
        // "http://www.opengis.net/ogc");
        attributes.addAttribute("", "xmlns:gml", "xmlns:gml", "", "http://www.opengis.net/gml/3.2");
        attributes.addAttribute(
                "", "xmlns:gmlcov", "xmlns:gmlcov", "", "http://www.opengis.net/gmlcov/1.0");

        attributes.addAttribute(
                "", "xmlns:xlink", "xmlns:xlink", "", "http://www.w3.org/1999/xlink");
        attributes.addAttribute(
                "", "xmlns:xsi", "xmlns:xsi", "", "http://www.w3.org/2001/XMLSchema-instance");

        return attributes;
    }
}
