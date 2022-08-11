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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.geoserver.monitor.RequestData.Status;
import org.geotools.util.Converters;

public class MonitorTestData {

    static DateFormat FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    MonitorDAO dao;
    List<RequestData> data;
    boolean extended;

    public MonitorTestData(MonitorDAO dao) {
        this(dao, true);
    }

    public MonitorTestData(MonitorDAO dao, boolean extended) {
        this.dao = dao;
        this.data = new ArrayList<>();
        this.extended = extended;
    }

    public List<RequestData> getData() {
        return data;
    }

    public void setup() throws ParseException {

        data.add(data(1, "/one", "2010-07-23T15:26:44", "2010-07-23T15:26:59", "RUNNING"));
        data.add(data(2, "/two", "2010-07-23T15:36:44", "2010-07-23T15:36:47", "WAITING"));
        data.add(data(3, "/three", "2010-07-23T15:46:44", "2010-07-23T15:46:52", "FINISHED"));
        data.add(data(4, "/four", "2010-07-23T15:56:44", "2010-07-23T15:56:48", "FAILED"));
        data.add(data(5, "/five", "2010-07-23T16:06:44", "2010-07-23T16:06:45", "RUNNING"));
        data.add(data(6, "/six", "2010-07-23T16:16:44", "2010-07-23T16:16:53", "WAITING"));
        data.add(data(7, "/seven", "2010-07-23T16:26:44", "2010-07-23T16:26:47", "FINISHED"));
        data.add(data(8, "/eight", "2010-07-23T16:36:44", "2010-07-23T16:36:46", "FAILED"));
        data.add(data(9, "/nine", "2010-07-23T16:46:44", "2010-07-23T16:46:53", "CANCELLING"));
        data.add(data(10, "/ten", "2010-07-23T16:56:44", "2010-07-23T16:56:47", "RUNNING"));

        if (extended) {
            data.add(
                    data(
                            11,
                            "/foo",
                            "2010-08-23T15:26:44",
                            "2010-08-23T15:26:59",
                            "RUNNING",
                            "foo",
                            "x",
                            "widgets"));
            data.add(
                    data(
                            12,
                            "/bar",
                            "2010-08-23T15:36:44",
                            "2010-08-23T15:36:47",
                            "WAITING",
                            "bar",
                            "y",
                            "things"));
            data.add(
                    data(
                            13,
                            "/baz",
                            "2010-08-23T15:46:44",
                            "2010-08-23T15:46:52",
                            "FINISHED",
                            "baz",
                            "x",
                            "stuff"));
            data.add(
                    data(
                            14,
                            "/bam",
                            "2010-08-23T15:56:44",
                            "2010-08-23T15:56:48",
                            "FAILED",
                            "bam",
                            "x",
                            "widgets",
                            "things"));
            data.add(
                    data(
                            15,
                            "/foo",
                            "2010-08-23T16:06:44",
                            "2010-08-23T16:06:45",
                            "RUNNING",
                            "foo",
                            "x",
                            "things",
                            "stuff"));
            data.add(
                    data(
                            16,
                            "/foo",
                            "2010-08-23T16:16:44",
                            "2010-08-23T16:16:53",
                            "WAITING",
                            "foo",
                            "x",
                            "stuff"));
            data.add(
                    data(
                            17,
                            "/bar",
                            "2010-08-23T16:26:44",
                            "2010-08-23T16:26:47",
                            "FINISHED",
                            "bar",
                            "z",
                            "things",
                            "stuff"));
            data.add(
                    data(
                            18,
                            "/bam",
                            "2010-08-23T16:36:44",
                            "2010-08-23T16:36:46",
                            "FAILED",
                            "bam",
                            "y",
                            "widgets"));
            data.add(
                    data(
                            19,
                            "/bam",
                            "2010-08-23T16:46:44",
                            "2010-08-23T16:46:53",
                            "CANCELLING",
                            "bam",
                            "y",
                            "stuff"));
            data.add(
                    data(
                            20,
                            "/foo",
                            "2010-08-23T16:56:44",
                            "2010-08-23T16:56:47",
                            "RUNNING",
                            "foo",
                            "x",
                            "things"));
        }

        // subclass hook
        addTestData(data);

        for (RequestData r : data) {
            dao.save(dao.init(r));
        }
    }

    protected RequestData data(long id, String path, String start, String end, String status)
            throws ParseException {
        RequestData data = new RequestData();
        data.setPath(path);
        data.setStartTime(toDate(start));
        data.setEndTime(toDate(end));
        data.setStatus(Status.valueOf(status));
        return data;
    }

    protected RequestData data(
            long id,
            String path,
            String start,
            String end,
            String status,
            String owsService,
            String owsOperation,
            String... layers)
            throws ParseException {
        RequestData data = data(id, path, start, end, status);
        data.setService(owsService);
        data.setOperation(owsOperation);
        data.setResources(Arrays.asList(layers));

        return data;
    }

    protected void addTestData(List<RequestData> datas) throws ParseException {}

    public static Date toDate(String s) {
        return Converters.convert(s, Date.class);
    }

    public static void assertCovered(List<RequestData> datas, int... id) {
        assertEquals(id.length, datas.size());
        HashSet<Long> ids = new HashSet<>();
        for (RequestData data : datas) {
            ids.add(data.getId());
        }

        for (int j : id) {
            assertTrue(ids.contains(Long.valueOf(j)));
        }
    }

    public static void assertCoveredInOrder(List<RequestData> datas, int... id) {
        assertEquals(id.length, datas.size());
        for (int i = 0; i < id.length; i++) {
            assertEquals(id[i], datas.get(i).getId());
        }
    }
}
