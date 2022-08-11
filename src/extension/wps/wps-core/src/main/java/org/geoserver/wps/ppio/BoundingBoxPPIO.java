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

package org.geoserver.wps.ppio;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.Ows11Factory;
import org.geoserver.wps.WPSException;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Process parameter input / output for bounding boxes
 *
 * @author Andrea Aime, OpenGeo
 */
public class BoundingBoxPPIO extends ProcessParameterIO {

    public BoundingBoxPPIO(Class type) {
        super(type, type);
    }

    /**
     * Decodes the parameter from an external source or input stream.
     *
     * <p>This method should parse the input stream into its "internal" representation.
     *
     * @return An object of type {@link #getType()}.
     */
    public Object decode(BoundingBoxType boundingBoxType) throws Exception {
        if (boundingBoxType == null) {
            return null;
        } else {
            return toTargetType(boundingBoxType);
        }
    }

    @SuppressWarnings("unchecked") // EMF model without generics...
    private Object toTargetType(BoundingBoxType bbox) throws Exception {
        CoordinateReferenceSystem crs = null;
        if (bbox.getCrs() != null) {
            crs = CRS.decode(bbox.getCrs());
        }

        double[] lower = ordinates(bbox.getLowerCorner());
        double[] upper = ordinates(bbox.getUpperCorner());

        if (ReferencedEnvelope.class.isAssignableFrom(getType())
                || BoundingBox.class.isAssignableFrom(getType())) {
            return new ReferencedEnvelope(lower[0], upper[0], lower[1], upper[1], crs);
        } else if (Envelope.class.isAssignableFrom(getType())) {
            return new Envelope(lower[0], upper[0], lower[1], upper[1]);
        } else if (org.opengis.geometry.Envelope.class.isAssignableFrom(getType())) {
            GeneralEnvelope ge = new GeneralEnvelope(lower, upper);
            ge.setCoordinateReferenceSystem(crs);
            return ge;
        } else {
            throw new WPSException(
                    "Failed to convert from OWS 1.1 Bounding box type "
                            + "to the internal representation: "
                            + getType());
        }
    }

    double[] ordinates(List<Double> corner) {
        Double[] objects = corner.toArray(new Double[corner.size()]);
        double[] result = new double[objects.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = objects[i];
        }
        return result;
    }

    /**
     * Encodes the internal representation of the object to an XML stream.
     *
     * @param object An object of type {@link #getType()}.
     */
    public BoundingBoxType encode(Object object) throws WPSException {
        if (object == null) {
            throw new IllegalArgumentException("Cannot encode a null bounding box");
        }
        return fromTargetType(object);
    }

    BoundingBoxType fromTargetType(Object object) throws WPSException {
        Ows11Factory factory = Ows11Factory.eINSTANCE;
        BoundingBoxType bbox = factory.createBoundingBoxType();

        // basic conversion and collect the crs
        CoordinateReferenceSystem crs = null;
        if (object instanceof Envelope) {
            Envelope env = (Envelope) object;
            if (object instanceof ReferencedEnvelope) {
                ReferencedEnvelope re = (ReferencedEnvelope) object;
                crs = re.getCoordinateReferenceSystem();
            }
            bbox.setLowerCorner(Arrays.asList(env.getMinX(), env.getMinY()));
            bbox.setUpperCorner(Arrays.asList(env.getMaxX(), env.getMaxY()));
        } else if (org.opengis.geometry.Envelope.class.isAssignableFrom(getType())) {
            org.opengis.geometry.Envelope env = (org.opengis.geometry.Envelope) object;
            crs = env.getCoordinateReferenceSystem();
            bbox.setLowerCorner(doubleArrayToList(env.getLowerCorner().getCoordinate()));
            bbox.setUpperCorner(doubleArrayToList(env.getUpperCorner().getCoordinate()));
        } else {
            throw new WPSException(
                    "Failed to convert from " + object + " to an OWS 1.1 Bounding box type");
        }

        // handle the EPSG code
        if (crs != null) {
            try {
                Integer code = CRS.lookupEpsgCode(crs, false);
                if (code != null) {
                    bbox.setCrs("EPSG:" + code);
                }
            } catch (Exception e) {
                throw new WPSException("Could not lookup epsg code for " + crs, e);
            }
        }

        return bbox;
    }

    private List<Double> doubleArrayToList(double[] coordinate) {
        return Arrays.stream(coordinate).boxed().collect(Collectors.toList());
    }
}
