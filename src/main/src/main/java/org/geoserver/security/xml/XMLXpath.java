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

package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLConstants.NSP_RR;
import static org.geoserver.security.xml.XMLConstants.NSP_UR;
import static org.geoserver.security.xml.XMLConstants.NS_RR;
import static org.geoserver.security.xml.XMLConstants.NS_UR;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * This class is a base class for concrete implemenations
 *
 * <p>The main purpose is to serve as registry of precompiled {@link XPathExpression} objects
 *
 * @author christian
 */
public abstract class XMLXpath {

    /**
     * Inner class providing a {@link NamespaceContext} implementation
     *
     * @author christian
     */
    @SuppressWarnings("unchecked") // Java 8 and Java 11 interfaces differ
    public class NamespaceContextImpl implements NamespaceContext {
        private Map<String, String> prefix_ns_Map = new HashMap<>();
        private Map<String, String> ns_prefix_Map = new HashMap<>();

        public String getNamespaceURI(String prefix) {
            return prefix_ns_Map.get(prefix);
        }

        public String getPrefix(String namespaceURI) {
            return ns_prefix_Map.get(namespaceURI);
        }

        public Iterator getPrefixes(String namespaceURI) {
            return prefix_ns_Map.keySet().iterator();
        }

        public void register(String prefix, String ns) {
            prefix_ns_Map.put(prefix, ns);
            ns_prefix_Map.put(ns, prefix);
        }
    }

    /** XML name space context for user/group store */
    protected NamespaceContextImpl urContext;
    /** XML name space context for role store */
    protected NamespaceContextImpl rrContext;

    protected XMLXpath() {

        urContext = new NamespaceContextImpl();
        urContext.register(NSP_UR, NS_UR);

        rrContext = new NamespaceContextImpl();
        rrContext.register(NSP_RR, NS_RR);
    }

    /** Compile XPath Strings to {@link XPathExpression} */
    protected XPathExpression compile(XPath xpath, String expression) {
        try {
            return xpath.compile(expression);
        } catch (XPathExpressionException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    /** Creates a relatvie XPathExpression for a XML attribute, needs name space prefix */
    protected XPathExpression compileRelativeAttribute(
            XPath xpath, String attrName, String prefix) {
        // return compile(xpath,"@"+prefix+":"+attrName);
        return compile(xpath, "@" + attrName);
    }
}
