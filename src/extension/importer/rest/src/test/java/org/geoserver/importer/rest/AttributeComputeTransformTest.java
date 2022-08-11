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

package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import org.geoserver.importer.transform.AttributeComputeTransform;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.Converters;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public class AttributeComputeTransformTest extends TransformTestSupport {

    @Test
    public void testTransformLiteral() throws Exception {
        AttributeComputeTransform tx =
                new AttributeComputeTransform("theDate", Date.class, "2012-05-03T12:00:00Z");

        // reference
        // riverType = DataUtilities.createType(namespace+".river",
        // "id:0,geom:MultiLineString,river:String,flow:0.0");

        // transforming type
        SimpleFeatureType transformedType = tx.apply(null, null, riverType);
        AttributeDescriptor ad = transformedType.getDescriptor("theDate");
        assertNotNull(ad);
        assertEquals(Date.class, ad.getType().getBinding());

        // transforming feature
        SimpleFeature riverFeature = riverFeatures[0];
        SimpleFeature targetFeature =
                SimpleFeatureBuilder.build(transformedType, riverFeature.getAttributes(), "theId");
        SimpleFeature transformed = tx.apply(null, null, riverFeature, targetFeature);
        assertEquals(
                Converters.convert("2012-05-03T12:00:00Z", Date.class),
                transformed.getAttribute("theDate"));
    }

    @Test
    public void testTransformExpression() throws Exception {
        AttributeComputeTransform tx =
                new AttributeComputeTransform("flowSquared", Double.class, "flow * flow");

        // reference
        // riverType = DataUtilities.createType(namespace+".river",
        // "id:0,geom:MultiLineString,river:String,flow:0.0");

        // transforming type
        SimpleFeatureType transformedType = tx.apply(null, null, riverType);
        AttributeDescriptor ad = transformedType.getDescriptor("flowSquared");
        assertNotNull(ad);
        assertEquals(Double.class, ad.getType().getBinding());

        // transforming feature
        SimpleFeature riverFeature = riverFeatures[0];
        SimpleFeature targetFeature =
                SimpleFeatureBuilder.build(transformedType, riverFeature.getAttributes(), "theId");
        SimpleFeature transformed = tx.apply(null, null, riverFeature, targetFeature);
        Double flow = (Double) riverFeature.getAttribute("flow");
        assertEquals(flow * flow, (Double) transformed.getAttribute("flowSquared"), 0d);
    }

    @Test
    public void testAddExisting() throws Exception {
        AttributeComputeTransform tx = new AttributeComputeTransform("flow", Double.class, "123");

        // reference
        // riverType = DataUtilities.createType(namespace+".river",
        // "id:0,geom:MultiLineString,river:String,flow:0.0");

        // transforming type
        try {
            tx.apply(null, null, riverType);
            fail("Should have thrown an exception, flow is already there");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("flow"));
        }
    }

    @Test
    public void testJSON() throws Exception {
        doJSONTest(new AttributeComputeTransform("flowSquared", Double.class, "flow * flow"));
        doJSONTest(new AttributeComputeTransform("theDate", Date.class, "2012-05-03T12:00:00Z"));
    }
}
