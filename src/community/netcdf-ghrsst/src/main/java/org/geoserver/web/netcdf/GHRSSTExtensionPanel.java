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


package org.geoserver.web.netcdf;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wcs.responses.GHRSSTEncoder;
import org.geoserver.web.util.MetadataMapModel;

/** Configuration panel for GHRSST settings */
public class GHRSSTExtensionPanel extends NetCDFExtensionPanel {

    static final List<String> RDACS =
            Arrays.asList(
                    "ABOM",
                    "CMC",
                    "DMI",
                    "EUR",
                    "GOS",
                    "JPL",
                    "JPL_OUROCEAN",
                    "METNO",
                    "MYO",
                    "NAVO",
                    "NCDC",
                    "NEODAAS",
                    "NOC",
                    "NODC",
                    "OSDPD",
                    "OSISAF",
                    "REMSS",
                    "RSMAS",
                    "UKMO",
                    "UPA",
                    "ESACCI",
                    "JAXA");

    static final List<String> PROCESSING_LEVELS =
            Arrays.asList("L0", "L1A", "L1B", "L2P", "L3U", "L3C", "L3S", "L4");

    static final List<String> SST_TYPES =
            Arrays.asList("SSTint", "SSTskin", "SSTsubskin", "SSTdepth", "SSTfnd", "SSTblend");

    /**
     * Product Strings. This list is incomplete and conteplates only L3, the actual list is much
     * larger and depends on the RDAC, so some complex autocomplete code should be written instead
     * (TODO)
     */
    static final List<String> PRODUCT_STRINGS =
            Arrays.asList(
                    "AVHRR7_D",
                    "AVHRR9_D",
                    "AVHRR10_D",
                    "AVHRR11_D",
                    "AVHRR12_D",
                    "AVHRR14_D",
                    "AVHRR15_D",
                    "AVHRR16_D",
                    "AVHRR17_D",
                    "AVHRR18_D",
                    "AVHRR19_D",
                    "AVHRR_Pathfinder",
                    "AVHRR_METOP_A",
                    "AATSR",
                    "ATSR1",
                    "ATSR2");

    private final CheckBox enabled;
    private final NetCDFPanel parent;
    private final AutoCompleteTextField<String> processingLevel;
    private final AutoCompleteTextField<String> sstType;
    private final AutoCompleteTextField<String> productString;
    private final AutoCompleteTextField<String> rdac;

    public GHRSSTExtensionPanel(String id, IModel<?> model, NetCDFPanel parent) {
        super(id, model);
        this.parent = parent;

        PropertyModel<MetadataMap> metadataModel = new PropertyModel<>(model, "metadata");
        enabled =
                new CheckBox(
                        "enabled",
                        new MetadataMapModel<>(
                                metadataModel, GHRSSTEncoder.SETTINGS_KEY, Boolean.class));
        add(enabled);

        WebMarkupContainer settings = new WebMarkupContainer("settings");
        settings.setOutputMarkupId(true);
        add(settings);
        this.rdac =
                getAutocompleter(
                        "rdac",
                        new MetadataMapModel<>(
                                metadataModel, GHRSSTEncoder.SETTINGS_RDAC_KEY, String.class),
                        RDACS);
        rdac.setRequired(true);
        settings.add(rdac);
        this.processingLevel =
                getAutocompleter(
                        "processingLevel",
                        new MetadataMapModel<>(
                                metadataModel,
                                GHRSSTEncoder.SETTINGS_PROCESSING_LEVEL_KEY,
                                String.class),
                        PROCESSING_LEVELS);
        processingLevel.setRequired(true);
        settings.add(processingLevel);
        this.sstType =
                getAutocompleter(
                        "sstType",
                        new MetadataMapModel<>(
                                metadataModel, GHRSSTEncoder.SETTINGS_SST_TYPE, String.class),
                        SST_TYPES);
        sstType.setRequired(true);
        settings.add(sstType);
        this.productString =
                getAutocompleter(
                        "productString",
                        new MetadataMapModel<>(
                                metadataModel, GHRSSTEncoder.SETTINGS_PRODUCT_STRING, String.class),
                        PRODUCT_STRINGS);
        productString.setRequired(true);
        settings.add(productString);

        // enable/disable on open
        settings.visitChildren(
                (component, visit) -> {
                    component.setEnabled(Boolean.TRUE.equals(enabled.getModelObject()));
                });

        enabled.add(
                new AjaxEventBehavior("change") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        enabled.processInput();
                        boolean enableSettings = Boolean.TRUE.equals(enabled.getModelObject());
                        settings.visitChildren(
                                (component, visit) -> {
                                    component.setEnabled(enableSettings);
                                });
                        target.add(settings);
                    }
                });
    }

    AutoCompleteTextField<String> getAutocompleter(
            String id, IModel<String> model, List<String> choices) {
        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowCompleteListOnFocusGain(true);
        settings.setShowListOnEmptyInput(true);
        return new AutoCompleteTextField<String>(id, model, settings) {
            @Override
            protected Iterator<String> getChoices(String input) {
                if (input == null || input.trim().length() == 0) {
                    return choices.iterator();
                }

                String reference = input.trim().toLowerCase();
                return choices.stream().filter(c -> c.toLowerCase().contains(reference)).iterator();
            }
        };
    }

    @Override
    public void convertInput(NetCDFSettingsContainer settings) {
        enabled.processInput();
        rdac.processInput();
        processingLevel.processInput();
        sstType.processInput();
        productString.processInput();
        settings.getMetadata().put(GHRSSTEncoder.SETTINGS_KEY, enabled.getModelObject());
        settings.getMetadata().put(GHRSSTEncoder.SETTINGS_RDAC_KEY, rdac.getModelObject());
        settings.getMetadata()
                .put(GHRSSTEncoder.SETTINGS_PROCESSING_LEVEL_KEY, processingLevel.getModelObject());
        settings.getMetadata().put(GHRSSTEncoder.SETTINGS_SST_TYPE, sstType.getModelObject());
        settings.getMetadata()
                .put(GHRSSTEncoder.SETTINGS_PRODUCT_STRING, productString.getModelObject());
    }
}
