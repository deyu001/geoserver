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

import javax.xml.namespace.QName;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:GetCapabilitiesType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="GetCapabilitiesType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *            Request to a WFS to perform the GetCapabilities operation.
 *            This operation allows a client to retrieve a Capabilities
 *            XML document providing metadata for the specific WFS server.
 *
 *            The GetCapapbilities element is used to request that a Web Feature
 *            Service generate an XML document describing the organization
 *            providing the service, the WFS operations that the service
 *            supports, a list of feature types that the service can operate
 *            on and list of filtering capabilities that the service support.
 *            Such an XML document is called a capabilities document.
 *         &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="ows:GetCapabilitiesType"&gt;
 *              &lt;xsd:attribute default="WFS" name="service"
 *                  type="ows:ServiceType" use="optional"/&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class GetCapabilitiesTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public GetCapabilitiesTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    public int getExecutionMode() {
        return AFTER;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.GETCAPABILITIESTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetCapabilitiesType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        net.opengis.ows10.GetCapabilitiesType owsGetCapabilities =
                (net.opengis.ows10.GetCapabilitiesType) value;
        GetCapabilitiesType getCapabilities = wfsfactory.createGetCapabilitiesType();

        getCapabilities.setAcceptFormats(owsGetCapabilities.getAcceptFormats());
        getCapabilities.setAcceptVersions(owsGetCapabilities.getAcceptVersions());
        getCapabilities.setSections(owsGetCapabilities.getSections());
        getCapabilities.setUpdateSequence(owsGetCapabilities.getUpdateSequence());

        if (node.hasAttribute("service")) {
            getCapabilities.setService((String) node.getAttributeValue("service"));
        }

        return getCapabilities;
    }
}
