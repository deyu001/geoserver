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

package org.geoserver.wfs.xml.v1_0_0;

import java.math.BigInteger;
import javax.xml.namespace.QName;
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.LockType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:LockFeatureType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="LockFeatureType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              This type defines the LockFeature operation.  The LockFeature
 *              element contains one or more Lock elements that define
 *              which features of a particular type should be locked.  A lock
 *              identifier (lockId) is returned to the client application which
 *              can be used by subsequent operations to reference the locked
 *              features.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element maxOccurs="unbounded" name="Lock" type="wfs:LockType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The lock element is used to indicate which feature
 *                    instances of particular type are to be locked.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute fixed="1.0.0" name="version" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute fixed="WFS" name="service" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute name="expiry" type="xsd:positiveInteger" use="optional"/&gt;
 *      &lt;xsd:attribute name="lockAction" type="wfs:AllSomeType" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The lockAction attribute is used to indicate what
 *                 a Web Feature Service should do when it encounters
 *                 a feature instance that has already been locked by
 *                 another client application.
 *
 *                 Valid values are ALL or SOME.
 *
 *                 ALL means that the Web Feature Service must acquire
 *                 locks on all the requested feature instances.  If it
 *                 cannot acquire those locks then the request should
 *                 fail.  In this instance, all locks acquired by the
 *                 operation should be released.
 *
 *                 SOME means that the Web Feature Service should lock
 *                 as many of the requested features as it can.
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class LockFeatureTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public LockFeatureTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.LOCKFEATURETYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class<?> getType() {
        return LockFeatureType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @SuppressWarnings("unchecked") // EMF model not having generics
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        LockFeatureType lockFeature = wfsfactory.createLockFeatureType();

        // &lt;xsd:element maxOccurs="unbounded" name="Lock" type="wfs:LockType"&gt;
        lockFeature.getLock().addAll(node.getChildValues(LockType.class));

        // &lt;xsd:attribute fixed="1.0.0" name="version" type="xsd:string" use="required"/&gt;
        // &lt;xsd:attribute fixed="WFS" name="service" type="xsd:string" use="required"/&gt;
        WFSBindingUtils.version(lockFeature, node);
        WFSBindingUtils.service(lockFeature, node);

        // &lt;xsd:attribute name="expiry" type="xsd:positiveInteger" use="optional"/&gt;
        if (node.hasAttribute("expiry")) {
            lockFeature.setExpiry(
                    BigInteger.valueOf(((Number) node.getAttributeValue("expiry")).longValue()));
        }

        // &lt;xsd:attribute name="lockAction" type="wfs:AllSomeType" use="optional"&gt;
        if (node.hasAttribute(AllSomeType.class)) {
            lockFeature.setLockAction((AllSomeType) node.getAttributeValue(AllSomeType.class));
        }

        return lockFeature;
    }
}
