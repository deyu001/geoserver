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

package org.geoserver.metadata.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.junit.Test;

public class ComplexMetaDataMapTest {

    @Test
    public void testSimpleAttributes() {
        MetadataMap underlying = createMap();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

        assertEquals(0, map.size("field-doesntexist"));

        // single-valued

        assertEquals(1, map.size("field-single"));

        ComplexMetadataAttribute<String> att = map.get(String.class, "field-single");

        assertEquals("single value string", att.getValue());
        att.setValue("alteredValue");
        assertEquals("alteredValue", underlying.get("field-single"));

        map.delete("field-single");

        assertEquals(0, map.size("field-single"));
        assertNull(underlying.get("field-single"));

        // multi-valued

        assertEquals(2, map.size("field-as-list"));

        att = map.get(String.class, "field-as-list", 1);
        assertEquals("field list value 2", att.getValue());
        att.setValue("alteredListValue");
        assertEquals("alteredListValue", ((ArrayList<?>) underlying.get("field-as-list")).get(1));

        map.delete("field-as-list", 0);

        assertEquals(1, map.size("field-as-list"));

        assertEquals("alteredListValue", att.getValue());
        assertEquals("alteredListValue", ((ArrayList<?>) underlying.get("field-as-list")).get(0));
        att.setValue("alteredListValue2");
        assertEquals("alteredListValue2", ((ArrayList<?>) underlying.get("field-as-list")).get(0));

        // create new multi value
        assertEquals(0, map.size("ohter-as-list"));

        att = map.get(String.class, "ohter-as-list", 1);
        att.setValue("insert-new-value");

        assertEquals(2, map.size("ohter-as-list"));

        att = map.get(String.class, "ohter-as-list", 1);
        assertEquals("insert-new-value", att.getValue());

        att = map.get(String.class, "ohter-as-list", 0);
        assertEquals(null, att.getValue());
    }

    @Test
    public void testSingleComplex() {
        MetadataMap underlying = createMap();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

        ComplexMetadataMap subMap = map.subMap("object-field");

        assertEquals(1, subMap.size("field1"));
        assertEquals(1, subMap.size("field2"));
        assertEquals(2, subMap.size("field3"));

        ComplexMetadataAttribute<String> att = subMap.get(String.class, "field1");
        assertEquals("object field 01", att.getValue());
        att.setValue("alteredValue");
        assertEquals("alteredValue", underlying.get("object-field/field1"));

        att = subMap.get(String.class, "field3", 1);
        assertEquals("object field list value 2", att.getValue());
        att.setValue("alteredListValue");
        assertEquals(
                "alteredListValue", ((ArrayList<?>) underlying.get("object-field/field3")).get(1));

        subMap.delete("field3", 0);

        assertEquals(1, subMap.size("field3"));

        assertEquals("alteredListValue", att.getValue());
        assertEquals(
                "alteredListValue", ((ArrayList<?>) underlying.get("object-field/field3")).get(0));
        att.setValue("alteredListValue2");
        assertEquals(
                "alteredListValue2", ((ArrayList<?>) underlying.get("object-field/field3")).get(0));
    }

    @Test
    public void testMultiComplex() {
        MetadataMap underlying = createMap();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

        ComplexMetadataMap subMap = map.subMap("object-as-list", 1);

        ComplexMetadataAttribute<String> att1 = subMap.get(String.class, "field 01");
        assertEquals("object list value 2", att1.getValue());
        att1.setValue("alteredValue");
        assertEquals(
                "alteredValue", ((ArrayList<?>) underlying.get("object-as-list/field 01")).get(1));

        ComplexMetadataAttribute<String> att2 = subMap.get(String.class, "field 02", 1);
        assertEquals("object list value other 2.2", att2.getValue());
        att2.setValue("alteredOtherValue");
        assertEquals(
                "alteredOtherValue",
                ((ArrayList<?>) ((ArrayList<?>) underlying.get("object-as-list/field 02")).get(1))
                        .get(1));

        ComplexMetadataAttribute<String> att3 = subMap.get(String.class, "field 03");
        assertNull(att3.getValue());
        att3.setValue("alteredYetOtherValue");
        assertEquals(
                "alteredYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 03")).get(1));

        ComplexMetadataAttribute<String> att4 = subMap.get(String.class, "field 04");
        assertEquals("object single value", att4.getValue());
        att4.setValue("alteredYetYetOtherValue");
        assertEquals(
                "alteredYetYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 04")).get(1));
        assertEquals(
                "object single value",
                ((ArrayList<?>) underlying.get("object-as-list/field 04")).get(0));

        map.delete("object-as-list", 0);

        assertEquals("alteredValue", subMap.get(String.class, "field 01").getValue());
        assertEquals("alteredValue", att1.getValue());
        assertEquals(
                "alteredValue", ((ArrayList<?>) underlying.get("object-as-list/field 01")).get(0));
        assertEquals("alteredOtherValue", att2.getValue());
        assertEquals(
                "alteredOtherValue",
                ((ArrayList<?>) ((ArrayList<?>) underlying.get("object-as-list/field 02")).get(0))
                        .get(1));
        assertEquals("alteredYetOtherValue", att3.getValue());
        assertEquals(
                "alteredYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 03")).get(0));
        assertEquals("alteredYetYetOtherValue", att4.getValue());
        assertEquals(
                "alteredYetYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 04")).get(0));
    }

    private MetadataMap createMap() {
        MetadataMap map = new MetadataMap();
        // String
        map.put("field-single", "single value string");

        // list String
        ArrayList<Object> fieldAsList = new ArrayList<>();
        fieldAsList.add("field list value 1");
        fieldAsList.add("field list value 2");
        map.put("field-as-list", fieldAsList);

        // Object
        map.put("object-field/field1", "object field 01");
        map.put("object-field/field2", "object field 02");
        fieldAsList = new ArrayList<>();
        fieldAsList.add("object field list value 1");
        fieldAsList.add("object field list value 2");
        map.put("object-field/field3", fieldAsList);

        // String per object
        ArrayList<Object> fieldAsListObjectValue01 = new ArrayList<>();
        fieldAsListObjectValue01.add("object list value 1");
        fieldAsListObjectValue01.add("object list value 2");
        map.put("object-as-list/field 01", fieldAsListObjectValue01);

        // list per object
        ArrayList<Object> fieldAsListObjectValue02 = new ArrayList<>();
        ArrayList<Object> fieldAsListObjectValue0201 = new ArrayList<>();
        fieldAsListObjectValue0201.add("object list value other 1");
        ArrayList<Object> fieldAsListObjectValue0202 = new ArrayList<>();
        fieldAsListObjectValue0202.add("object list value other 2.1");
        fieldAsListObjectValue0202.add("object list value other 2.2");
        fieldAsListObjectValue02.add(fieldAsListObjectValue0201);
        fieldAsListObjectValue02.add(fieldAsListObjectValue0202);
        map.put("object-as-list/field 02", fieldAsListObjectValue02);

        ArrayList<Object> fieldAsListObjectValue03 = new ArrayList<>();
        fieldAsListObjectValue03.add("object incomplete list value");
        map.put("object-as-list/field 03", fieldAsListObjectValue03);

        map.put("object-as-list/field 04", "object single value");

        return map;
    }
}
