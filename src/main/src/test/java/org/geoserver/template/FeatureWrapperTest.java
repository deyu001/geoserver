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

package org.geoserver.template;

import static org.junit.Assert.assertEquals;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.StringWriter;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureWrapperTest {
    DefaultFeatureCollection features;
    Configuration cfg;

    @Before
    public void setUp() throws Exception {

        // create some data
        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType(
                        "testType", "string:String,int:Integer,double:Double,geom:Point");

        features = new DefaultFeatureCollection() {};
        features.add(
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "one",
                            Integer.valueOf(1),
                            Double.valueOf(1.1),
                            gf.createPoint(new Coordinate(1, 1))
                        },
                        "fid.1"));
        features.add(
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "two",
                            Integer.valueOf(2),
                            Double.valueOf(2.2),
                            gf.createPoint(new Coordinate(2, 2))
                        },
                        "fid.2"));
        features.add(
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "three",
                            Integer.valueOf(3),
                            Double.valueOf(3.3),
                            gf.createPoint(new Coordinate(3, 3))
                        },
                        "fid.3"));
        cfg = TemplateUtils.getSafeConfiguration();
        cfg.setClassForTemplateLoading(getClass(), "");
        cfg.setObjectWrapper(createWrapper());
    }

    public FeatureWrapper createWrapper() {
        return new FeatureWrapper();
    }

    @Test
    public void testFeatureCollection() throws Exception {
        Template template = cfg.getTemplate("FeatureCollection.ftl");

        StringWriter out = new StringWriter();
        template.process(features, out);

        assertEquals(
                "fid.1\nfid.2\nfid.3\n",
                out.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    @Test
    public void testFeatureSimple() throws Exception {
        Template template = cfg.getTemplate("FeatureSimple.ftl");

        StringWriter out = new StringWriter();
        template.process(features.iterator().next(), out);

        // replace ',' with '.' for locales which use a comma for decimal point
        assertEquals(
                "one\n1\n1.1\nPOINT (1 1)",
                out.toString().replace(',', '.').replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    @Test
    public void testFeatureDynamic() throws Exception {
        Template template = cfg.getTemplate("FeatureDynamic.ftl");

        StringWriter out = new StringWriter();
        template.process(features.iterator().next(), out);

        // replace ',' with '.' for locales which use a comma for decimal point
        assertEquals(
                "string=one\nint=1\ndouble=1.1\ngeom=POINT (1 1)\n",
                out.toString().replace(',', '.').replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    @Test
    public void testFeatureSequence() throws Exception {
        Template template = cfg.getTemplate("FeatureSequence.ftl");

        StringWriter out = new StringWriter();
        template.process(features, out);

        assertEquals(
                "three\none\n3", out.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }
}
