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
import net.opengis.wfs.IdentifierGenerationOptionType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.wfs.WFSException;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.gml2.bindings.GML2ParsingUtils;
import org.geotools.gml3.GML;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;

/**
 * Binding object for the type http://www.opengis.net/wfs:InsertElementType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="InsertElementType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              An Insert element may contain a feature collection or one
 *              or more feature instances to be inserted into the
 *              repository.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:choice&gt;
 *          &lt;xsd:element ref="gml:_FeatureCollection"/&gt;
 *          &lt;xsd:sequence&gt;
 *              &lt;xsd:element maxOccurs="unbounded" ref="gml:_Feature"/&gt;
 *          &lt;/xsd:sequence&gt;
 *      &lt;/xsd:choice&gt;
 *      &lt;xsd:attribute default="GenerateNew" name="idgen"
 *          type="wfs:IdentifierGenerationOptionType" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The idgen attribute control how a WFS generates identifiers
 *                 from newly created feature instances using the Insert action.
 *                 The default action is to have the WFS generate a new id for
 *                 the features.  This is also backward compatible with WFS 1.0
 *                 where the only action was for the WFS to generate an new id.
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The handle attribute allows a client application
 *                 to assign a client-generated request identifier
 *                 to an Insert action.  The handle is included to
 *                 facilitate error reporting.  If an Insert action
 *                 in a Transaction request fails, then a WFS may
 *                 include the handle in an exception report to localize
 *                 the error.  If no handle is included of the offending
 *                 Insert element then a WFS may employee other means of
 *                 localizing the error (e.g. line number).
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute default="text/xml; subtype=gml/3.1.1"
 *          name="inputFormat" type="xsd:string" use="optional"&gt;
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
 *        The optional srsName attribute is used to assert the SRS of the
 *        incoming feature data, which can be useful if the incoming feature
 *        data does not have an SRS declared for each geometry. If the
 *        srsName attribute exists on an &lt;Insert&gt; element, its value shall
 *        be equivalent to the value of &lt;DefaultSRS&gt; or any of the
 *        &lt;OtherSRS&gt; of the relevant feature types. If, however, the SRS is
 *        not supported, the WFS shall raise an exception as described in
 *        subclause 7.7. If the srsName is not specified on the &lt;Insert&gt;
 *        element, the WFS shall interpret this to mean that the feature
 *        data is given in the &lt;DefaultSRS&gt; list, except where an SRS is
 *        specified on the feature geometry. In this case, if the SRS for
 *        such a geometry is one of the &lt;DefaultSRS&gt; or &lt;OtherSR&gt; values
 *        for the respective feature types, it will be transformed as
 *        required before insertion. However, if the aforesaid SRS is not
 *        supported for the respective feature type, the entire transaction
 *        shall fail and the WFS shall raise an exception as described in
 *        subclause 7.7. If atomic transactions are not supported by the
 *        underlying DBMS, the WFS shall skip any feature with an
 *        unsupported SRS and continue
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
public class InsertElementTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;

    public InsertElementTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.INSERTELEMENTTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return InsertElementType.class;
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
    @SuppressWarnings("unchecked")
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        InsertElementType insertElement = wfsfactory.createInsertElementType();

        // &lt;xsd:choice&gt;
        //   &lt;xsd:element ref="gml:_FeatureCollection"/&gt;
        //   &lt;xsd:sequence&gt;
        //       &lt;xsd:element maxOccurs="unbounded" ref="gml:_Feature"/&gt;
        //   &lt;/xsd:sequence&gt;
        // &lt;/xsd:choice&gt;
        if (node.hasChild(FeatureCollection.class)) {
            SimpleFeatureCollection fc =
                    (SimpleFeatureCollection) node.getChildValue(FeatureCollection.class);
            insertElement.getFeature().addAll(DataUtilities.list(fc));
        } else if (node.hasChild(SimpleFeature.class)) {
            insertElement.getFeature().addAll(node.getChildValues(SimpleFeature.class));
        }

        // &lt;xsd:attribute default="GenerateNew" name="idgen"
        //		type="wfs:IdentifierGenerationOptionType" use="optional"&gt;
        if (node.hasAttribute("idgen")) {
            insertElement.setIdgen(
                    (IdentifierGenerationOptionType) node.getAttributeValue("idgen"));
        }

        // &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("handle")) {
            insertElement.setHandle((String) node.getAttributeValue("handle"));
        }

        // &lt;xsd:attribute default="text/xml; subtype=gml/3.1.1"
        //		 name="inputFormat" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("inputFormat")) {
            insertElement.setInputFormat((String) node.getAttributeValue("inputFormat"));
        }

        // &lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
        if (node.hasAttribute("srsName")) {
            insertElement.setSrsName((URI) node.getAttributeValue("srsName"));
        }

        return insertElement;
    }

    public Object getProperty(Object object, QName name) throws Exception {
        InsertElementType insert = (InsertElementType) object;

        if (GML._Feature.equals(name)) {
            return insert.getFeature();
        }

        return super.getProperty(object, name);
    }
}
