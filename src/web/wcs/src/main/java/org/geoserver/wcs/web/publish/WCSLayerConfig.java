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

package org.geoserver.wcs.web.publish;

import static org.geoserver.wcs.responses.AscCoverageResponseDelegate.ARCGRID_COVERAGE_FORMAT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SimpleChoiceRenderer;

/** A configuration panel for CoverageInfo properties that related to WCS publication */
public class WCSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 6120092654147588736L;

    private static final List<String> WCS_FORMATS =
            Arrays.asList(
                    "GIF",
                    "PNG",
                    "JPEG",
                    "TIFF",
                    "GEOTIFF",
                    "IMAGEMOSAIC",
                    ARCGRID_COVERAGE_FORMAT);
    private static final List<String> INTERPOLATIONS =
            Arrays.asList("nearest neighbor", "bilinear", "bicubic");

    private List<String> selectedRequestSRSs;
    private List<String> selectedResponseSRSs;
    private String newRequestSRS;
    private String newResponseSRS;

    public WCSLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        final CoverageInfo coverage = (CoverageInfo) getPublishedInfo().getResource();
        add(
                new ListMultipleChoice<>(
                        "requestSRS",
                        new PropertyModel<List<String>>(this, "selectedRequestSRSs"),
                        coverage.getRequestSRS()));

        add(new TextField<>("newRequestSRS", new PropertyModel<>(this, "newRequestSRS")));

        add(
                new Button("deleteSelectedRequestSRSs") {
                    private static final long serialVersionUID = 8363252127939759315L;

                    public void onSubmit() {
                        coverage.getRequestSRS().removeAll(selectedRequestSRSs);
                        selectedRequestSRSs.clear();
                    }
                });

        add(
                new Button("addNewRequestSRS") {
                    private static final long serialVersionUID = -3493317500980471055L;

                    public void onSubmit() {
                        coverage.getRequestSRS().add(newRequestSRS);
                        newRequestSRS = "";
                    }
                });

        add(
                new ListMultipleChoice<>(
                        "responseSRS",
                        new PropertyModel<List<String>>(this, "selectedResponseSRSs"),
                        coverage.getResponseSRS()));

        add(new TextField<>("newResponseSRS", new PropertyModel<>(this, "newResponseSRS")));

        add(
                new Button("deleteSelectedResponseSRSs") {
                    private static final long serialVersionUID = -8727831157546262491L;

                    public void onSubmit() {
                        coverage.getResponseSRS().removeAll(selectedResponseSRSs);
                        selectedResponseSRSs.clear();
                    }
                });

        add(
                new Button("addNewResponseSRS") {
                    private static final long serialVersionUID = -2888152896129259019L;

                    public void onSubmit() {
                        coverage.getResponseSRS().add(newResponseSRS);
                        newResponseSRS = "";
                    }
                });

        add(
                new DropDownChoice<>(
                        "defaultInterpolationMethod",
                        new PropertyModel<>(coverage, "defaultInterpolationMethod"),
                        new WCSInterpolationModel()));

        Palette<String> interpolationMethods =
                new Palette<String>(
                        "interpolationMethods",
                        LiveCollectionModel.list(
                                new PropertyModel<List<String>>(coverage, "interpolationMethods")),
                        new WCSInterpolationModel(),
                        new SimpleChoiceRenderer<>(),
                        7,
                        false) {
                    private static final long serialVersionUID = 6815545819673802290L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("InterpolationMethodsPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("InterpolationMethodsPalette.availableHeader"));
                    }
                };
        interpolationMethods.add(new DefaultTheme());
        add(interpolationMethods);

        // don't allow editing the native format
        TextField<String> nativeFormat =
                new TextField<>("nativeFormat", new PropertyModel<>(coverage, "nativeFormat"));
        nativeFormat.setEnabled(false);
        add(nativeFormat);

        Palette<String> formatPalette =
                new Palette<String>(
                        "formatPalette",
                        LiveCollectionModel.list(
                                new PropertyModel<List<String>>(coverage, "supportedFormats")),
                        new WCSFormatsModel(),
                        new SimpleChoiceRenderer<>(),
                        10,
                        false) {
                    private static final long serialVersionUID = -2463012775305597908L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId, new ResourceModel("FormatsPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId, new ResourceModel("FormatsPalette.availableHeader"));
                    }
                };
        formatPalette.add(new DefaultTheme());
        add(formatPalette);
    }

    static class WCSFormatsModel extends LoadableDetachableModel<ArrayList<String>> {

        private static final long serialVersionUID = 1802421566341456007L;

        WCSFormatsModel() {
            super(new ArrayList<>(WCS_FORMATS));
        }

        @Override
        protected ArrayList<String> load() {
            return new ArrayList<>(WCS_FORMATS);
        }
    }

    static class WCSInterpolationModel extends LoadableDetachableModel<ArrayList<String>> {

        private static final long serialVersionUID = 7328612985196203413L;

        WCSInterpolationModel() {
            super(new ArrayList<>(INTERPOLATIONS));
        }

        @Override
        protected ArrayList<String> load() {
            return new ArrayList<>(INTERPOLATIONS);
        }
    }
}
