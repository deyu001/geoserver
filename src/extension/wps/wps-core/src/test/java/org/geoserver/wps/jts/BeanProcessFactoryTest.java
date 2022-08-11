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

package org.geoserver.wps.jts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.vector.BoundsProcess;
import org.geotools.process.vector.NearestProcess;
import org.geotools.process.vector.SnapProcess;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.factory.FactoryIteratorProvider;
import org.geotools.util.factory.GeoTools;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * Tests some processes that do not require integration with the application context
 *
 * @author Andrea Aime - OpenGeo
 */
public class BeanProcessFactoryTest extends WPSTestSupport {

    public class BeanProcessFactory extends AnnotatedBeanProcessFactory {

        public BeanProcessFactory() {
            super(
                    new SimpleInternationalString("Some bean based processes custom processes"),
                    "bean",
                    BoundsProcess.class,
                    NearestProcess.class,
                    SnapProcess.class);
        }
    }

    BeanProcessFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new BeanProcessFactory();

        // check SPI will see the factory if we register it using an iterator
        // provider
        GeoTools.addFactoryIteratorProvider(
                new FactoryIteratorProvider() {

                    @SuppressWarnings("unchecked")
                    public <T> Iterator<T> iterator(Class<T> category) {
                        if (ProcessFactory.class.isAssignableFrom(category)) {
                            return (Iterator<T>) Collections.singletonList(factory).iterator();
                        } else {
                            return null;
                        }
                    }
                });
    }

    @Test
    public void testNames() {
        Set<Name> names = factory.getNames();
        assertFalse(names.isEmpty());
        // System.out.println(names);
        assertTrue(names.contains(new NameImpl("bean", "Bounds")));
    }

    @Test
    public void testDescribeBounds() {
        NameImpl boundsName = new NameImpl("bean", "Bounds");
        InternationalString desc = factory.getDescription(boundsName);
        assertNotNull(desc);

        Map<String, Parameter<?>> params = factory.getParameterInfo(boundsName);
        assertEquals(1, params.size());

        Parameter<?> features = params.get("features");
        assertEquals(FeatureCollection.class, features.type);
        assertTrue(features.required);

        Map<String, Parameter<?>> result = factory.getResultInfo(boundsName, null);
        assertEquals(1, result.size());
        Parameter<?> bounds = result.get("bounds");
        assertEquals(ReferencedEnvelope.class, bounds.type);
    }

    @Test
    public void testExecuteBounds() throws ProcessException {
        // prepare a mock feature collection
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("test");
        final ReferencedEnvelope re = new ReferencedEnvelope(-10, 10, -10, 10, null);
        FeatureCollection fc =
                new ListFeatureCollection(tb.buildFeatureType()) {
                    @Override
                    public synchronized ReferencedEnvelope getBounds() {
                        return re;
                    }
                };

        org.geotools.process.Process p = factory.create(new NameImpl("bean", "Bounds"));
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("features", fc);
        Map<String, Object> result = p.execute(inputs, null);

        assertEquals(1, result.size());
        ReferencedEnvelope computed = (ReferencedEnvelope) result.get("bounds");
        assertEquals(re, computed);
    }

    @Test
    public void testSPI() throws Exception {
        NameImpl boundsName = new NameImpl("bean", "Bounds");
        ProcessFactory factory = GeoServerProcessors.createProcessFactory(boundsName, false);
        assertNotNull(factory);

        org.geotools.process.Process buffer = GeoServerProcessors.createProcess(boundsName);
        assertNotNull(buffer);
    }
}
