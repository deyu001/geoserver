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

package org.geoserver.security;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

public class TestAccessLimitsSerialization {

    FilterFactory2 ff;

    Filter filter;

    MultiPolygon g;

    @Before
    public void setUp() throws Exception {
        ff = CommonFactoryFinder.getFilterFactory2(null);
        filter = ff.equal(ff.property("attribute"), ff.literal(3), true);
        g = (MultiPolygon) new WKTReader().read("MULTIPOLYGON(((0 0, 0 10, 10 10, 10 0, 0 0)))");
    }

    @Test
    public void testAccessLimits() throws Exception {
        AccessLimits limits = new AccessLimits(CatalogMode.MIXED);

        testObjectSerialization(limits);
    }

    @Test
    public void testSerializeWorkspaceAccessLimits() throws Exception {
        WorkspaceAccessLimits limits =
                new WorkspaceAccessLimits(CatalogMode.HIDE, true, true, true);

        testObjectSerialization(limits);
    }

    @Test
    public void testSerializeDataAccessLimits() throws Exception {
        DataAccessLimits limits = new DataAccessLimits(CatalogMode.CHALLENGE, filter);

        testObjectSerialization(limits);
    }

    @Test
    public void testCoverageAccessLimits() throws Exception {
        CoverageAccessLimits limits = new CoverageAccessLimits(CatalogMode.MIXED, filter, g, null);

        testObjectSerialization(limits);
    }

    @Test
    public void testVectorAccessLimits() throws Exception {
        List<PropertyName> properties = new ArrayList<>();
        properties.add(ff.property("test"));
        VectorAccessLimits limits =
                new VectorAccessLimits(CatalogMode.MIXED, properties, filter, properties, filter);

        testObjectSerialization(limits);
    }

    @Test
    public void testWMSAccessLimits() throws Exception {
        List<PropertyName> properties = new ArrayList<>();
        properties.add(ff.property("test"));
        WMSAccessLimits limits = new WMSAccessLimits(CatalogMode.MIXED, filter, g, true);

        testObjectSerialization(limits);
    }

    private void testObjectSerialization(Serializable object) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);

            try (ObjectInputStream ois =
                    new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
                Object clone = ois.readObject();
                Assert.assertNotSame(object, clone);
                Assert.assertEquals(object, clone);
            }
        }
    }
}
