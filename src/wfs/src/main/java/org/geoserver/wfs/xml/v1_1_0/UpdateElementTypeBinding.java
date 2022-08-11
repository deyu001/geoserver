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

package org.geoserver.wfs.xml.v1_1_0;

import java.net.URI;
import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.wfs.WFSException;
import org.geotools.gml2.bindings.GML2ParsingUtils;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;

/**
 * Binding object for the type http://www.opengis.net/wfs:UpdateElementType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="UpdateElementType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element maxOccurs="unbounded" ref="wfs:Property"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    Changing or updating a feature instance means that
 *                    the current value of one or more properties of
 *                    the feature are replaced with new values.  The Update
 *                    element contains  one or more Property elements.  A
 *                    Property element contains the name or a feature property
 *                    who's value is to be changed and the replacement value
 *                    for that property.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The Filter element is used to constrain the scope
 *                    of the update operation to those features identified
 *                    by the filter.  Feature instances can be specified
 *                    explicitly and individually using the identifier of
 *                    each feature instance OR a set of features to be
 *                    operated on can be identified by specifying spatial
 *                    and non-spatial constraints in the filter.
 *                    If no filter is specified then update operation
 *                    applies to all feature instances.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The handle attribute allows a client application
 *                 to assign a client-generated request identifier
 *                 to an Insert action.  The handle is included to
 *                 facilitate error reporting.  If an Update action
 *                 in a Transaction request fails, then a WFS may
 *                 include the handle in an exception report to localize
 *                 the error.  If no handle is included of the offending
 *                 Insert element then a WFS may employee other means of
 *                 localizing the error (e.g. line number).
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="typeName" type="xsd:QName" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                The value of the typeName attribute is the name
 *                of the feature type to be updated. The name
 *                specified must be a valid type that belongs to
 *                the feature content as defined by the GML
 *                Application Schema.
 *             &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute default="x-application/gml:3" name="inputFormat"
 *          type="xsd:string" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 This inputFormat attribute is used to indicate
 *                 the format used to encode a feature instance in
 *                 an Insert element.  The default value of
 *                 'text/xml; subtype=gml/3.1.1' is used to indicate
 *                 that feature encoding is GML3.  Another example
 *                 might be 'text/xml; subtype=gml/2.1.2' indicating
 *                 that the feature us encoded in GML2.  A WFS must
 *                 declare in the capabilities document, using a
 *                 Parameter element, which version of GML it supports.
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 DO WE NEED THIS HERE?
 *             &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class UpdateElementTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;

    public UpdateElementTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.UPDATEELEMENTTYPE;
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

    public void initializeChildContext(
            ElementInstance childInstance, Node node, MutablePicoContainer context) {
        // if an srsName is set for this geometry, put it in the context for
        // children, so they can use it as well
        if (node.hasAttribute("srsName")) {
            try {
                CoordinateReferenceSystem crs = GML2ParsingUtils.crs(node);
                if (crs != null) {
                    context.registerComponentInstance(CoordinateReferenceSystem.class, crs);
                }
            } catch (Exception e) {
                throw new WFSException(e, "InvalidParameterValue");
            }
        }
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @SuppressWarnings("unchecked") // EMF model without generics
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        UpdateElementType updateElement = wfsfactory.createUpdateElementType();

        // &lt;xsd:element maxOccurs="unbounded" ref="wfs:Property"&gt;
        updateElement.getProperty().addAll(node.getChildValues(PropertyType.class));

        // &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter"&gt;
        updateElement.setFilter(node.getChildValue(Filter.class));

        // &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("handle")) {
            updateElement.setHandle((String) node.getAttributeValue("handle"));
        }

        // &lt;xsd:attribute name="typeName" type="xsd:QName" use="required"&gt;
        updateElement.setTypeName((QName) node.getAttributeValue("typeName"));

        // &lt;xsd:attribute default="x-application/gml:3" name="inputFormat"
        //	type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("inputFormat")) {
            updateElement.setInputFormat((String) node.getAttributeValue("inputFormat"));
        }

        // &lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
        if (node.hasAttribute("srsName")) {
            updateElement.setSrsName((URI) node.getAttributeValue("srsName"));
        }

        return updateElement;
    }

    public Object getProperty(Object arg0, QName arg1) throws Exception {
        Object result = super.getProperty(arg0, arg1);

        // System.out.println("Being asked for " + arg1);
        // System.out.println("Returning " + result);
        return result;
    }
}
