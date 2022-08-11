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

package org.geoserver.monitor.web;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.web.GeoServerApplication;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;

public abstract class ActivityChartBasePanel extends Panel {

    private static final long serialVersionUID = -2436197080363116473L;

    Date from;
    Date to;
    NonCachingImage chartImage;

    public ActivityChartBasePanel(String id, Monitor monitor) {
        super(id);

        Date[] range = getDateRange();

        BufferedDynamicImageResource resource = queryAndRenderChart(monitor, range);
        add(chartImage = new NonCachingImage("chart", resource));
        chartImage.setOutputMarkupId(true);

        Form<?> form = new Form<>("form");
        add(form);

        from = new Date(range[0].getTime());
        to = new Date(range[1].getTime());

        form.add(
                new DateTimeField("from", new PropertyModel<>(this, "from")) {
                    private static final long serialVersionUID = -6541833048507323265L;

                    @Override
                    protected boolean use12HourFormat() {
                        return false;
                    }
                });
        form.add(
                new DateTimeField("to", new PropertyModel<>(this, "to")) {
                    private static final long serialVersionUID = 1306927761884039503L;

                    @Override
                    protected boolean use12HourFormat() {
                        return false;
                    }
                });

        form.add(
                new AjaxButton("refresh") {
                    private static final long serialVersionUID = -6954067333262732996L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        Monitor monitor =
                                ((GeoServerApplication) getApplication())
                                        .getBeanOfType(Monitor.class);

                        Date[] range = new Date[] {from, to};

                        chartImage.setImageResource(queryAndRenderChart(monitor, range));
                        target.add(chartImage);
                    }
                });
    }

    BufferedDynamicImageResource queryAndRenderChart(Monitor monitor, Date[] range) {
        Query q = new Query();
        q.properties("startTime").between(range[0], range[1]);

        DataGatherer gatherer = new DataGatherer();
        monitor.query(q, gatherer);

        HashMap<RegularTimePeriod, Integer> data = gatherer.getData();

        Class<?> timeUnitClass = getTimePeriod(range[0]).getClass();
        TimeSeries series = new TimeSeries("foo", timeUnitClass);
        for (Map.Entry<RegularTimePeriod, Integer> d : data.entrySet()) {
            series.add(new TimeSeriesDataItem(d.getKey(), d.getValue()));
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        final JFreeChart chart =
                createTimeSeriesChart(
                        getChartTitle(range),
                        "Time (" + timeUnitClass.getSimpleName() + ")",
                        "Requests",
                        dataset);

        BufferedDynamicImageResource resource = new BufferedDynamicImageResource();
        resource.setImage(chart.createBufferedImage(700, 500));
        return resource;
    }

    JFreeChart createTimeSeriesChart(
            String title, String timeAxisLabel, String valueAxisLabel, XYDataset dataset) {

        ValueAxis timeAxis = new DateAxis(timeAxisLabel);
        timeAxis.setLowerMargin(0.02); // reduce the default margins
        timeAxis.setUpperMargin(0.02);
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false); // override default

        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        JFreeChart chart = new JFreeChart(plot);

        // TextTitle t = new TextTitle(title);
        // t.setTextAlignment(HorizontalAlignment.LEFT);

        // chart.setTitle(t);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setAntiAlias(true);
        chart.clearSubtitles();

        return chart;
    }

    class DataGatherer implements RequestDataVisitor {

        HashMap<RegularTimePeriod, Integer> data = new HashMap<>();

        public void visit(RequestData r, Object... aggregates) {
            RegularTimePeriod period = getTimePeriod(r.getStartTime());
            Integer count = data.get(period);

            if (count == null) {
                count = Integer.valueOf(1);
            } else {
                count = Integer.valueOf(count.intValue() + 1);
            }

            data.put(period, count);
        }

        public HashMap<RegularTimePeriod, Integer> getData() {
            return data;
        }
    }

    protected String getChartTitle(Date[] range) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return "Activity " + format.format(range[0]) + " - " + format.format(range[1]);
    }

    protected abstract Date[] getDateRange();

    protected abstract RegularTimePeriod getTimePeriod(Date time);
}
