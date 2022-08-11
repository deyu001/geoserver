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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/** @author Martin Davis OpenGeo */
public class WPSRequestBuilderTest extends GeoServerWicketTestSupport {

    @Test
    public void testJTSAreaWorkflow() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSRequestBuilder());

        tester.assertComponent("form:requestBuilder:process", DropDownChoice.class);

        // look for JTS area
        DropDownChoice choice =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage("form:requestBuilder:process");
        int index = -1;
        final List choices = choice.getChoices();
        for (Object o : choices) {
            if (o.equals("JTS:area")) {
                index = 0;
                break;
            }
        }

        // choose a process
        FormTester form = tester.newFormTester("form");
        form.select("requestBuilder:process", index);
        tester.executeAjaxEvent("form:requestBuilder:process", "change");

        // print(tester.getComponentFromLastRenderedPage("form"), true, true);

        // check process description
        tester.assertModelValue("form:requestBuilder:process", "JTS:area");
        Label label =
                (Label)
                        tester.getComponentFromLastRenderedPage(
                                "form:requestBuilder:descriptionContainer:processDescription");
        assertTrue(label.getDefaultModelObjectAsString().contains("area"));

        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:mime",
                DropDownChoice.class);
        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                TextArea.class);

        // fill in the params
        form = tester.newFormTester("form");
        form.select("requestBuilder:inputContainer:inputs:0:paramValue:editor:mime", 2);
        form.setValue(
                "requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                "POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))");
        form.submit();
        tester.clickLink("form:execute", true);

        // print(tester.getLastRenderedPage(), true, true);

        assertTrue(
                tester.getComponentFromLastRenderedPage("responseWindow")
                        .getDefaultModelObjectAsString()
                        .contains("wps:Execute"));

        // unfortunately the wicket tester does not allow us to get work with the popup window
        // contents,
        // as that requires a true browser to execute the request
    }

    /** Tests initializing page to specific process via name request parameter. */
    @Test
    public void testNameRequest() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSRequestBuilder(new PageParameters().add("name", "JTS:area")));

        tester.assertComponent("form:requestBuilder:process", DropDownChoice.class);

        // check process description
        tester.assertModelValue("form:requestBuilder:process", "JTS:area");

        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                TextArea.class);
    }

    @Test
    public void testGetRequestXMLWithEntity() throws Exception {
        login();
        WPSRequestBuilder builder =
                tester.startPage(
                        new WPSRequestBuilder(new PageParameters().add("name", "JTS:area")));
        String resource = getClass().getResource("secret.txt").toExternalForm();
        builder.builder
                .execute
                .inputs
                .get(0)
                .values
                .get(0)
                .setValue(
                        "<?xml version=\"1.0\"?>"
                                + "<!DOCTYPE foo [ "
                                + "<!ELEMENT foo ANY >"
                                + "<!ENTITY xxe SYSTEM \""
                                + resource
                                + "\" >]><foo>&xxe;</foo>");
        String executeXML = builder.getRequestXML();
        assertThat(executeXML, not(containsString("HELLO WORLD")));
    }
}
