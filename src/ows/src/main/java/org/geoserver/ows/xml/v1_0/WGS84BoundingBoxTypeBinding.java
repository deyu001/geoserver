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

package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;
import net.opengis.ows10.Ows10Factory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/ows:WGS84BoundingBoxType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="WGS84BoundingBoxType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;XML encoded minimum rectangular bounding box (or region) parameter, surrounding all the associated data. This box is specialized for use with the 2D WGS 84 coordinate reference system with decimal values of longitude and latitude. &lt;/documentation&gt;
 *          &lt;documentation&gt;This type is adapted from the general BoundingBoxType, with modified contents and documentation for use with the 2D WGS 84 coordinate reference system. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;complexContent&gt;
 *          &lt;restriction base="ows:BoundingBoxType"&gt;
 *              &lt;sequence&gt;
 *                  &lt;element name="LowerCorner" type="ows:PositionType2D"&gt;
 *                      &lt;annotation&gt;
 *                          &lt;documentation&gt;Position of the bounding box corner at which the values of longitude and latitude normally are the algebraic minimums within this bounding box. For more information, see Subclauses 10.4.5 and C.13. &lt;/documentation&gt;
 *                      &lt;/annotation&gt;
 *                  &lt;/element&gt;
 *                  &lt;element name="UpperCorner" type="ows:PositionType2D"&gt;
 *                      &lt;annotation&gt;
 *                          &lt;documentation&gt;Position of the bounding box corner at which the values of longitude and latitude normally are the algebraic minimums within this bounding box. For more information, see Subclauses 10.4.5 and C.13. &lt;/documentation&gt;
 *                      &lt;/annotation&gt;
 *                  &lt;/element&gt;
 *              &lt;/sequence&gt;
 *              &lt;attribute fixed="urn:ogc:def:crs:OGC:2:84" name="crs"
 *                  type="anyURI" use="optional"&gt;
 *                  &lt;annotation&gt;
 *                      &lt;documentation&gt;This attribute can be included when considered useful. When included, this attribute shall reference the 2D WGS 84 coordinate reference system with longitude before latitude and decimal values of longitude and latitude. &lt;/documentation&gt;
 *                  &lt;/annotation&gt;
 *              &lt;/attribute&gt;
 *              &lt;attribute fixed="2" name="dimensions"
 *                  type="positiveInteger" use="optional"&gt;
 *                  &lt;annotation&gt;
 *                      &lt;documentation&gt;The number of dimensions in this CRS (the length of a coordinate sequence in this use of the PositionType). This number is specified by the CRS definition, but can also be specified here. &lt;/documentation&gt;
 *                  &lt;/annotation&gt;
 *              &lt;/attribute&gt;
 *          &lt;/restriction&gt;
 *      &lt;/complexContent&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class WGS84BoundingBoxTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public WGS84BoundingBoxTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.WGS84BOUNDINGBOXTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return null;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        // TODO: implement
        return null;
    }
}
