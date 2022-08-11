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

package org.geoserver.jdbcconfig.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.base.Optional;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.easymock.EasyMock;
import org.geoserver.jdbcloader.DataSourceFactoryBean;
import org.junit.Test;

/** @author Kevin Smith, OpenGeo */
public class DataSourceFactoryBeanTest {

    @Test
    public void testBasic() throws Exception {
        final BasicDataSource ds = EasyMock.createMock(BasicDataSource.class);
        JDBCConfigProperties config = EasyMock.createMock(JDBCConfigProperties.class);
        Context jndi = EasyMock.createMock(Context.class);

        expect(config.isEnabled()).andReturn(true);
        expectJndi(config, null);

        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:test"));
        ds.setUrl("jdbc:test");
        expectLastCall();
        expect(config.getProperty("driverClassName"))
                .andStubReturn("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        ds.setDriverClassName("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        expectLastCall();
        expect(config.getProperty("username")).andStubReturn("testUser");
        ds.setUsername("testUser");
        expect(config.getProperty("password")).andStubReturn("swordfish");
        ds.setPassword("swordfish");

        expect(config.getProperty("pool.minIdle")).andStubReturn(null);
        expect(config.getProperty("pool.maxActive")).andStubReturn(null);
        expect(config.getProperty("pool.poolPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.maxOpenPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.testOnBorrow")).andStubReturn(null);

        config.setDatasourceId("jdbc:test");
        expectLastCall();

        ds.setMinIdle(1);
        expectLastCall();
        ds.setMaxActive(10);
        expectLastCall();
        ds.setPoolPreparedStatements(true);
        expectLastCall();
        ds.setMaxOpenPreparedStatements(50);
        expectLastCall();

        expectVerifyConnect(ds);
        replay(ds, config, jndi);

        DataSourceFactoryBean fact =
                new DataSourceFactoryBean(config, jndi) {

                    @Override
                    protected BasicDataSource createBasicDataSource() {
                        return ds;
                    }
                };

        // Check that we get the DataSource
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds);
        reset(ds);
        replay(ds);

        // Check that the same DataSource is returned on subsequent calls without any changes
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds, config, jndi);

        // Check that destruction properly closes the DataSource
        reset(ds);
        ds.close();
        expectLastCall();
        replay(ds);
        fact.destroy();
        verify(ds);
    }

    @Test
    public void testJNDI() throws Exception {
        DataSource ds = EasyMock.createMock(DataSource.class);
        JDBCConfigProperties config = EasyMock.createMock(JDBCConfigProperties.class);
        Context jndi = EasyMock.createMock(Context.class);

        expect(config.isEnabled()).andReturn(true);
        expectJndi(config, "java:comp/env/jdbc/test");
        expect(jndi.lookup("java:comp/env/jdbc/test")).andStubReturn(ds);
        config.setDatasourceId("java:comp/env/jdbc/test");
        expectLastCall();

        expectVerifyConnect(ds);
        replay(ds, config, jndi);

        DataSourceFactoryBean fact = new DataSourceFactoryBean(config, jndi);

        // Check that we get the DataSource
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds);
        reset(ds);
        replay(ds);

        // Check that the same DataSource is returned on subsequent calls without any changes
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds, config, jndi);

        // Destruction shouldn't do anything to the DataSource
        reset(ds);

        replay(ds);
        fact.destroy();
        verify(ds);
    }

    /** If JNDI lookup fails, fall back to properties file */
    @Test
    public void testJNDIFail() throws Exception {
        final BasicDataSource ds = EasyMock.createMock(BasicDataSource.class);
        JDBCConfigProperties config = EasyMock.createMock(JDBCConfigProperties.class);
        Context jndi = EasyMock.createMock(Context.class);

        expect(config.isEnabled()).andReturn(true);
        expectJndi(config, "java:comp/env/jdbc/test");
        expect(jndi.lookup("java:comp/env/jdbc/test")).andStubThrow(new NamingException());

        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:test"));
        ds.setUrl("jdbc:test");
        expectLastCall();
        expect(config.getProperty("driverClassName"))
                .andStubReturn("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        ds.setDriverClassName("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        expectLastCall();
        expect(config.getProperty("username")).andStubReturn("testUser");
        ds.setUsername("testUser");
        expect(config.getProperty("password")).andStubReturn("swordfish");
        ds.setPassword("swordfish");

        expect(config.getProperty("pool.minIdle")).andStubReturn(null);
        expect(config.getProperty("pool.maxActive")).andStubReturn(null);
        expect(config.getProperty("pool.poolPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.maxOpenPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.testOnBorrow")).andStubReturn(null);

        config.setDatasourceId("jdbc:test");
        expectLastCall();

        ds.setMinIdle(1);
        expectLastCall();
        ds.setMaxActive(10);
        expectLastCall();
        ds.setPoolPreparedStatements(true);
        expectLastCall();
        ds.setMaxOpenPreparedStatements(50);
        expectLastCall();

        expectVerifyConnect(ds);

        replay(ds, config, jndi);

        DataSourceFactoryBean fact =
                new DataSourceFactoryBean(config, jndi) {

                    @Override
                    protected BasicDataSource createBasicDataSource() {
                        return ds;
                    }
                };

        // Check that we get the DataSource
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds);
        reset(ds);
        replay(ds);

        // Check that the same DataSource is returned on subsequent calls without any changes
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds, config, jndi);

        // Check that destruction properly closes the DataSource
        reset(ds);
        ds.close();
        expectLastCall();
        replay(ds);
        fact.destroy();
        verify(ds);
    }

    private void expectJndi(JDBCConfigProperties config, String name) {
        expect(config.getProperty("jndiName")).andStubReturn(name);
        expect(config.getJndiName()).andStubReturn(Optional.fromNullable(name));
    }

    private void expectVerifyConnect(DataSource ds) throws Exception {
        Connection conn = createMock(Connection.class);
        // the 2 times expectations are due to checking the database metadata
        // during Dialect initialization
        conn.close();
        expectLastCall();
        DatabaseMetaData metadata = EasyMock.createMock(DatabaseMetaData.class);
        expect(metadata.getDriverName()).andReturn("test");
        expect(conn.getMetaData()).andReturn(metadata);
        replay(conn);
        replay(metadata);
        expect(ds.getConnection()).andReturn(conn);
    }
}
