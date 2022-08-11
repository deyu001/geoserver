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

package org.geoserver.monitor.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.Converters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = {
        RestBaseController.ROOT_PATH + "/monitor/requests/{request}",
        RestBaseController.ROOT_PATH + "/monitor/requests"
    }
)
public class MonitorRequestController extends RestBaseController {

    static final String CSV_MEDIATYPE_VALUE = "application/csv";

    static final String ZIP_MEDIATYPE_VALUE = "application/zip";

    static final String EXCEL_MEDIATYPE_VALUE = "application/vnd.ms-excel";

    static final MediaType EXCEL_MEDIATYPE = MediaType.valueOf(EXCEL_MEDIATYPE_VALUE);

    static final MediaType ZIP_MEDIATYPE = MediaType.valueOf(ZIP_MEDIATYPE_VALUE);

    static final MediaType CSV_MEDIATYPE = MediaType.valueOf(CSV_MEDIATYPE_VALUE);

    Monitor monitor;

    @Autowired
    public MonitorRequestController(Monitor monitor) {
        this.monitor = monitor;
    }

    String[] getFields(String fields) {
        if (fields != null) {
            return fields.split(";");
        } else {
            List<String> props = OwsUtils.getClassProperties(RequestData.class).properties();

            props.remove("Class");
            props.remove("Body");
            props.remove("Error");

            return props.toArray(new String[props.size()]);
        }
    }

    @GetMapping(
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @ResponseBody
    protected RestWrapper handleObjectGetRestWrapper(
            @PathVariable(name = "request", required = false) String req,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "offset", required = false) Long offset,
            @RequestParam(name = "count", required = false) Long count,
            @RequestParam(name = "live", required = false) Boolean live,
            @RequestParam(name = "fields", required = false) String fieldsSpec)
            throws Exception {
        MonitorQueryResults results =
                handleObjectGet(req, from, to, filter, order, offset, count, live, fieldsSpec);
        Object object = results.getResult();

        // HTML specific bits
        if (object instanceof RequestData) {
            return wrapObject((RequestData) object, RequestData.class);
        } else {
            final List<RequestData> requests = new ArrayList<>();
            BaseMonitorConverter.handleRequests(
                    object,
                    new RequestDataVisitor() {
                        public void visit(RequestData data, Object... aggregates) {
                            requests.add(data);
                        }
                    },
                    monitor);
            return wrapList(requests, RequestData.class);
        }
    }

    /**
     * Template method to get a custom template name
     *
     * @param o The object being serialized.
     */
    protected String getTemplateName(Object o) {
        if (o instanceof RequestData) {
            return "request.html";
        } else {
            return "requests.html";
        }
    }

    @GetMapping(produces = {CSV_MEDIATYPE_VALUE, EXCEL_MEDIATYPE_VALUE, ZIP_MEDIATYPE_VALUE})
    @ResponseBody
    protected MonitorQueryResults handleObjectGet(
            @PathVariable(name = "request", required = false) String req,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "offset", required = false) Long offset,
            @RequestParam(name = "count", required = false) Long count,
            @RequestParam(name = "live", required = false) Boolean live,
            @RequestParam(name = "fields", required = false) String fieldsSpec)
            throws Exception {
        String[] fields = getFields(fieldsSpec);

        if (req == null) {
            Query q =
                    new Query()
                            .between(
                                    from != null ? parseDate(from) : null,
                                    to != null ? parseDate(to) : null);

            // filter
            if (filter != null) {
                try {
                    parseFilter(filter, q);
                } catch (Exception e) {
                    throw new RestException(
                            "Error parsing filter " + filter, HttpStatus.BAD_REQUEST, e);
                }
            }

            // sorting
            String sortBy;
            SortOrder sortOrder;
            if (order != null) {
                int semi = order.indexOf(';');
                if (semi != -1) {
                    String[] split = order.split(";");
                    sortBy = split[0];
                    sortOrder = SortOrder.valueOf(split[1]);
                } else {
                    sortBy = order;
                    sortOrder = SortOrder.ASC;
                }

                q.sort(sortBy, sortOrder);
            }

            // limit offset
            q.page(offset, count);

            // live?
            if (live != null) {
                if (live) {
                    q.filter(
                            "status",
                            Arrays.asList(
                                    org.geoserver.monitor.RequestData.Status.RUNNING,
                                    org.geoserver.monitor.RequestData.Status.WAITING,
                                    org.geoserver.monitor.RequestData.Status.CANCELLING),
                            Comparison.IN);
                } else {
                    q.filter(
                            "status",
                            Arrays.asList(
                                    org.geoserver.monitor.RequestData.Status.FINISHED,
                                    org.geoserver.monitor.RequestData.Status.FAILED),
                            Comparison.IN);
                }
            }

            return new MonitorQueryResults(q, fields, monitor);
        } else {
            // return the individual
            RequestData data = monitor.getDAO().getRequest(Long.parseLong(req));
            if (data == null) {
                throw new ResourceNotFoundException("No such request" + req);
            }
            return new MonitorQueryResults(data, fields, monitor);
        }
    }

    Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
        } catch (ParseException e) {
            return Converters.convert(s, Date.class);
        }
    }

    void parseFilter(String filter, Query q) {
        for (String s : filter.split(";")) {
            if ("".equals(s.trim())) continue;
            String[] split = s.split(":");

            String left = split[0];
            Object right = split[2];
            if (right.toString().contains(",")) {
                List<Object> list = new ArrayList<>();
                for (String t : right.toString().split(",")) {
                    list.add(parseProperty(left, t));
                }
                right = list;
            } else {
                right = parseProperty(left, right.toString());
            }

            q.and(left, right, Comparison.valueOf(split[1]));
        }
    }

    Object parseProperty(String property, String value) {
        if ("status".equals(property)) {
            return org.geoserver.monitor.RequestData.Status.valueOf(value);
        }

        return value;
    }

    @DeleteMapping
    public void handleObjectDelete(@PathVariable(name = "request", required = false) String req) {
        if (req == null) {
            monitor.getDAO().clear();
        } else {
            throw new RestException(
                    "Cannot delete a specific request", HttpStatus.METHOD_NOT_ALLOWED);
        }
    }
}
