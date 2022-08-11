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

package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Before;
import org.w3c.dom.Document;

public class WFS20TestSupport extends WFSTestSupport {

    @Before
    public void beforeTest() {
        // deactivate GML MIME type overriding
        setGmlMimeTypeOverride(null);
    }

    /**
     * Helper method that just sets the GML 3.2 MIME type to force. If NULL is provided then GML
     * MIME type overriding will be deactivated. This changes will be applied to WFS global
     * configuration.
     */
    protected void setGmlMimeTypeOverride(String mimeTypeToForce) {
        WFSInfo info = getGeoServer().getService(WFSInfo.class);
        GMLInfo gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        gmlInfo.setMimeTypeToForce(mimeTypeToForce);
        getGeoServer().save(info);
    }

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        // override some namespaces
        namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("fes", "http://www.opengis.net/fes/2.0");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
    }

    /** @return The 2.0 service descriptor. */
    protected Service getServiceDescriptor20() {
        return (Service) GeoServerExtensions.bean("wfsService-2.0");
    }

    /** Asserts a document is valid gml 3.2 */
    protected void assertGML32(Document doc) {
        assertEquals(WFS.NAMESPACE, doc.getDocumentElement().getAttribute("xmlns:wfs"));

        String schemaLocation = doc.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertTrue(schemaLocation.contains(WFS.NAMESPACE));

        String[] parts = schemaLocation.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(WFS.NAMESPACE)) {
                assertTrue(parts[i + 1].endsWith("2.0/wfs.xsd"));
            }
        }
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }
}
