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

import javax.xml.namespace.QName;
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.NativeType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:TransactionType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="TransactionType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              The TranactionType defines the Transaction operation.  A
 *              Transaction element contains one or more Insert, Update
 *              Delete and Native elements that allow a client application
 *              to create, modify or remove feature instances from the
 *              feature repository that a Web Feature Service controls.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element minOccurs="0" ref="wfs:LockId"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    In order for a client application to operate upon locked
 *                    feature instances, the Transaction request must include
 *                    the LockId element.  The content of this element must be
 *                    the lock identifier the client application obtained from
 *                    a previous GetFeatureWithLock or LockFeature operation.
 *
 *                    If the correct lock identifier is specified the Web
 *                    Feature Service knows that the client application may
 *                    operate upon the locked feature instances.
 *
 *                    No LockId element needs to be specified to operate upon
 *                    unlocked features.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:choice maxOccurs="unbounded" minOccurs="0"&gt;
 *              &lt;xsd:element ref="wfs:Insert"/&gt;
 *              &lt;xsd:element ref="wfs:Update"/&gt;
 *              &lt;xsd:element ref="wfs:Delete"/&gt;
 *              &lt;xsd:element ref="wfs:Native"/&gt;
 *          &lt;/xsd:choice&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute fixed="1.0.0" name="version" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute fixed="WFS" name="service" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"/&gt;
 *      &lt;xsd:attribute name="releaseAction" type="wfs:AllSomeType" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The releaseAction attribute is used to control how a Web
 *                 Feature service releases locks on feature instances after
 *                 a Transaction request has been processed.
 *
 *                 Valid values are ALL or SOME.
 *
 *                 A value of ALL means that the Web Feature Service should
 *                 release the locks of all feature instances locked with the
 *                 specified lockId, regardless or whether or not the features
 *                 were actually modified.
 *
 *                 A value of SOME means that the Web Feature Service will
 *                 only release the locks held on feature instances that
 *                 were actually operated upon by the transaction.  The lockId
 *                 that the client application obtained shall remain valid and
 *                 the other, unmodified, feature instances shall remain locked.
 *                 If the expiry attribute was specified in the original operation
 *                 that locked the feature instances, then the expiry counter
 *                 will be reset to give the client application that same amount
 *                 of time to post subsequent transactions against the locked
 *                 features.
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
public class TransactionTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;

    public TransactionTypeBinding(WfsFactory wfsfactory) {
        super(wfsfactory);
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.TRANSACTIONTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return TransactionType.class;
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
        TransactionType transaction = wfsfactory.createTransactionType();

        // lock id
        if (node.hasChild("LockId")) {
            transaction.setLockId((String) node.getChildValue("LockId"));
        }

        // transactions, need to maintain order
        for (Node child : node.getChildren()) {
            Object cv = child.getValue();

            if (cv instanceof InsertElementType) {
                transaction.getInsert().add(cv);
            } else if (cv instanceof UpdateElementType) {
                transaction.getUpdate().add(cv);
            } else if (cv instanceof DeleteElementType) {
                transaction.getDelete().add(cv);
            } else if (cv instanceof NativeType) {
                transaction.getNative().add(cv);
            }
        }

        // service + version
        WFSBindingUtils.service(transaction, node);
        WFSBindingUtils.version(transaction, node);

        // handle
        if (node.hasAttribute("handle")) {
            transaction.setHandle((String) node.getAttributeValue("handle"));
        }

        // release action
        if (node.hasAttribute(AllSomeType.class)) {
            transaction.setReleaseAction((AllSomeType) node.getAttributeValue(AllSomeType.class));
        }

        return transaction;
    }
}
