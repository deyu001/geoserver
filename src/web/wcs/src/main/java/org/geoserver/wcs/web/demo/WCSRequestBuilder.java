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

package org.geoserver.wcs.web.demo;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs.web.demo.GetCoverageRequest.Version;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.demo.DemoRequest;
import org.geoserver.web.demo.DemoRequestResponse;
import org.geoserver.web.demo.PlainCodePage;
import org.geotools.xml.transform.TransformerBase;

/**
 * Small embedded WCS client enabling users to build a wcs GetCoverage request (and as a side effect
 * also showing what capabilities and describe process would provide) using
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class WCSRequestBuilder extends GeoServerBasePage {

    ModalWindow responseWindow;

    WCSRequestBuilderPanel builder;

    public WCSRequestBuilder() {
        // the form
        Form form = new Form("form");
        add(form);

        // the actual request builder component
        builder = new WCSRequestBuilderPanel("requestBuilder", new GetCoverageRequest());
        form.add(builder);

        // the xml popup window
        final ModalWindow xmlWindow = new ModalWindow("xmlWindow");
        add(xmlWindow);
        xmlWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        return new PlainCodePage(xmlWindow, responseWindow, getRequestXML());
                    }
                });

        // the output response window
        responseWindow = new ModalWindow("responseWindow");
        add(responseWindow);
        // responseWindow.setPageMapName("demoResponse");
        responseWindow.setCookieName("demoResponse");

        responseWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        DemoRequest request = new DemoRequest(null);
                        HttpServletRequest http = GeoServerApplication.get().servletRequest();
                        String url =
                                ResponseUtils.buildURL(
                                        ResponseUtils.baseURL(http),
                                        "ows",
                                        Collections.singletonMap("strict", "true"),
                                        URLType.SERVICE);
                        request.setRequestUrl(url);
                        request.setRequestBody((String) responseWindow.getDefaultModelObject());
                        return new DemoRequestResponse(new Model<>(request));
                    }
                });

        form.add(
                new AjaxSubmitLink("execute") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        responseWindow.setDefaultModel(new Model<>(getRequestXML()));
                        responseWindow.show(target);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
                        super.onError(target, form);
                        target.add(builder.getFeedbackPanel());
                    }
                });

        form.add(
                new AjaxSubmitLink("executeXML") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        try {
                            getRequestXML();
                            xmlWindow.show(target);
                        } catch (Exception e) {
                            error(e.getMessage());
                            addFeedbackPanels(target);
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
                        addFeedbackPanels(target);
                    }
                });
    }

    String getRequestXML() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        TransformerBase tx;
        if (builder.getCoverage.version == Version.v1_0_0) {
            tx = new WCS10GetCoverageTransformer(getCatalog());
        } else {
            CoverageResponseDelegateFinder responseFactory =
                    (CoverageResponseDelegateFinder)
                            getGeoServerApplication().getBean("coverageResponseDelegateFactory");
            tx = new WCS11GetCoverageTransformer(getCatalog(), responseFactory);
        }

        try {
            tx.setIndentation(2);
            tx.transform(builder.getCoverageRequest(), out);
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "Error generating xml request", e);
            error(e);
        }
        return out.toString();
    }
}
