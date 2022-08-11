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

package org.geoserver.wps.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataInputsType1;
import net.opengis.wps10.DataType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.Wps10Factory;
import org.geoserver.wps.AbstractProcessStoreTest;
import org.geoserver.wps.ProcessStatusStore;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * Tests the JDBC based process status store with a single instance
 *
 * @author Ian Turton
 */
public abstract class AbstractJDBCStatusStoreTest extends AbstractProcessStoreTest {

    private DataStore datastore;

    JDBCStatusStore statusStore;

    String fixtureId;

    abstract String getFixtureId();

    protected Properties getFixture() {
        Properties properties = GSFixtureUtilitiesDelegate.loadFixture(getFixtureId());
        Assume.assumeNotNull(properties);
        return properties;
    }

    @After
    public void cleanup() {
        if (datastore != null) {
            datastore.dispose();
        }
    }

    @Override
    protected ProcessStatusStore buildStore() throws IOException {
        setupDataStore();
        if (Arrays.asList(datastore.getTypeNames()).contains(getStatusTable())) {
            datastore.removeSchema(getStatusTable());
        }
        statusStore = new JDBCStatusStore(datastore);
        return statusStore;
    }

    protected String getStatusTable() {
        return JDBCStatusStore.STATUS;
    }

    protected void setupDataStore() {
        Properties props = getFixture();

        try {
            datastore = DataStoreFinder.getDataStore(DataUtilities.toConnectionParameters(props));
        } catch (IOException e) {
        }

        if (datastore == null) {
            throw new RuntimeException("failed to create dataStore with \n " + props);
        }
    }

    @After
    public void shutdown() {
        // clean up the DB
        if (statusStore != null) statusStore.remove(Filter.INCLUDE);
        if (datastore != null) datastore.dispose();
    }

    @Test
    public void testStackTrace() {
        ExecutionStatus s = new ExecutionStatus(new NameImpl("tracetest"), "ian", false);
        IllegalArgumentException exception = new IllegalArgumentException("a test exception");
        exception.fillInStackTrace();
        s.setException(exception);
        store.save(s);
        ExecutionStatus status = store.get(s.getExecutionId());
        assertEquals(s, status);
        assertEquals(s.getException().getMessage(), status.getException().getMessage());

        StackTraceElement[] expStackTrace = s.getException().getStackTrace();
        StackTraceElement[] obsStackTrace = status.getException().getStackTrace();
        assertEquals(expStackTrace.length, obsStackTrace.length);
        // under latest Java 11 the two traces are not identical, relaxed testing
        assertEquals(expStackTrace[0].toString(), obsStackTrace[0].toString());
        store.remove(s.getExecutionId());
    }

    @Test
    @SuppressWarnings("unchecked") // EMF models without generics
    public void testRequest() {
        Wps10Factory f = Wps10Factory.eINSTANCE;
        ExecuteType ex = f.createExecuteType();

        CodeType id = Ows11Factory.eINSTANCE.createCodeType();
        ex.setIdentifier(id);
        id.setValue("foo");

        DataInputsType1 inputs = f.createDataInputsType1();
        ex.setDataInputs(inputs);

        InputType in = f.createInputType();
        inputs.getInput().add(in);

        DataType data = f.createDataType();
        in.setData(data);

        ComplexDataType cd = f.createComplexDataType();
        data.setComplexData(cd);
        ExecutionStatus s = new ExecutionStatus(new NameImpl("requesttest"), "ian", false);
        s.setRequest(ex);
        store.save(s);
        ExecutionStatus status = store.get(s.getExecutionId());
        assertEquals(s, status);
        ExecuteType obs = status.getRequest();
        ExecuteType expected = s.getRequest();
        assertEquals(expected.getBaseUrl(), obs.getBaseUrl());
        assertEquals(expected.getIdentifier().getValue(), obs.getIdentifier().getValue());
    }
}
