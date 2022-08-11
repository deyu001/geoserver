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

package org.geoserver.web.data.layer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.xml.namespace.QName;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageView;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class CoverageViewEditorTest extends GeoServerWicketTestSupport {

    private static QName TIME_RANGES =
            new QName(MockData.DEFAULT_URI, "timeranges", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // add raster file to perform some tests
        testData.addRasterLayer(
                TIME_RANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());
        testData.addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
    }

    @Test
    public void testSingleBandsIndexIsNotVisible() throws Exception {
        // perform the login as administrator
        login();
        // opening the new coverage view page
        CoverageViewNewPage newPage =
                new CoverageViewNewPage(MockData.DEFAULT_PREFIX, "timeranges", null, null);
        tester.startPage(newPage);
        tester.assertComponent("form:coverages:outputBandsChoice", ListMultipleChoice.class);
        // let's see if we have the correct components instantiated
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:coverages", CoverageViewEditor.class);
        tester.assertComponent("form:coverages:coveragesChoice", ListMultipleChoice.class);
        tester.assertComponent("form:coverages:outputBandsChoice", ListMultipleChoice.class);
        tester.assertComponent("form:coverages:addBand", Button.class);
        // check the available bands names without any selected band
        CoverageViewEditor coverageViewEditor =
                (CoverageViewEditor) tester.getComponentFromLastRenderedPage("form:coverages");
        coverageViewEditor.setModelObject(null);
        ListMultipleChoice availableBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:coveragesChoice");
        ListMultipleChoice selectedBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:outputBandsChoice");
        // select the first band
        FormTester formTester = tester.newFormTester("form");
        formTester.selectMultiple("coverages:coveragesChoice", new int[] {0});
        tester.executeAjaxEvent("form:coverages:addBand", "click");
        // check that the coverage name contains the band index
        assertThat(availableBands.getChoices().size(), is(1));
        assertThat(availableBands.getChoices().get(0), is("time_domainsRanges"));
        assertThat(selectedBands.getChoices().size(), is(1));
        CoverageView.CoverageBand selectedBand =
                (CoverageView.CoverageBand) selectedBands.getChoices().get(0);
        assertThat(selectedBand.getDefinition(), is("time_domainsRanges"));
        // set a name and submit
        formTester.setValue("name", "bands_index_coverage_test");
        formTester.submit("save");
    }

    @Test
    public void testMultiBandsIndexIsVisible() throws Exception {
        // perform the login as administrator
        login();
        // opening the new coverage view page
        CoverageViewNewPage newPage =
                new CoverageViewNewPage(
                        MockData.TASMANIA_BM.getPrefix(),
                        MockData.TASMANIA_BM.getLocalPart(),
                        null,
                        null);
        tester.startPage(newPage);
        tester.assertComponent("form:coverages:outputBandsChoice", ListMultipleChoice.class);
        // check the available bands names without any selected band
        CoverageViewEditor coverageViewEditor =
                (CoverageViewEditor) tester.getComponentFromLastRenderedPage("form:coverages");
        coverageViewEditor.setModelObject(null);
        ListMultipleChoice availableBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:coveragesChoice");
        ListMultipleChoice selectedBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:outputBandsChoice");
        // select the first band
        FormTester formTester = tester.newFormTester("form");
        formTester.selectMultiple("coverages:coveragesChoice", new int[] {0});
        tester.executeAjaxEvent("form:coverages:addBand", "click");
        // check that the coverage name contains the band index
        assertThat(availableBands.getChoices().size(), is(3));
        assertThat(availableBands.getChoices().get(0), is("tazbm@0"));
        assertThat(selectedBands.getChoices().size(), is(1));
        CoverageView.CoverageBand selectedBand =
                (CoverageView.CoverageBand) selectedBands.getChoices().get(0);
        assertThat(selectedBand.getDefinition(), is("tazbm@0"));
        // set a name and submit
        formTester.setValue("name", "bands_index_coverage_test");
        formTester.submit("save");
    }
}
