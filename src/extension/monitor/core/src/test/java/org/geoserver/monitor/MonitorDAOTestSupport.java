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

package org.geoserver.monitor;

import static org.geoserver.monitor.MonitorTestData.assertCovered;
import static org.geoserver.monitor.MonitorTestData.assertCoveredInOrder;
import static org.geoserver.monitor.MonitorTestData.toDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData.Status;
import org.junit.Test;

public abstract class MonitorDAOTestSupport {

    protected static MonitorTestData testData;
    protected static MonitorDAO dao;

    protected static void setUpData() throws Exception {
        testData = new MonitorTestData(dao);
        testData.setup();
    }

    @Test
    public void testUpdate() throws Exception {
        RequestData data = dao.getRequest(1);
        data.setPath("/one_updated");
        dao.update(data);

        data = dao.getRequest(1);
        assertEquals("/one_updated", data.getPath());

        data.getResources().add("one_layer");
        dao.update(data);

        data = dao.getRequest(1);
        assertEquals(1, data.getResources().size());

        assertEquals("one_layer", data.getResources().get(0));
    }

    @Test
    public void testGetRequests() throws Exception {
        List<RequestData> requests = dao.getRequests();
        assertEquals(testData.getData().size(), requests.size());
        // assertCovered(requests, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertCovered(requests, range(1, 20));
    }

    int[] range(int low, int high) {
        int[] nums = new int[high - low + 1];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = low + i;
        }
        return nums;
    }

    @Test
    public void testGetRequestsVisitor() throws Exception {
        final List<RequestData> datas = new ArrayList<>();
        dao.getRequests(
                new Query().filter("path", "/seven", Comparison.EQ),
                new RequestDataVisitor() {

                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                    }
                });

        assertCoveredInOrder(datas, 7);
    }

    @Test
    public void testGetRequestById() throws Exception {
        assertNotNull(dao.getRequest(8));
        assertEquals("/eight", dao.getRequest(8).getPath());
    }

    @Test
    public void testGetRequestsSorted() throws Exception {
        assertCoveredInOrder(
                dao.getRequests(
                        new Query().filter("id", 11l, Comparison.LT).sort("path", SortOrder.ASC)),
                8,
                5,
                4,
                9,
                1,
                7,
                6,
                10,
                3,
                2);
    }

    @Test
    public void testGetRequestsBetween() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .between(
                                        toDate("2010-07-23T15:55:00"),
                                        toDate("2010-07-23T16:17:00")));

        assertCoveredInOrder(datas, 6, 5, 4);
    }

    @Test
    public void testGetRequestsBetween2() throws Exception {
        // test that the query is inclusive, and test sorting
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .between(
                                        toDate("2010-07-23T15:56:44"),
                                        toDate("2010-07-23T16:16:44"))
                                .sort("startTime", SortOrder.ASC));

        assertCoveredInOrder(datas, 4, 5, 6);
    }

    @Test
    public void testGetRequestsPaged() throws Exception {
        List<RequestData> datas =
                dao.getRequests(new Query().page(5l, 2l).sort("startTime", SortOrder.ASC));

        assertCoveredInOrder(datas, 6, 7);
    }

    @Test
    public void testGetRequestsFilter() throws Exception {
        assertCoveredInOrder(
                dao.getRequests(new Query().filter("path", "/seven", Comparison.EQ)), 7);
    }

    @Test
    public void testGetRequestsFilterNull() throws Exception {
        assertEquals(0, dao.getRequests(new Query().filter("path", null, Comparison.EQ)).size());
        assertEquals(
                testData.getData().size(),
                dao.getRequests(new Query().filter("path", null, Comparison.NEQ)).size());
    }

    @Test
    public void testGetRequestsFilterIN() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query().filter("path", Arrays.asList("/two", "/seven"), Comparison.IN));
        assertCovered(datas, 2, 7);
    }

    @Test
    public void testGetRequestsFilterIN2() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .filter(
                                        "status",
                                        Arrays.asList(Status.RUNNING, Status.WAITING),
                                        Comparison.IN));
        assertCovered(datas, 1, 2, 5, 6, 10, 11, 12, 15, 16, 20);
    }

    @Test
    public void testGetCount() throws Exception {
        assertEquals(4, dao.getCount(new Query().filter("path", "/foo", Comparison.EQ)));
    }

    @Test
    public void testGetIterator() throws Exception {
        Iterator<RequestData> it =
                dao.getIterator(
                        new Query().filter("path", Arrays.asList("/two", "/seven"), Comparison.IN));

        assertTrue(it.hasNext());
        RequestData data = it.next();
        assertEquals("/two", data.getPath());

        assertTrue(it.hasNext());
        data = it.next();
        assertEquals("/seven", data.getPath());

        assertFalse(it.hasNext());
    }
}
