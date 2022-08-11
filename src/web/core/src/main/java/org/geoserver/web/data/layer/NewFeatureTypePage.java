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

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Builds a new feature type by having the user specify the attributes
 *
 * @author aaime
 */
@SuppressWarnings("serial")
public class NewFeatureTypePage extends GeoServerSecuredPage {

    public static final String DATASTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    String name;

    Form<?> form;

    AttributesProvider attributesProvider;

    GeoServerTablePanel<AttributeDescription> attributeTable;

    String storeId;

    public NewFeatureTypePage(PageParameters params) {
        this(params.get(WORKSPACE).toOptionalString(), params.get(DATASTORE).toString());
    }

    public NewFeatureTypePage(String workspaceName, String storeName) {
        DataStoreInfo di = getCatalog().getDataStoreByName(workspaceName, storeName);
        if (di == null) {
            throw new IllegalArgumentException(
                    "Could not find a "
                            + storeName
                            + " store in the "
                            + workspaceName
                            + " workspace");
        }
        this.storeId = di.getId();

        form = new Form<>("form");
        form.setOutputMarkupId(true);
        add(form);

        form.add(new TextField<>("name", new PropertyModel<>(this, "name")).setRequired(true));

        attributesProvider = new AttributesProvider();
        attributeTable =
                new GeoServerTablePanel<AttributeDescription>(
                        "attributes", attributesProvider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<AttributeDescription> itemModel,
                            Property<AttributeDescription> property) {
                        AttributeDescription att = itemModel.getObject();
                        if (property == AttributesProvider.NAME) {
                            Fragment f = new Fragment(id, "nameFragment", NewFeatureTypePage.this);
                            f.add(editAttributeLink(itemModel));
                            return f;
                        } else if (property == AttributesProvider.BINDING) {
                            return new Label(
                                    id, AttributeDescription.getLocalizedName(att.getBinding()));
                        } else if (property == AttributesProvider.CRS) {
                            if (att.getBinding() != null
                                    && Geometry.class.isAssignableFrom(att.getBinding())) {
                                try {
                                    Integer epsgCode = CRS.lookupEpsgCode(att.getCrs(), false);
                                    return new Label(id, "EPSG:" + epsgCode);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                return new Label(id, "");
                            }
                        } else if (property == AttributesProvider.SIZE) {
                            if (att.getBinding() != null && String.class.equals(att.getBinding())) {
                                return new Label(id, String.valueOf(att.getSize()));
                            } else {
                                return new Label(id, "");
                            }
                        } else if (property == AttributesProvider.UPDOWN) {
                            return upDownFragment(id, att);
                        }

                        return null;
                    }
                };
        attributeTable.setSortable(false);
        attributeTable.setFilterable(false);
        attributeTable.getBottomPager().setVisible(false);
        form.add(attributeTable);

        SubmitLink saveLink = saveLink();
        form.add(saveLink);
        form.setDefaultButton(saveLink);
        form.add(cancelLink());

        setHeaderPanel(headerPanel());
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                DataStore ds = null;
                DataStoreInfo dsInfo = null;
                try {
                    // basic checks
                    dsInfo = getCatalog().getDataStore(storeId);
                    ds = (DataStore) dsInfo.getDataStore(null);
                    if (Arrays.asList(ds.getTypeNames()).contains(name)) {
                        error(
                                new ParamResourceModel(
                                                "duplicateTypeName", this, dsInfo.getName(), name)
                                        .getString());
                        return;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (attributesProvider.getAttributes().size() == 0) {
                    error(new ParamResourceModel("noAttributes", this).getString());
                    return;
                }

                try {
                    SimpleFeatureType featureType = buildFeatureType();
                    ds.createSchema(featureType);

                    CatalogBuilder builder = new CatalogBuilder(getCatalog());
                    builder.setStore(dsInfo);
                    FeatureTypeInfo fti = builder.buildFeatureType(getFeatureSource(ds));
                    LayerInfo layerInfo = builder.buildLayer(fti);
                    setResponsePage(new ResourceConfigurationPage(layerInfo, true));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
                    error(
                            new ParamResourceModel("creationFailure", this, e.getMessage())
                                    .getString());
                }
            }
        };
    }

    FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(DataStore ds)
            throws IOException {
        try {
            return ds.getFeatureSource(name);
        } catch (IOException e) {
            // maybe it's Oracle?
            try {
                return ds.getFeatureSource(name.toUpperCase());
            } catch (Exception ora) {
                // nope, the reason was another one
                throw e;
            }
        }
    }

    SimpleFeatureType buildFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        for (AttributeDescription att : attributesProvider.getAttributes()) {
            if (att.getSize() > 0) {
                builder.length(att.getSize());
            }
            if (Geometry.class.isAssignableFrom(att.getBinding())) {
                builder.add(att.getName(), att.getBinding(), att.getCrs());
            } else {
                builder.add(att.getName(), att.getBinding());
            }
        }
        builder.setName(name);
        return builder.buildFeatureType();
    }

    private Link<Void> cancelLink() {
        return new Link<Void>("cancel") {

            @Override
            public void onClick() {
                doReturn(NewLayerPage.class);
            }
        };
    }

    Component editAttributeLink(final IModel<AttributeDescription> itemModel) {
        GeoServerAjaxFormLink link =
                new GeoServerAjaxFormLink("link") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        AttributeDescription attribute = itemModel.getObject();
                        setResponsePage(new AttributeEditPage(attribute, NewFeatureTypePage.this));
                    }
                };
        link.add(new Label("name", new PropertyModel<String>(itemModel, "name")));
        return link;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(
                new GeoServerAjaxFormLink("addNew", form) {

                    @Override
                    public void onClick(AjaxRequestTarget target, Form<?> form) {
                        AttributeDescription attribute = new AttributeDescription();
                        setResponsePage(new AttributeNewPage(attribute, NewFeatureTypePage.this));
                    }
                });

        header.add(
                new GeoServerAjaxFormLink("removeSelected", form) {

                    @Override
                    public void onClick(AjaxRequestTarget target, Form<?> form) {
                        attributesProvider.removeAll(attributeTable.getSelection());
                        attributeTable.clearSelection();
                        target.add(form);
                    }
                });

        return header;
    }

    protected Component upDownFragment(String id, final AttributeDescription attribute) {
        Fragment upDown = new Fragment(id, "upDown", this);
        if (attributesProvider.isFirst(attribute)) {
            upDown.add(new PlaceholderLink("up"));
        } else {
            ImageAjaxLink<Void> upLink =
                    new ImageAjaxLink<Void>(
                            "up",
                            new PackageResourceReference(
                                    getClass(), "../../img/icons/silk/arrow_up.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            attributesProvider.moveUp(attribute);
                            target.add(form);
                        }
                    };
            upDown.add(upLink);
        }

        if (attributesProvider.isLast(attribute)) {
            upDown.add(new PlaceholderLink("down"));
        } else {
            ImageAjaxLink<Void> downLink =
                    new ImageAjaxLink<Void>(
                            "down",
                            new PackageResourceReference(
                                    getClass(), "../../img/icons/silk/arrow_down.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            attributesProvider.moveDown(attribute);
                            target.add(form);
                        }
                    };
            upDown.add(downLink);
        }

        return upDown;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    /**
     * An empty link, put there just so that it will consume the same space as an ImageAjaxLink
     *
     * @author Andrea Aime
     */
    class PlaceholderLink extends ImageAjaxLink<Void> {

        public PlaceholderLink(String id) {
            super(
                    id,
                    new PackageResourceReference(
                            NewFeatureTypePage.class, "../../img/icons/blank.png"));
            setEnabled(false);
        }

        @Override
        protected void onClick(AjaxRequestTarget target) {
            // nothing to do
        }
    }
}
