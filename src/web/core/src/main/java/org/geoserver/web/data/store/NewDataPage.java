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

package org.geoserver.web.data.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataAccessFactory;
import org.h2.store.DataPage;
import org.opengis.coverage.grid.Format;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Page that presents a list of vector and raster store types available in the classpath in order to
 * choose what kind of data source to create, as well as which workspace to create the store in.
 *
 * <p>Meant to be called by {@link DataPage} when about to add a new datastore or coverage.
 *
 * @author Gabriel Roldan
 */
@SuppressWarnings("serial")
public class NewDataPage extends GeoServerSecuredPage {

    // do not access directly, it is transient and the instance can be the de-serialized version
    private transient Map<String, DataAccessFactory> dataStores = getAvailableDataStores();

    // do not access directly, it is transient and the instance can be the de-serialized version
    private transient Map<String, Format> coverages = getAvailableCoverageStores();

    /**
     * Creates the page components to present the list of available vector and raster data source
     * types
     */
    public NewDataPage() {

        final boolean thereAreWorkspaces = !getCatalog().getWorkspaces().isEmpty();

        if (!thereAreWorkspaces) {
            super.error(new ResourceModel("NewDataPage.noWorkspacesErrorMessage").getObject());
        }

        final Form storeForm = new Form("storeForm");
        add(storeForm);

        final ArrayList<String> sortedDsNames = new ArrayList<>(getAvailableDataStores().keySet());
        Collections.sort(sortedDsNames);

        final CatalogIconFactory icons = CatalogIconFactory.get();
        final ListView<String> dataStoreLinks =
                new ListView<String>("vectorResources", sortedDsNames) {
                    @Override
                    protected void populateItem(ListItem item) {
                        final String dataStoreFactoryName = item.getDefaultModelObjectAsString();
                        final DataAccessFactory factory =
                                getAvailableDataStores().get(dataStoreFactoryName);
                        final String description = factory.getDescription();
                        SubmitLink link;
                        link =
                                new SubmitLink("resourcelink") {
                                    @Override
                                    public void onSubmit() {
                                        setResponsePage(
                                                new DataAccessNewPage(dataStoreFactoryName));
                                    }
                                };
                        link.setEnabled(thereAreWorkspaces);
                        link.add(new Label("resourcelabel", dataStoreFactoryName));
                        item.add(link);
                        item.add(new Label("resourceDescription", description));
                        Image icon = new Image("storeIcon", icons.getStoreIcon(factory.getClass()));
                        // TODO: icons could provide a description too to be used in alt=...
                        icon.add(new AttributeModifier("alt", new Model<>("")));
                        item.add(icon);
                    }
                };

        final List<String> sortedCoverageNames = new ArrayList<>();
        sortedCoverageNames.addAll(getAvailableCoverageStores().keySet());
        Collections.sort(sortedCoverageNames);

        final ListView<String> coverageLinks =
                new ListView<String>("rasterResources", sortedCoverageNames) {
                    @Override
                    protected void populateItem(ListItem item) {
                        final String coverageFactoryName = item.getDefaultModelObjectAsString();
                        final Map<String, Format> coverages = getAvailableCoverageStores();
                        Format format = coverages.get(coverageFactoryName);
                        final String description = format.getDescription();
                        SubmitLink link;
                        link =
                                new SubmitLink("resourcelink") {
                                    @Override
                                    public void onSubmit() {
                                        setResponsePage(
                                                new CoverageStoreNewPage(coverageFactoryName));
                                    }
                                };
                        link.setEnabled(thereAreWorkspaces);
                        link.add(new Label("resourcelabel", coverageFactoryName));
                        item.add(link);
                        item.add(new Label("resourceDescription", description));
                        Image icon = new Image("storeIcon", icons.getStoreIcon(format.getClass()));
                        // TODO: icons could provide a description too to be used in alt=...
                        icon.add(new AttributeModifier("alt", new Model<>("")));
                        item.add(icon);
                    }
                };

        final List<OtherStoreDescription> otherStores = getOtherStores();

        final ListView<OtherStoreDescription> otherStoresLinks =
                new ListView<OtherStoreDescription>("otherStores", otherStores) {
                    @Override
                    protected void populateItem(ListItem item) {
                        final OtherStoreDescription store =
                                (OtherStoreDescription) item.getModelObject();
                        SubmitLink link;
                        link =
                                new SubmitLink("resourcelink") {
                                    @Override
                                    public void onSubmit() {
                                        setResponsePage(store.configurationPage);
                                    }
                                };
                        link.setEnabled(thereAreWorkspaces);
                        link.add(
                                new Label(
                                        "resourcelabel",
                                        new ParamResourceModel(
                                                "other." + store.key, NewDataPage.this)));
                        item.add(link);
                        item.add(
                                new Label(
                                        "resourceDescription",
                                        new ParamResourceModel(
                                                "other." + store.key + ".description",
                                                NewDataPage.this)));
                        Image icon = new Image("storeIcon", store.icon);
                        // TODO: icons could provide a description too to be used in alt=...
                        icon.add(new AttributeModifier("alt", new Model<>("")));
                        item.add(icon);
                    }
                };

        storeForm.add(dataStoreLinks);
        storeForm.add(coverageLinks);
        storeForm.add(otherStoresLinks);
    }

    /** @return the name/description set of available datastore factories */
    private Map<String, DataAccessFactory> getAvailableDataStores() {
        // dataStores is transient, a back button may get us to the serialized version so check for
        // it
        if (dataStores == null) {
            final Iterator<DataAccessFactory> availableDataStores;
            availableDataStores = DataStoreUtils.getAvailableDataStoreFactories().iterator();

            Map<String, DataAccessFactory> storeNames = new HashMap<>();

            while (availableDataStores.hasNext()) {
                DataAccessFactory factory = availableDataStores.next();
                if (factory.getDisplayName() != null) {
                    storeNames.put(factory.getDisplayName(), factory);
                }
            }
            dataStores = storeNames;
        }
        return dataStores;
    }

    /** @return the name/description set of available raster formats */
    private Map<String, Format> getAvailableCoverageStores() {
        if (coverages == null) {
            Format[] availableFormats = GridFormatFinder.getFormatArray();
            Map<String, Format> formatNames = new HashMap<>();
            for (Format format : availableFormats) {
                formatNames.put(format.getName(), format);
            }
            coverages = formatNames;
        }
        return coverages;
    }

    private List<OtherStoreDescription> getOtherStores() {
        List<OtherStoreDescription> stores = new ArrayList<>();
        PackageResourceReference wmsIcon =
                new PackageResourceReference(
                        GeoServerApplication.class, "img/icons/geosilk/server_map.png");
        stores.add(new OtherStoreDescription("wms", wmsIcon, WMSStoreNewPage.class));
        stores.add(new OtherStoreDescription("wmts", wmsIcon, WMTSStoreNewPage.class));

        return stores;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    /** Provides a description for a store that is not a vector nor a raster data source */
    static class OtherStoreDescription implements Serializable {
        String key;

        PackageResourceReference icon;

        Class<? extends Page> configurationPage;

        public OtherStoreDescription(
                String key,
                PackageResourceReference icon,
                Class<? extends Page> configurationPage) {
            super();
            this.key = key;
            this.icon = icon;
            this.configurationPage = configurationPage;
        }
    }
}
