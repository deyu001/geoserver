/*
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


package org.geoserver.ysld;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleGenerator;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.StyleType;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geotools.data.DataUtilities;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

public class YsldStyleGeneratorTest {

    /**
     * Test integration of {@link YsldHandler#getStyle(org.geoserver.catalog.StyleType,
     * java.awt.Color, String, String)} with the {@link StyleGenerator} by generating a generic
     * style (from {@link YsldHandler#TEMPLATES} for {@link StyleType.GENERIC}).
     */
    @Test
    public void testYsldStyleGenerator() throws Exception {
        final YsldHandler handler = new YsldHandler();
        ResourcePool rp = createNiceMock(ResourcePool.class);
        rp.writeStyle(anyObject(), (InputStream) anyObject());
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            @SuppressWarnings("PMD.CloseResource")
                            public Void answer() throws Throwable {
                                Object[] args = getCurrentArguments();
                                InputStream is = (InputStream) args[1];
                                StyledLayerDescriptor sld = handler.parse(is, null, null, null);

                                assertEquals(1, sld.getStyledLayers().length);

                                NamedLayer nl = (NamedLayer) sld.getStyledLayers()[0];
                                assertEquals(1, nl.getStyles().length);

                                Style style = nl.getStyles()[0];
                                assertEquals(1, style.featureTypeStyles().size());

                                FeatureTypeStyle fts = style.featureTypeStyles().get(0);
                                assertEquals(4, fts.rules().size());
                                assertEquals(
                                        "raster",
                                        fts.rules().get(0).getDescription().getTitle().toString());
                                assertEquals(
                                        "orange polygon",
                                        fts.rules().get(1).getDescription().getTitle().toString());
                                assertEquals(
                                        "orange line",
                                        fts.rules().get(2).getDescription().getTitle().toString());
                                assertEquals(
                                        "orange point",
                                        fts.rules().get(3).getDescription().getTitle().toString());

                                for (org.geotools.styling.Rule r : fts.rules()) {
                                    assertEquals(1, r.symbolizers().size());
                                }

                                return null;
                            }
                        });

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getFactory()).andReturn(new CatalogFactoryImpl(null)).anyTimes();
        expect(cat.getResourcePool()).andReturn(rp).anyTimes();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        expect(ft.getName()).andReturn("foo").anyTimes();

        replay(rp, ft, ws, cat);

        StyleGenerator gen =
                new StyleGenerator(cat) {
                    protected void randomizeRamp() {
                        // do not randomize for this test
                    };
                };
        gen.setWorkspace(ws);

        SimpleFeatureType schema = DataUtilities.createType("foo", "geom:Geometry");
        StyleInfo style = gen.createStyle(handler, ft, schema);
        assertNotNull(style);
        assertNotNull(style.getWorkspace());
    }
}
