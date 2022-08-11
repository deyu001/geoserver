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

package org.geoserver.inspire.web;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.wfs.WFSInfo;

/** Panel for the service admin page to set the service INSPIRE extension preferences. */
public class InspireAdminPanel extends AdminPagePanel {

    private static final long serialVersionUID = -7670555379263411393L;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public InspireAdminPanel(final String id, final IModel<ServiceInfo> model) {
        super(id, model);

        MetadataMap serviceMetadata = model.getObject().getMetadata();

        String metadataURL = (String) serviceMetadata.get(SERVICE_METADATA_URL.key);
        boolean isDownloadService =
                model.getObject() instanceof WFSInfo || model.getObject() instanceof WCSInfo;
        UniqueResourceIdentifiers ids = null;
        if (isDownloadService) {
            ids =
                    serviceMetadata.get(
                            SPATIAL_DATASET_IDENTIFIER_TYPE.key, UniqueResourceIdentifiers.class);
        }
        if (!serviceMetadata.containsKey(CREATE_EXTENDED_CAPABILITIES.key)) {
            if (metadataURL == null || isDownloadService && (ids == null || ids.isEmpty())) {
                serviceMetadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
            } else {
                serviceMetadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
            }
        }

        PropertyModel<MetadataMap> metadata = new PropertyModel<>(model, "metadata");

        final CheckBox createInspireExtendedCapabilities =
                new CheckBox(
                        "createExtendedCapabilities",
                        new MetadataMapModel(
                                metadata, CREATE_EXTENDED_CAPABILITIES.key, Boolean.class));
        add(createInspireExtendedCapabilities);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final WebMarkupContainer configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        configs.setVisible(createInspireExtendedCapabilities.getModelObject());
        container.add(configs);

        createInspireExtendedCapabilities.add(
                new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        configs.setVisible(createInspireExtendedCapabilities.getModelObject());
                        target.add(container);
                    }
                });

        if (!model.getObject().getMetadata().containsKey(LANGUAGE.key)) {
            model.getObject().getMetadata().put(LANGUAGE.key, "eng");
        }
        configs.add(new LanguageDropDownChoice("language", new MapModel(metadata, LANGUAGE.key)));

        TextField metadataUrlField =
                new TextField("metadataURL", new MapModel(metadata, SERVICE_METADATA_URL.key));
        metadataUrlField.setRequired(true);
        FormComponentFeedbackBorder metadataURLBorder = new FormComponentFeedbackBorder("border");
        metadataURLBorder.add(metadataUrlField);
        configs.add(metadataURLBorder);
        metadataUrlField.add(
                new AttributeModifier(
                        "title", new ResourceModel("InspireAdminPanel.metadataURL.title")));

        final Map<String, String> mdUrlTypes = new HashMap<>();
        mdUrlTypes.put(
                "application/vnd.ogc.csw.GetRecordByIdResponse_xml", "CSW GetRecordById Response");
        mdUrlTypes.put("application/vnd.iso.19139+xml", "ISO 19139 ServiceMetadata record");

        IModel<String> urlTypeModel = new MapModel(metadata, SERVICE_METADATA_TYPE.key);

        IChoiceRenderer<String> urlTypeChoiceRenderer =
                new ChoiceRenderer<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(final String key) {
                        final String resourceKey =
                                "InspireAdminPanel.metadataURLType." + key; // as found in
                        // GeoServerApplication.properties
                        final String defaultValue = key;
                        final String displayValue =
                                new ResourceModel(resourceKey, defaultValue).getObject();
                        return displayValue;
                    }

                    @Override
                    public String getIdValue(final String key, int index) {
                        return key;
                    }
                };
        List<String> urlTypeChoices = new ArrayList<>(mdUrlTypes.keySet());
        DropDownChoice<String> serviceMetadataRecordType =
                new DropDownChoice<>(
                        "metadataURLType", urlTypeModel, urlTypeChoices, urlTypeChoiceRenderer);
        serviceMetadataRecordType.setNullValid(true);

        configs.add(serviceMetadataRecordType);

        // this is download service specific, will appear only if the service is
        // WFS or WCS
        WebMarkupContainer identifiersContainer =
                new WebMarkupContainer("datasetIdentifiersContainer");
        identifiersContainer.setVisible(isDownloadService);
        configs.add(identifiersContainer);
        IModel<UniqueResourceIdentifiers> sdiModel =
                new MetadataMapModel(
                        metadata,
                        SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                        UniqueResourceIdentifiers.class);
        UniqueResourceIdentifiersEditor identifiersEditor =
                new UniqueResourceIdentifiersEditor("spatialDatasetIdentifiers", sdiModel);
        identifiersContainer.add(identifiersEditor);
    }
}
