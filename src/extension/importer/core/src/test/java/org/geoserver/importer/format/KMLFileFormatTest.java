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

package org.geoserver.importer.format;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.geotools.data.FeatureReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class KMLFileFormatTest {

    private KMLFileFormat kmlFileFormat;
    static final String DOC_EL = "<kml xmlns=\"http://www.opengis.net/kml/2.2\">";

    @Before
    public void setUp() throws Exception {
        kmlFileFormat = new KMLFileFormat();
    }

    @Test
    public void testParseFeatureTypeNoPlacemarks() throws IOException {
        String kmlInput = DOC_EL + "</kml>";
        try {
            kmlFileFormat.parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"));
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.fail("Expected Illegal Argument Exception for no features");
    }

    @Test
    public void testParseFeatureTypeMinimal() throws Exception {
        String kmlInput = DOC_EL + "<Placemark></Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        Assert.assertEquals(
                "Unexpected number of feature type attributes",
                10,
                featureType.getAttributeCount());
    }

    @Test
    public void testExtendedUserData() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<Data name=\"foo\"><value>bar</value></Data>"
                        + "<Data name=\"quux\"><value>morx</value></Data>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes("fleem", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        Assert.assertEquals(
                "Unexpected number of feature type attributes",
                12,
                featureType.getAttributeCount());
        Assert.assertEquals(
                "Invalid attribute descriptor",
                String.class,
                featureType.getDescriptor("foo").getType().getBinding());
        Assert.assertEquals(
                "Invalid attribute descriptor",
                String.class,
                featureType.getDescriptor("quux").getType().getBinding());
    }

    @Test
    public void testReadFeatureWithNameAndDescription() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Placemark><name>foo</name><description>bar</description></Placemark></kml>";
        SimpleFeatureType featureType =
                kmlFileFormat
                        .parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"))
                        .get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            Assert.assertTrue("No features found", reader.hasNext());
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid name attribute", "foo", feature.getAttribute("name"));
            Assert.assertEquals(
                    "Invalid description attribute", "bar", feature.getAttribute("description"));
        }
    }

    @Test
    public void testReadFeatureWithUntypedExtendedData() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<Data name=\"foo\"><value>bar</value></Data>"
                        + "<Data name=\"quux\"><value>morx</value></Data>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        SimpleFeatureType featureType =
                kmlFileFormat
                        .parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"))
                        .get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            Assert.assertTrue("No features found", reader.hasNext());
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", "bar", feature.getAttribute("foo"));
            Assert.assertEquals("Invalid ext attr quux", "morx", feature.getAttribute("quux"));
        }
    }

    @Test
    public void testReadFeatureWithTypedExtendedData() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"myschema\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<SchemaData schemaUrl=\"#myschema\">"
                        + "<SimpleData name=\"foo\">42</SimpleData>"
                        + "</SchemaData>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        SimpleFeatureType featureType =
                kmlFileFormat
                        .parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"))
                        .get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            Assert.assertTrue("No features found", reader.hasNext());
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", 42, feature.getAttribute("foo"));
        }
    }

    @Test
    public void testMultipleSchemas() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"schema1\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<Schema name=\"schema2\">"
                        + "<SimpleField type=\"float\" name=\"bar\"></SimpleField>"
                        + "</Schema>"
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<SchemaData schemaUrl=\"#schema1\">"
                        + "<SimpleData name=\"foo\">42</SimpleData>"
                        + "</SchemaData>"
                        + "<SchemaData schemaUrl=\"#schema2\">"
                        + "<SimpleData name=\"bar\">4.2</SimpleData>"
                        + "</SchemaData>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes(
                        "multiple", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType ft = featureTypes.get(0);

        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(ft, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            SimpleFeature feature1 = reader.next();
            Assert.assertNotNull("Expecting feature", feature1);
            Assert.assertEquals("Invalid ext attr foo", 42, feature1.getAttribute("foo"));
            Assert.assertEquals(
                    "Invalid ext attr bar", 4.2f, (Float) feature1.getAttribute("bar"), 0.01);
        }
    }

    @Test
    public void testTypedAndUntyped() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"myschema\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<SchemaData schemaUrl=\"#myschema\">"
                        + "<SimpleData name=\"foo\">42</SimpleData>"
                        + "</SchemaData>"
                        + "<Data name=\"fleem\"><value>bar</value></Data>"
                        + "<Data name=\"quux\"><value>morx</value></Data>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes(
                        "typed-and-untyped", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", 42, feature.getAttribute("foo"));
            Assert.assertEquals("bar", feature.getAttribute("fleem"));
            Assert.assertEquals("morx", feature.getAttribute("quux"));
        }
    }

    @Test
    public void testReadCustomSchema() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"myschema\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<myschema><foo>7</foo></myschema>"
                        + "</kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes(
                        "custom-schema", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        Map<Object, Object> userData = featureType.getUserData();
        @SuppressWarnings("unchecked")
        List<String> schemaNames = (List<String>) userData.get("schemanames");
        Assert.assertEquals(1, schemaNames.size());
        Assert.assertEquals(
                "Did not find expected schema name metadata", "myschema", schemaNames.get(0));
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", 7, feature.getAttribute("foo"));
        }
    }
}
