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

package org.geoserver.wfs.web.publish;

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SRSListTextArea;

public class WFSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 4264296611272179367L;

    protected GeoServerDialog dialog;

    public WFSLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        TextField<Integer> maxFeatures =
                new TextField<>(
                        "perReqFeatureLimit", new PropertyModel<>(model, "resource.maxFeatures"));
        maxFeatures.add(RangeValidator.minimum(0));
        Border mfb = new FormComponentFeedbackBorder("perReqFeaturesBorder");
        mfb.add(maxFeatures);
        add(mfb);
        TextField<Integer> maxDecimals =
                new TextField<>("maxDecimals", new PropertyModel<>(model, "resource.numDecimals"));
        maxFeatures.add(RangeValidator.minimum(0));
        Border mdb = new FormComponentFeedbackBorder("maxDecimalsBorder");
        mdb.add(maxDecimals);
        add(mdb);
        CheckBox padWithZeros =
                new CheckBox("padWithZeros", new PropertyModel<>(model, "resource.padWithZeros"));
        Border pwzb = new FormComponentFeedbackBorder("padWithZerosBorder");
        pwzb.add(padWithZeros);
        add(pwzb);

        CheckBox forcedDecimal =
                new CheckBox("forcedDecimal", new PropertyModel<>(model, "resource.forcedDecimal"));
        Border fdb = new FormComponentFeedbackBorder("forcedDecimalBorder");
        fdb.add(forcedDecimal);
        add(fdb);
        CheckBox skipNumberMatched =
                new CheckBox(
                        "skipNumberMatched",
                        new PropertyModel<>(model, "resource.skipNumberMatched"));
        add(skipNumberMatched);

        // coordinates measures encoding
        CheckBox encodeMeasures =
                new CheckBox(
                        "encodeMeasures", new PropertyModel<>(model, "resource.encodeMeasures"));
        add(encodeMeasures);

        CheckBox complexToSimple =
                new CheckBox(
                        "complexToSimple",
                        new PropertyModel<>(model, "resource.simpleConversionEnabled"));
        Border complexToSimpleBorder = new FormComponentFeedbackBorder("complexToSimpleBorder");
        complexToSimpleBorder.add(complexToSimple);
        add(complexToSimpleBorder);

        // other srs list
        dialog = new GeoServerDialog("wfsDialog");
        add(dialog);
        PropertyModel<Boolean> overrideServiceSRSModel =
                new PropertyModel<>(model, "resource.overridingServiceSRS");
        final CheckBox overrideServiceSRS =
                new CheckBox("overridingServiceSRS", overrideServiceSRSModel);
        add(overrideServiceSRS);
        final WebMarkupContainer otherSrsContainer = new WebMarkupContainer("otherSRSContainer");
        otherSrsContainer.setOutputMarkupId(true);
        add(otherSrsContainer);
        final TextArea<List<String>> srsList =
                new SRSListTextArea(
                        "srs",
                        LiveCollectionModel.list(
                                new PropertyModel<List<String>>(model, "resource.responseSRS")));
        srsList.setOutputMarkupId(true);
        srsList.setVisible(Boolean.TRUE.equals(overrideServiceSRSModel.getObject()));
        otherSrsContainer.add(srsList);
        overrideServiceSRS.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = -6590810763209350915L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean visible = overrideServiceSRS.getConvertedInput();
                        srsList.setVisible(visible);
                        target.add(otherSrsContainer);
                    }
                });
        add(
                new AjaxLink<String>("skipNumberMatchedHelp") {
                    private static final long serialVersionUID = 9222171216768726057L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel(
                                        "skipNumberMatched", WFSLayerConfig.this, null),
                                new StringResourceModel(
                                        "skipNumberMatched.message", WFSLayerConfig.this, null));
                    }
                });
        add(
                new AjaxLink<String>("otherSRSHelp") {
                    private static final long serialVersionUID = -1239179491855142211L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel("otherSRS", WFSLayerConfig.this, null),
                                new StringResourceModel(
                                        "otherSRS.message", WFSLayerConfig.this, null));
                    }
                });
        add(
                new AjaxLink<String>("coordinatesEncodingHelp") {
                    private static final long serialVersionUID = 926171216768726057L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel(
                                        "coordinatesEncodingTitle", WFSLayerConfig.this, null),
                                new StringResourceModel(
                                        "coordinatesEncodingHelp.message",
                                        WFSLayerConfig.this,
                                        null));
                    }
                });
    }
}
