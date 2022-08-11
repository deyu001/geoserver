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

/**
 * Configuration object for WMS water marking.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WatermarkInfo extends Serializable {

    /** The position of a watermark logo. */
    public static enum Position {
        TOP_LEFT {

            public int getCode() {
                return 0;
            }
        },
        TOP_CENTER {

            public int getCode() {
                return 1;
            }
        },
        TOP_RIGHT {

            public int getCode() {
                return 2;
            }
        },
        MID_LEFT {

            public int getCode() {
                return 3;
            }
        },
        MID_CENTER {

            public int getCode() {
                return 4;
            }
        },
        MID_RIGHT {

            public int getCode() {
                return 5;
            }
        },
        BOT_LEFT {

            public int getCode() {
                return 6;
            }
        },
        BOT_CENTER {

            public int getCode() {
                return 7;
            }
        },
        BOT_RIGHT {

            public int getCode() {
                return 8;
            }
        };

        public abstract int getCode();

        public static Position get(int code) {
            for (Position p : values()) {
                if (code == p.getCode()) {
                    return p;
                }
            }

            return null;
        }
    };

    /** Flag indicating if water marking is enabled. */
    boolean isEnabled();

    /** Sets flag indicating if water marking is enabled. */
    void setEnabled(boolean enabled);

    /**
     * The position of the watermark on resulting wms images.
     *
     * <p>
     *
     * <pre>
     * O -- O -- O      0 -- 1 -- 2
     * |    |    |      |    |    |
     * O -- O -- O  ==  3 -- 4 -- 5
     * |    |    |      |    |    |
     * O -- O -- O      6 -- 7 -- 8
     * </pre>
     */
    Position getPosition();

    /** Sets the watermark position. */
    void setPosition(Position position);

    /**
     * The url of the watermark.
     *
     * <p>This is usually the location of some logo.
     */
    String getURL();

    /** Sets the url of the watermark. */
    void setURL(String url);

    /** The transparency of the watermark logo, ranging from 0 to 255. */
    int getTransparency();

    /** Sets the transparanecy of the watermark logo. */
    void setTransparency(int transparency);
}
