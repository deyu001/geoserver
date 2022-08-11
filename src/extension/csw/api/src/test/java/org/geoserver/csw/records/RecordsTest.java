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

package org.geoserver.csw.records;

import java.util.Collection;
import java.util.List;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;

public class RecordsTest {

    /**
     * Trying to build <code>
     * <?xml version="1.0" encoding="ISO-8859-1"?>
     * <Record
     * xmlns="http://www.opengis.net/cat/csw/2.0.2"
     * xmlns:dc="http://purl.org/dc/elements/1.1/"
     * xmlns:dct="http://purl.org/dc/terms/"
     * xmlns:ows="http://www.opengis.net/ows"
     * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     * xsi:schemaLocation="http://www.opengis.net/cat/csw/2.0.2
     * ../../../csw/2.0.2/record.xsd">
     * <dc:identifier>00180e67-b7cf-40a3-861d-b3a09337b195</dc:identifier>
     * <dc:title>Image2000 Product 1 (at1) Multispectral</dc:title>
     * <dct:modified>2004-10-04 00:00:00</dct:modified>
     * <dct:abstract>IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.</dct:abstract>
     * <dc:type>dataset</dc:type>
     * <dc:subject>imagery</dc:subject>
     * <dc:subject>baseMaps</dc:subject>
     * <dc:subject>earthCover</dc:subject>
     * <dc:format>BIL</dc:format>
     * <dc:creator>Vanda Lima</dc:creator>
     * <dc:language>en</dc:language>
     * <ows:WGS84BoundingBox>
     * <ows:LowerCorner>14.05 46.46</ows:LowerCorner>
     * <ows:UpperCorner>17.24 48.42</ows:UpperCorner>
     * </ows:WGS84BoundingBox>
     * </Record>
     * </code>
     */
    @Test
    public void testBuildCSWRecord() throws Exception {
        CSWRecordBuilder rb = new CSWRecordBuilder();
        rb.addElement("identifier", "00180e67-b7cf-40a3-861d-b3a09337b195");
        rb.addElement("title", "Image2000 Product 1 (at1) Multispectral");
        rb.addElement("modified", "2004-10-04 00:00:00");
        rb.addElement(
                "abstract",
                "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.");
        rb.addElement("type", "dataset");
        rb.addElement("subject", "imagery", "baseMaps", "earthCover");
        rb.addBoundingBox(
                new ReferencedEnvelope(14.05, 17.24, 46.46, 28.42, DefaultGeographicCRS.WGS84));
        Feature f = rb.build(null);

        assertRecordElement(f, "identifier", "00180e67-b7cf-40a3-861d-b3a09337b195");
        assertRecordElement(f, "title", "Image2000 Product 1 (at1) Multispectral");
        assertRecordElement(f, "modified", "2004-10-04 00:00:00");
        assertRecordElement(
                f,
                "abstract",
                "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.");
        assertRecordElement(f, "type", "dataset");
        assertRecordElement(f, "subject", "imagery", "baseMaps", "earthCover");
        assertBBox(
                f, new ReferencedEnvelope(14.05, 17.24, 46.46, 28.42, DefaultGeographicCRS.WGS84));
    }

    private void assertBBox(Feature f, ReferencedEnvelope... envelopes) throws Exception {
        Property p = f.getProperty(CSWRecordDescriptor.RECORD_BBOX_NAME);
        MultiPolygon geometry = (MultiPolygon) p.getValue();
        @SuppressWarnings("unchecked")
        List<ReferencedEnvelope> featureEnvelopes =
                (List<ReferencedEnvelope>)
                        p.getUserData().get(GenericRecordBuilder.ORIGINAL_BBOXES);
        ReferencedEnvelope total = null;
        for (int i = 0; i < envelopes.length; i++) {
            Assert.assertEquals(envelopes[i], featureEnvelopes.get(i));
            ReferencedEnvelope re = envelopes[i].transform(CSWRecordDescriptor.DEFAULT_CRS, true);
            if (total == null) {
                total = re;
            } else {
                total.expandToInclude(re);
            }
        }

        Assert.assertTrue(total.contains(geometry.getEnvelopeInternal()));
    }

    private void assertRecordElement(Feature f, String elementName, Object... values) {
        AttributeDescriptor identifierDescriptor = CSWRecordDescriptor.getDescriptor(elementName);
        Collection<Property> propertyList = f.getProperties(identifierDescriptor.getName());
        Property[] properties = propertyList.toArray(new Property[propertyList.size()]);
        Assert.assertEquals(properties.length, values.length);
        for (int i = 0; i < properties.length; i++) {
            ComplexAttribute cad = (ComplexAttribute) properties[i];
            Assert.assertEquals(identifierDescriptor, cad.getDescriptor());
            Assert.assertEquals(
                    values[i],
                    cad.getProperty(CSWRecordDescriptor.SIMPLE_LITERAL_VALUE).getValue());
        }
    }
}
