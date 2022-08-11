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

package org.geoserver.kml.utils;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Data object to hold the KML <a
 * href="http://code.google.com/apis/kml/documentation/kmlreference.html#lookat">lookAt<a>
 * properties as they come from the WMS GetMap's FORMAT_OPTIONS vendor specific parameter.
 *
 * <p>The following parameters are parsed at construction time and held by an instance of this
 * class:
 *
 * <ul>
 *   <li>LOOKATBBOX: {@code xmin,ymin,xmax,ymax}
 *   <li>LOOKATGEOM: {@code Geometry WKT}
 *   <li>ALTITUDE: Double
 *   <li>HEADING: Double
 *   <li>TILT: Double
 *   <li>RANGE: Double
 *   <li>ALTITUDEMODE: String literal. One of <clampToGround|relativeToGround|absolute>
 * </ul>
 *
 * All of them are optional, and {@code null} will be returned by it's matching accessor method if
 * not provided. LOOKATBBOX and LOOKATGEOM are mutually exclusive, and LOOKATBBOX takes precedence
 * over LOOKATGEOM.
 *
 * <p>Both LOOKATBBOX and LOOKATGEOM allow to define the area where the KML lookAt should be
 * pointing at. If none is provided, GeoServer will use the aggregated lat lon bounding box of all
 * the requested layers as it normally does.
 *
 * @author Gabriel Roldan
 */
public class LookAtOptions {

    private static final Logger LOGGER = Logging.getLogger(LookAtOptions.class);

    private static final GeometryFactory gfac = new GeometryFactory();

    private static final String KEY_BBOX = "LOOKATBBOX";

    private static final String KEY_LOOKAT = "LOOKATGEOM";

    private static final String KEY_ALTITUDE = "ALTITUDE";

    private static final String KEY_HEADING = "HEADING";

    private static final String KEY_TILT = "TILT";

    private static final String KEY_RANGE = "RANGE";

    private static final String KEY_ALTITUDEMODE = "ALTITUDEMODE";

    private Geometry lookAt;

    private Double altitude;

    private Double heading;

    private Double tilt;

    private Double range;

    private AltitudeMode altitudeMode;

    @SuppressWarnings("unchecked")
    public LookAtOptions() {
        this(Collections.emptyMap());
    }

    /**
     * Creates a new KMLLookAt object by parsing the vendor specific parameters out of the provided
     * map, using the properties defined in the class javadoc above as Map keys.
     */
    public LookAtOptions(final Map<String, Object> options) {

        this.lookAt = parseLookAtBBOX(options.get(KEY_BBOX));
        if (this.lookAt == null) {
            this.lookAt = parseLookAt(options.get(KEY_LOOKAT));
        }
        this.altitude = parseDouble(options.get(KEY_ALTITUDE));
        this.heading = parseDouble(options.get(KEY_HEADING));
        this.tilt = parseDouble(options.get(KEY_TILT));
        this.range = parseDouble(options.get(KEY_RANGE));
        this.altitudeMode = parseAltitudeMode(options.get(KEY_ALTITUDEMODE));
    }

    private Geometry parseLookAtBBOX(Object object) {
        Geometry bbox = null;
        if (null != object) {
            Envelope env;
            try {
                BBoxKvpParser parser = new BBoxKvpParser();
                env = (Envelope) parser.parse(String.valueOf(object));
                bbox = JTS.toGeometry(env);
            } catch (Exception e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        return bbox;
    }

    private Geometry parseLookAt(final Object object) {
        Geometry geom = null;
        if (object != null) {
            String geomWKT = String.valueOf(object);
            try {
                WKTReader2 reader = new WKTReader2(gfac);
                geom = reader.read(geomWKT);
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(
                            "Error parsing "
                                    + KEY_LOOKAT
                                    + " KML format option: "
                                    + e.getMessage()
                                    + ". Argument WKT: '"
                                    + geomWKT
                                    + "'");
                }
            }
        }
        return geom;
    }

    private Double parseDouble(final Object object) {
        Double parsed = null;
        if (object != null) {
            try {
                parsed = Double.valueOf(String.valueOf(object));
            } catch (NumberFormatException ignored) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Not a number in KML format options: '" + object + "'");
                }
            }
        }
        return parsed;
    }

    private AltitudeMode parseAltitudeMode(final Object object) {
        AltitudeMode mode = AltitudeMode.CLAMP_TO_GROUND;
        if (object != null) {
            try {
                mode = AltitudeMode.fromValue(String.valueOf(object));
            } catch (IllegalArgumentException ignore) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(
                            "Illegal value for KML format option 'altitudeMode': '"
                                    + object
                                    + "'. Expected one of "
                                    + Arrays.toString(AltitudeMode.values()));
                }
            }
        }
        return mode;
    }

    public Geometry getLookAt() {
        return lookAt;
    }

    public Double getAltitude() {
        return altitude;
    }

    public Double getHeading() {
        return heading;
    }

    public Double getTilt() {
        return tilt;
    }

    public Double getRange() {
        return range;
    }

    public AltitudeMode getAltitudeMode() {
        return altitudeMode;
    }
}
