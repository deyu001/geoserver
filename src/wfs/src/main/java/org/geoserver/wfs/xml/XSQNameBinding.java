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

package org.geoserver.wfs.xml;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geotools.xsd.InstanceComponent;

/**
 * Overrides the base class parsing code so that prefix can be resolved to URI's using the GeoServer
 * Data catalog as well
 *
 * @author Andrea Aime - TOPP
 */
public class XSQNameBinding extends org.geotools.xs.bindings.XSQNameBinding {

    Catalog data;

    public XSQNameBinding(NamespaceContext namespaceContext, Catalog data) {
        super(namespaceContext);
        this.data = data;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * This binding returns objects of type {@link QName}.
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value) throws Exception {

        //        QName qName = null;
        //        try {
        //            qName = DatatypeConverterImpl.getInstance()
        //                .parseQName((String) value, namespaceContext);
        //        }
        //        catch( Exception e ) {
        //        }
        //
        //        if (qName != null) {
        //            return qName;
        //        }

        if (value == null) {
            return new QName(null);
        }

        String s = (String) value;
        int i = s.indexOf(':');

        if (i != -1) {
            String prefix = s.substring(0, i);
            String local = s.substring(i + 1);

            // first match the prefix back to a uri, since the prefix might not match the one
            // used by the catalog
            String namespaceURI = namespaceContext.getNamespaceURI(prefix);
            NamespaceInfo nsInfo = null;
            if (namespaceURI != null) {
                nsInfo = data.getNamespaceByURI(namespaceURI);
            } else {
                // fall back to just looking up by prefix
                nsInfo = data.getNamespaceByPrefix(prefix);
            }

            if (nsInfo != null) {
                return new QName(nsInfo.getURI(), local, nsInfo.getPrefix());
            }

            return new QName(namespaceURI, local, prefix);
        }

        return new QName(null, s);
    }
}
