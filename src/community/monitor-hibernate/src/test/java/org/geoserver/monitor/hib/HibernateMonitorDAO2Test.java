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

package org.geoserver.monitor.hib;

import static org.geoserver.monitor.MonitorTestData.assertCovered;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.geoserver.hibernate.HibUtil;
import org.geoserver.monitor.Filter;
import org.geoserver.monitor.MonitorConfig.Mode;
import org.geoserver.monitor.MonitorDAOTestSupport;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.hib.HibernateMonitorDAO2.Sync;
import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class HibernateMonitorDAO2Test extends MonitorDAOTestSupport {

    private static XmlWebApplicationContext ctx;

    @BeforeClass
    public static void initHibernate() throws Exception {

        // setup in memory h2 db
        Properties p = new Properties();
        p.put("driver", "org.h2.Driver");
        p.put("url", "jdbc:h2:mem:monitoring");
        File file = new File("./target/monitoring/db.properties");

        if (!file.getParentFile().exists()) {
            assertTrue(file.getParentFile().mkdirs());
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            p.store(fos, null);
        }

        ctx =
                new XmlWebApplicationContext() {
                    public String[] getConfigLocations() {
                        return new String[] {
                            "classpath*:applicationContext-hibtest.xml",
                            "classpath*:applicationContext-hib2.xml"
                        };
                    }
                };
        ctx.refresh();
        HibernateMonitorDAO2 hibdao = (HibernateMonitorDAO2) ctx.getBean("hibMonitorDAO");
        hibdao.setSync(Sync.SYNC);
        hibdao.setMode(Mode.HYBRID);
        dao = hibdao;

        setUpData();
    }

    @AfterClass
    public static void destroy() throws Exception {
        dao.dispose();
        ctx.close();
        DeleteDbFiles.execute("target/monitoring", "monitoring", false);
    }

    @Before
    public void setUpSession() throws Exception {
        HibUtil.setUpSession(((HibernateMonitorDAO2) dao).getSessionFactory());
    }

    @After
    public void tearDownSession() throws Exception {
        HibUtil.tearDownSession(((HibernateMonitorDAO2) dao).getSessionFactory(), null);
    }

    @Test
    public void testGetRequestsFilterIN3() throws Exception {
        List<RequestData> datas =
                dao.getRequests(new Query().filter("widgets", "resources", Comparison.IN));
        assertCovered(datas, 11, 14, 18);
    }

    @Test
    public void testGetRequestsAggregate() throws Exception {
        final List<RequestData> datas = new ArrayList();
        final List<Object> aggs = new ArrayList();

        RequestDataVisitor v =
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                        aggs.addAll(Arrays.asList(aggregates));
                    }
                };
        dao.getRequests(
                new Query()
                        .properties("path")
                        .filter("path", "/foo", Comparison.EQ)
                        .aggregate("count()")
                        .group("path"),
                v);

        assertEquals(1, datas.size());
        assertEquals("/foo", datas.get(0).getPath());
        assertEquals(4, ((Number) aggs.get(0)).intValue());

        datas.clear();
        aggs.clear();

        dao.getRequests(
                new Query()
                        .properties("service", "operation")
                        .filter("service", null, Comparison.NEQ)
                        .aggregate("count()")
                        .group("service", "operation")
                        .sort("count()", SortOrder.DESC),
                v);

        RequestData r = datas.get(0);
        assertEquals("foo", r.getService());
        assertEquals("x", r.getOperation());
        assertEquals(4, ((Number) aggs.get(0)).intValue());

        r = datas.get(1);
        assertEquals("bam", r.getService());
        assertEquals("y", r.getOperation());
        assertEquals(2, ((Number) aggs.get(1)).intValue());
    }

    @Test
    public void testGetRequestsCount() throws Exception {
        final List<Object> aggs = new ArrayList();

        RequestDataVisitor v =
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        aggs.addAll(Arrays.asList(aggregates));
                    }
                };
        dao.getRequests(new Query().aggregate("count()").filter("path", "/foo", Comparison.EQ), v);

        assertEquals(1, aggs.size());
        assertEquals(4, ((Number) aggs.get(0)).intValue());
    }

    @Test
    public void testGetRequestsFilterAnd() throws Exception {
        assertEquals(
                1,
                dao.getRequests(
                                new Query()
                                        .filter("path", "/foo", Comparison.EQ)
                                        .filter("widgets", "resources", Comparison.IN))
                        .size());
    }

    @Test
    public void testGetRequestsFilterOr() throws Exception {
        assertEquals(
                4,
                dao.getRequests(
                                new Query()
                                        .filter("path", "/seven", Comparison.EQ)
                                        .or("widgets", "resources", Comparison.IN))
                        .size());
    }

    @Test
    public void testGetRequestsJoin() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .properties("path", "resource")
                                .filter("path", "/foo", Comparison.EQ)
                                .group("path", "resource")
                                .sort("resource", SortOrder.ASC));

        assertEquals(3, datas.size());
        assertEquals("stuff", datas.get(0).getResources().get(0));
        assertEquals("things", datas.get(1).getResources().get(0));
        assertEquals("widgets", datas.get(2).getResources().get(0));
    }

    @Test
    public void testGetRequestsJoinVisitor() throws Exception {
        final List<RequestData> datas = new ArrayList();
        final List<Object> aggs = new ArrayList();

        RequestDataVisitor v =
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                        // aggs.addAll(Arrays.asList(aggregates));
                    }
                };
        dao.getRequests(
                new Query()
                        .properties("path", "resource")
                        .filter("path", "/foo", Comparison.EQ)
                        .group("path", "resource")
                        .sort("resource", SortOrder.ASC),
                v);

        assertEquals(3, datas.size());
        assertEquals(1, datas.get(0).getResources().size());
        assertEquals("stuff", datas.get(0).getResources().get(0));
        assertEquals(1, datas.get(1).getResources().size());
        assertEquals("things", datas.get(1).getResources().get(0));
        assertEquals(1, datas.get(2).getResources().size());
        assertEquals("widgets", datas.get(2).getResources().get(0));
    }

    @Test
    public void testGetRequestsJoin2() throws Exception {
        final List<RequestData> datas = new ArrayList();
        final List<Object> aggs = new ArrayList();

        dao.getRequests(
                new Query()
                        .properties("resource")
                        .aggregate("count()")
                        .filter("resource", null, Comparison.NEQ)
                        .group("resource"),
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                        aggs.add(aggregates[0]);
                    }
                });

        // assertEquals(3, datas.size());
        for (RequestData data : datas) {
            System.out.println(data.getResources());
        }
    }

    @Test
    public void testGetRequestsJoinIN() throws Exception {
        List<String> resources = Arrays.asList("widgets", "things");
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .properties("resource")
                                .aggregate("count()")
                                .filter("resource", resources, Comparison.IN)
                                .group("resource")
                                .sort("resource", SortOrder.ASC));

        assertEquals(2, datas.size());
        assertEquals("things", datas.get(0).getResources().get(0));
        assertEquals("widgets", datas.get(1).getResources().get(0));
    }

    @Test
    public void testGetRequestsAdvancedFilter() throws Exception {
        Filter filter =
                new Filter("path", "/four", Comparison.EQ)
                        .or(
                                new Filter("service", "foo", Comparison.EQ)
                                        .and(
                                                new Filter(
                                                        "resource",
                                                        Arrays.asList("widgets"),
                                                        Comparison.IN)));

        List<RequestData> datas = dao.getRequests(new Query().filter(filter));
        assertEquals(2, datas.size());
        assertCovered(datas, 4, 11);
    }

    //    @Test
    //    public void testFoo() throws Exception {
    //        SessionFactory sessionFactory = ((HibernateMonitorDAO2)dao).getSessionFactory();
    //        Session session = sessionFactory.getCurrentSession();
    //
    //        /*Query q = session.createQuery("SELECT rd.path FROM RequestData rd, LayerData ld " +
    //                "WHERE ld in elements(rd.layers) " +
    //                "AND ld.name = 'things'");*/
    //        /*Query q = session.createQuery("SELECT rd.path FROM RequestData rd " +
    //            "INNER JOIN rd.layers as layer WITH layer.name = 'things'");*/
    //        Query q = session.createQuery(
    //            "SELECT r.path, layer FROM RequestData r LEFT JOIN r.layers as layer " +
    //            " WHERE r.path = '/foo' GROUP BY r.path, layer");
    //
    //        for (Object o : q.list()) {
    //            Object[] vals = (Object[]) o;
    //            System.out.println(String.format("%s, %s", vals[0].toString(),
    // vals[1].toString()));
    //        }
    //
    //    }
}
