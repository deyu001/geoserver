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

package org.geoserver.wms.map;

import static org.geoserver.data.test.CiteTestData.STREAMS;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.RenderingVariables;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.NullFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;
import org.junit.Test;
import org.opengis.filter.spatial.BBOX;

/**
 * This test class simply ensures that the vector to raster transform is given a BBOX within its
 * query filter
 *
 * @author Rich Fecher
 */
public class VectorToRasterTransformTest extends WMSTestSupport {
    @SuppressWarnings("unchecked")
    @Test
    public void testVectorToRasterTransformUsesBBox() throws IOException {
        final GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.setMapWidth(100);
        map.setMapHeight(100);
        map.setRequest(request);
        final ReferencedEnvelope bounds =
                new ReferencedEnvelope(0, 45, 0, 45, DefaultGeographicCRS.WGS84);
        map.getViewport().setBounds(bounds);

        final FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(STREAMS.getNamespaceURI(), STREAMS.getLocalPart());

        final SimpleFeatureSource featureSource =
                (SimpleFeatureSource) ftInfo.getFeatureSource(null, null);
        final MutableBoolean containsBBox = new MutableBoolean(false);
        // This source should make the renderer fail when asking for the features
        final DecoratingFeatureSource source =
                new DecoratingFeatureSource(featureSource) {
                    @Override
                    public SimpleFeatureCollection getFeatures(final Query query)
                            throws IOException {
                        query.getFilter()
                                .accept(
                                        new NullFilterVisitor() {

                                            @Override
                                            public Object visit(
                                                    final BBOX filter, final Object data) {
                                                containsBBox.setValue(true);
                                                return data;
                                            }
                                        },
                                        null);
                        return featureSource.getFeatures(query);
                    }
                };

        final Style style = parseStyle("HeatmapTransform.sld");
        map.addLayer(new FeatureLayer(source, style));
        request.setFormat("image/gif");

        RenderingVariables.setupEnvironmentVariables(map);
        final RenderedImageMap imageMap =
                new RenderedImageMapOutputFormat(getWMS()).produceMap(map);
        imageMap.dispose();
        assertTrue("The query filter should have a BBOX", containsBBox.booleanValue());
    }

    private Style parseStyle(final String styleName) throws IOException {
        final SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        parser.setInput(RasterSymbolizerVisitorTest.class.getResource(styleName));
        final StyledLayerDescriptor sld = parser.parseSLD();
        final NamedLayer ul = (NamedLayer) sld.getStyledLayers()[0];
        return ul.getStyles()[0];
    }
}
