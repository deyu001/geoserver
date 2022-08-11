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

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geoserver.wps.web.FilteredProcessesProvider.FilteredProcess;
import org.geotools.feature.NameImpl;
import org.geotools.process.geometry.GeometryProcessFactory;
import org.geotools.util.NumberRange;
import org.junit.Test;

public class ProcessSelectionPageTest extends WPSPagesTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add some limits to the processes
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);

        ProcessGroupInfo geoGroup = getGeoGroup(wps.getProcessGroups());

        // for the buffer process
        ProcessInfo buffer = new ProcessInfoImpl();
        buffer.setEnabled(true);
        buffer.setName(new NameImpl("geo", "buffer"));
        buffer.getValidators().put("geom", new MaxSizeValidator(1));
        buffer.getValidators()
                .put(
                        "distance",
                        new NumberRangeValidator(new NumberRange<>(Double.class, 0d, 100d)));
        buffer.getValidators()
                .put(
                        "quadrantSegments",
                        new NumberRangeValidator(new NumberRange<>(Integer.class, 2, 20)));
        geoGroup.getFilteredProcesses().add(buffer);

        // save
        getGeoServer().save(wps);
    }

    @Test
    public void test() throws Exception {
        login();
        WPSInfo wps = getGeoServerApplication().getGeoServer().getService(WPSInfo.class);
        ProcessGroupInfo pgi = getGeoGroup(wps.getProcessGroups());

        // start the page
        WPSAccessRulePage accessRulePage = tester.startPage(new WPSAccessRulePage());
        tester.startPage(new ProcessSelectionPage(accessRulePage, pgi));

        // print(selectionPage, true, true);

        // grab the table and check its contents
        @SuppressWarnings("unchecked")
        DataView<OddEvenItem> datas =
                (DataView)
                        tester.getComponentFromLastRenderedPage(
                                "form:selectionTable:listContainer:items");
        for (Component c : datas) {
            OddEvenItem item = (OddEvenItem) c;
            FilteredProcess fp = (FilteredProcess) item.getDefaultModelObject();

            Component validatedLabel = item.get("itemProperties:5:component");
            if (fp.getName().getLocalPart().equals("buffer")) {
                assertEquals("*", validatedLabel.getDefaultModelObject());
            } else {
                assertEquals("", validatedLabel.getDefaultModelObject());
            }
        }
    }

    private ProcessGroupInfo getGeoGroup(List<ProcessGroupInfo> processGroups) {
        for (ProcessGroupInfo pgi : processGroups) {
            if (pgi.getFactoryClass().equals(GeometryProcessFactory.class)) {
                return pgi;
            }
        }

        return null;
    }
}
