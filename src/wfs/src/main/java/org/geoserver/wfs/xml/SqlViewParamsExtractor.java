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

import java.util.List;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.ViewParamsKvpParser;
import org.geoserver.wfs.xml.v1_0_0.WFSBindingUtils;
import org.geotools.xsd.Node;

/** Static methods for accessing the ViewParams KVP parser. */
public class SqlViewParamsExtractor {

    /** Fully setup KVP parser for viewParams. Injected by spring at runtime */
    private static ViewParamsKvpParser wfsSqlViewKvpParser = null;

    public static ViewParamsKvpParser getWfsSqlViewKvpParser() {
        return wfsSqlViewKvpParser;
    }

    public static void setWfsSqlViewKvpParser(ViewParamsKvpParser wfsSqlViewKvpParser) {
        SqlViewParamsExtractor.wfsSqlViewKvpParser = wfsSqlViewKvpParser;
    }

    /**
     * Fix the node object to store a parsed list of viewParams instead of a raw string. This
     * prevents the parse() method choking later on...
     */
    @SuppressWarnings("unchecked") // no generics in EMF model
    public static void fixNodeObject(Node node) throws Exception {
        List viewParams = null;
        if (node.hasAttribute("viewParams")) {
            Node viewParamsAttribute = node.getAttribute("viewParams");
            viewParams = (List) wfsSqlViewKvpParser.parse((String) viewParamsAttribute.getValue());

            EList viewParamsList = new org.eclipse.emf.common.util.BasicEList();
            viewParamsList.addAll(viewParams);

            viewParamsAttribute.setValue(viewParamsList);
        }
    }

    /** Set the viewParams in the binding class manually */
    public static void viewParams(EObject object, Node node) throws Exception {
        if (node.hasAttribute("viewParams")) {
            String rawViewParams = (String) node.getAttributeValue("viewParams");
            List viewParams = (List) wfsSqlViewKvpParser.parse(rawViewParams);
            WFSBindingUtils.set(object, "viewParams", viewParams);
        }
    }
}
