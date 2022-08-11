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

package org.geoserver.catalog;

/**
 * The AttributionInfo interface describes a data provider for attribution, such as in the WMS
 * Capabilities document.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public interface AttributionInfo extends Info {
    /**
     * Get the title field of the attribution, providing a human-readable name for the attribution.
     *
     * @return a String containing the title
     */
    String getTitle();

    /**
     * Get the href field of the attribution, indicating a link that users can follow for more
     * information about the providing organization.
     *
     * @return a String containing the href
     */
    String getHref();

    /**
     * Get the logoURL field of the attribution, providing a URL for an image representing the
     * providing organization.
     *
     * @return a String containing the logoURL
     */
    String getLogoURL();

    /**
     * Get the mimetype field of the attribution, indicating the mimetype of the logo image pointed
     * to by the logoURL.
     *
     * @return a String containing the logoType
     */
    String getLogoType();

    /**
     * Get the width field of the attribution, indicating the width of the logo image pointed to by
     * the logoURL.
     *
     * @return the logoWidth as an int
     */
    int getLogoWidth();

    /**
     * Get the height field of the attribution, indicating the height of the logo image pointed to
     * by the logoURL.
     *
     * @return the logoHeight as an int
     */
    int getLogoHeight();

    /**
     * Set the title field of the attribution, providing a human-readable name for the attribution.
     *
     * @param title a String containing the new title value
     */
    void setTitle(String title);

    /**
     * Set the href field of the attribution, indicating a link that users can follow for more
     * information about the providing organization.
     *
     * @param href a String containing the new href value
     */
    void setHref(String href);

    /**
     * Set the logoURL field of the attribution, providing a URL for an image representing the
     * providing organization.
     *
     * @param logoURL a String containing the new logoURL value
     */
    void setLogoURL(String logoURL);

    /**
     * Set the mimetype field of the attribution, indicating the mimetype of the logo image pointed
     * to by the logoURL.
     *
     * @param logoType a String containing the new logoType value
     */
    void setLogoType(String logoType);

    /**
     * Set the width field of the attribution, indicating the width of the logo image pointed to by
     * the logoURL.
     *
     * @param logoWidth the new logoWidth value
     */
    void setLogoWidth(int logoWidth);

    /**
     * Set the height field of the attribution, indicating the height of the logo image pointed to
     * by the logoURL.
     *
     * @param logoHeight the new logoHeight value
     */
    void setLogoHeight(int logoHeight);
}
