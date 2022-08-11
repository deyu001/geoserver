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

package org.geoserver.mbtiles;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.demo.MapPreviewPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.junit.Test;

public class MBTilesMimeTypeTest extends GeoServerWicketTestSupport {

    @Test
    public void testMBTilesFormat() {
        // Opening the selected page
        tester.startPage(new MapPreviewPage());
        tester.assertRenderedPage(MapPreviewPage.class);
        tester.assertNoErrorMessage();

        // Getting the wms outputformats available
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "table:listContainer:items:1:itemProperties:4:component:menu:wms:wmsFormats");
        assertNotNull(component);
        assertTrue(component instanceof RepeatingView);
        // Get the list of all the format
        RepeatingView view = (RepeatingView) component;
        Iterator<? extends Component> iterator = view.iterator();
        // Check that MBTiles has been found
        boolean mbtilesFound = false;
        // Get the string for the application/x-sqlite3 mimetype
        ParamResourceModel rm =
                new ParamResourceModel("format.wms.application/x-sqlite3", null, "");
        String mbtiles = rm.getString();
        while (iterator.hasNext()) {
            Component comp = iterator.next();
            assertTrue(comp instanceof Label);
            Label lb = (Label) comp;
            String test = lb.getDefaultModelObjectAsString();
            if (test.contains(mbtiles)) {
                mbtilesFound = true;
            }
        }
        // Ensure the MBTiles string has been found
        assertTrue(mbtilesFound);
    }
}
