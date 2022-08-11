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

package org.geoserver.wps.web;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.wps.MonkeyProcess;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geotools.data.Query;
import org.geotools.process.Processors;
import org.junit.Test;
import org.w3c.dom.Document;

public class ProcessStatusPageTest extends WPSPagesTestSupport {

    static {
        Processors.addProcessFactory(MonkeyProcess.getFactory());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("feature", "http://geoserver.sf.net");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Test
    public void test() throws Exception {
        login();

        // submit a monkey process
        String request =
                "wps?service=WPS&version=1.0.0&request=Execute&Identifier=gs:Monkey&storeExecuteResponse=true&DataInputs="
                        + URLEncoder.encode("id=x2", "ASCII");
        Document dom = getAsDOM(request);
        // print(dom);
        assertXpathExists("//wps:ProcessAccepted", dom);

        MonkeyProcess.progress("x2", 10.0f, true);

        // start the page, should have one process running
        tester.startPage(new ProcessStatusPage());
        // print(tester.getLastRenderedPage(), true, true);
        tester.assertLabel("table:listContainer:items:1:itemProperties:3:component", "gs:Monkey");
        tester.assertLabel("table:listContainer:items:1:itemProperties:5:component", "RUNNING");
        List<ExecutionStatus> executions = getItems();
        assertEquals(1, executions.size());
        ExecutionStatus status = executions.get(0);
        DateFormat df =
                new SimpleDateFormat(
                        "E, d MMM yyyy HH:mm:ss.SSS 'GMT'", tester.getSession().getLocale());
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        tester.assertLabel(
                "table:listContainer:items:1:itemProperties:7:component",
                df.format(status.getExpirationDate()));
        tester.assertLabel(
                "table:listContainer:items:1:itemProperties:8:component",
                df.format(status.getEstimatedCompletion()));
        tester.assertLabel(
                "table:listContainer:items:1:itemProperties:9:component",
                df.format(status.getNextPoll()));

        // select the process and delete it
        GeoServerTablePanel<?> table =
                (GeoServerTablePanel<?>) tester.getComponentFromLastRenderedPage("table");
        table.selectIndex(0);
        tester.getComponentFromLastRenderedPage("headerPanel:dismissSelected").setEnabled(true);
        tester.clickLink("headerPanel:dismissSelected");
        // this submits the dialog
        tester.clickLink("dialog:dialog:content:form:submit", true);
        // this makes the dialog actually close
        tester.getComponentFromLastRenderedPage("dialog:dialog")
                .getBehaviors()
                .forEach(
                        b -> {
                            final String name = b.getClass().getSimpleName();
                            if (name.contains("WindowClosedBehavior")) {
                                tester.executeBehavior((AbstractAjaxBehavior) b);
                            }
                        });

        // check the table is refreshed and process is dismissing
        tester.assertComponentOnAjaxResponse("table");
        tester.assertLabel("table:listContainer:items:2:itemProperties:3:component", "gs:Monkey");
        tester.assertLabel("table:listContainer:items:2:itemProperties:5:component", "DISMISSING");

        // let the process exit to ensure clean shutdown
        MonkeyProcess.exit("x2", null, true);
    }

    protected List<ExecutionStatus> getItems() {
        ProcessStatusTracker tracker =
                GeoServerApplication.get().getBeanOfType(ProcessStatusTracker.class);
        return tracker.getStore().list(Query.ALL);
    }
}
