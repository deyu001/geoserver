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

package org.geoserver.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFactory;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

/**
 * Simple test class for testing the InstanceOf class.
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class InstanceOfTest {

    @Test
    public void testFactory() {
        // Ensure the Function Factory behaves correctly
        FunctionFactory factory = new GeoServerFunctionFactory();

        List<FunctionName> functionNames = factory.getFunctionNames();

        // Ensure the function name is returned correctly
        assertNotNull(functionNames);
        assertEquals(1, functionNames.size());
        assertEquals(IsInstanceOf.NAME, functionNames.get(0));

        // Get the filterFactory
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        // Ensure the function is returned correctly
        List<Expression> args = new ArrayList<>();
        args.add(ff.literal(Object.class));
        Function f = factory.function(IsInstanceOf.NAME.getFunctionName(), args, null);
        assertNotNull(f);

        f = factory.function(IsInstanceOf.NAME.getName(), args, null);
        assertNotNull(f);

        // Check if the function throws an exception when the parameters number
        // is not correct
        boolean catchedException = false;
        try {
            // Add a new parameter, should trow an exception
            args.add(ff.literal(Object.class));
            f = factory.function(IsInstanceOf.NAME.getName(), args, null);
        } catch (IllegalArgumentException e) {
            catchedException = true;
        }

        assertTrue(catchedException);

        // Check if the function throws an exception when no parameters
        // is present

        catchedException = false;
        try {
            // Add a new parameter, should trow an exception
            f = factory.function(IsInstanceOf.NAME.getName(), null, null);
        } catch (NullPointerException e) {
            catchedException = true;
        }

        assertTrue(catchedException);
    }

    @Test
    public void testFunction() {
        Filter filter = Predicates.isInstanceOf(Object.class);
        // Ensure the filter exists
        assertNotNull(filter);

        // Ensure the filter returned is a PropertyIsEqual filter
        assertTrue(filter instanceof PropertyIsEqualTo);
    }

    @Test
    public void testInstanceOfObject() {
        // Ensuring that this function always return true when the input
        // class is Object
        Filter filter = Predicates.isInstanceOf(Object.class);

        assertTrue(filter.evaluate(new Object()));
        assertTrue(filter.evaluate("test"));
        assertTrue(filter.evaluate(1));
        assertTrue(filter.evaluate(true));
    }

    @Test
    public void testInstanceOfString() {
        // Ensuring that this function return true only when the object
        // class is String
        Filter filter = Predicates.isInstanceOf(String.class);

        assertTrue(filter.evaluate("test"));

        assertFalse(filter.evaluate(new Object()));
        assertFalse(filter.evaluate(1));
        assertFalse(filter.evaluate(true));
    }

    @Test
    public void testInstanceOfLayerInfo() {
        // Ensuring that this function return true only when the object
        // class is LayerInfo
        Filter filter = Predicates.isInstanceOf(LayerInfo.class);

        assertTrue(filter.evaluate(new LayerInfoImpl()));

        assertFalse(filter.evaluate("test"));
        assertFalse(filter.evaluate(new Object()));
        assertFalse(filter.evaluate(1));
        assertFalse(filter.evaluate(true));
    }
}
