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

package org.geoserver.web.demo;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.GeoServerBasePage;
import org.vfny.geoserver.wfs.servlets.TestWfsPost;

/**
 * An intermediate page used to submit a demo request to the {@link TestWfsPost /TestWfsPost}
 * servlet.
 *
 * <p>This page does not extend {@link GeoServerBasePage} since its just an intermediate form to
 * submit to the servlet.
 *
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.0.x
 */
public class DemoRequestResponse extends WebPage {

    /**
     * Fills out the form to be submitted to {@code TestWfsPost} with the properties from the {@code
     * DemoRequestModel} provided, and auto-submit the form on page load so the results get loaded
     * in the page body.
     *
     * @param model the demo request parameters holder, as a model for {@link DemoRequest}
     */
    public DemoRequestResponse(final IModel model) {
        // this page being in an IFrame needs to grap its own PageMap
        // in order not to share it with the parent page and thus be
        // marked as expired
        // JD: Couldn't find a replacement for PageMap, hopefully this is a non-issue
        // now, but won't know until we get things up and running with the new version
        // super(PageMap.forName("demoRequestResponse"));
        super(model);

        Form form = new Form("form");
        add(form);
        form.add(new HiddenField<String>("url", new PropertyModel<>(model, "requestUrl")));
        form.add(new TextArea<>("body", new PropertyModel<>(model, "requestBody")));
        form.add(new HiddenField<String>("username", new PropertyModel<>(model, "userName")));
        // [WICKET-6211] Wicket clears the password after submission, so we need to save as a string
        // now.
        HiddenField<String> passwordField =
                new HiddenField<String>(
                        "password", new Model<>(((DemoRequest) model.getObject()).getPassword())) {
                    @Override
                    protected void onDetach() {
                        // clear the password after we are done with it
                        clearInput();
                        if (getModel() != null) {
                            setModelObject(null);
                        }
                        super.onDetach();
                    }
                };

        form.add(passwordField);

        // override the action property of the form to submit to the TestWfsPost
        // servlet
        form.add(AttributeModifier.replace("action", "../../TestWfsPost"));

        // Set the same markup is as in the html page so wicket does not
        // generates
        // its own and the javascript code in the onLoad event for the <body>
        // element
        // finds out the form by id
        form.setMarkupId("form");
    }
}
