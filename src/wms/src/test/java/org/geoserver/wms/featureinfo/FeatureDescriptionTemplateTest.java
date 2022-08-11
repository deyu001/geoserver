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

package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertNotNull;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.TemplateUtils;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureDescriptionTemplateTest {

    @Test
    public void testTemplate() throws Exception {
        Configuration cfg = TemplateUtils.getSafeConfiguration();
        cfg.setObjectWrapper(new FeatureWrapper());
        cfg.setClassForTemplateLoading(FeatureTemplate.class, "");

        Template template = cfg.getTemplate("description.ftl");
        assertNotNull(template);

        // create some data
        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType(
                        "testType", "string:String,int:Integer,double:Double,geom:Point");

        SimpleFeature f =
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "three",
                            Integer.valueOf(3),
                            Double.valueOf(3.3),
                            gf.createPoint(new Coordinate(3, 3))
                        },
                        "fid.3");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.process(f, new OutputStreamWriter(output));
        // template.process(f, new OutputStreamWriter(System.out));

        // This generates the following:

        // <h4>testType</h4>

        // <ul class="textattributes">
        // <li><strong><span class="atr-name">string</span>:</strong> <span
        // class="atr-value">three</span></li>
        // <li><strong><span class="atr-name">int</span>:</strong> <span
        // class="atr-value">3</span></li>
        // <li><strong><span class="atr-name">double</span>:</strong> <span
        // class="atr-value">3.3</span></li>
        //
        // </ul>

        // TODO docbuilder cannot parse this? May expect encapsulation, which table did provide

        // DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        // Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        // assertNotNull(document);

        // assertEquals("table", document.getDocumentElement().getNodeName());
    }

    // public void testFeatureCollection() throws Exception {
    // Configuration cfg = TemplateUtils.getSafeConfiguration();
    // cfg.setObjectWrapper(new FeatureWrapper());
    // cfg.setClassForTemplateLoading(FeatureDescriptionTemplate.class, "");
    //
    // Template template = cfg.getTemplate("description.ftl");
    // assertNotNull(template);
    //
    // //create some data
    // GeometryFactory gf = new GeometryFactory();
    // FeatureType featureType = DataUtilities.createType("testType",
    // "string:String,int:Integer,double:Double,geom:Point");
    //
    // DefaultFeature f = new DefaultFeature((DefaultFeatureType) featureType,
    // new Object[] {
    // "three", Integer.valueOf(3), new Double(3.3), gf.createPoint(new Coordinate(3, 3))
    // }, "fid.3") {
    // };
    // DefaultFeature f4 = new DefaultFeature((DefaultFeatureType) featureType,
    // new Object[] {
    // "four", Integer.valueOf(4), new Double(4.4), gf.createPoint(new Coordinate(4, 4))
    // }, "fid.4") {
    // };
    // SimpleFeatureCollection features = new DefaultFeatureCollection(null,null) {};
    // features.add( f );
    // features.add( f4 );
    //
    // template.process(features, new OutputStreamWriter( System.out ));
    //
    // }
}
