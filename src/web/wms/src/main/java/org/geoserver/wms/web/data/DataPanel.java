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

package org.geoserver.wms.web.data;

import java.io.IOException;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.type.PropertyDescriptor;

/** Panel for listing sample attributes of a FeatureTypeInfo resource. */
public class DataPanel extends Panel {
    private static final long serialVersionUID = -2635691554700860434L;

    static final Logger LOGGER = Logging.getLogger(DataPanel.class);

    String featureTypeId;

    public DataPanel(String id, FeatureTypeInfo ft) {
        super(id, new Model<>(ft));
        this.featureTypeId = ft.getId();

        add(
                new Label(
                        "summary-message",
                        "For reference, here is a listing of the attributes in this data set.")); // TODO: I18N
        final WebMarkupContainer attsContainer = new WebMarkupContainer("attributes-container");
        attsContainer.setOutputMarkupId(true);
        add(attsContainer);

        Feature sample;
        try {
            sample = getSampleFeature(ft);
        } catch (Exception e) {
            attsContainer.error(
                    "Failed to load attribute list, internal error is: " + e.getMessage());
            attsContainer.add(new EmptyPanel("attributes"));
            return;
        }
        DataAttributesProvider summaries = new DataAttributesProvider(sample);

        final GeoServerTablePanel<DataAttribute> attributes =
                new GeoServerTablePanel<DataAttribute>("attributes", summaries) {

                    private static final long serialVersionUID = 7753093373969576568L;

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            final IModel<DataAttribute> itemModel,
                            Property<DataAttribute> property) {
                        if (DataAttributesProvider.COMPUTE_STATS.equals(property.getName())) {
                            Fragment f = new Fragment(id, "computeStatsFragment", DataPanel.this);
                            f.add(
                                    new AjaxLink<Void>("computeStats") {

                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        public void onClick(AjaxRequestTarget target) {
                                            DataAttribute attribute = itemModel.getObject();
                                            try {
                                                updateAttributeStats(attribute);
                                            } catch (IOException e) {
                                                error(
                                                        "Failed to compute stats for the attribute: "
                                                                + e.getMessage());
                                            }
                                            target.add(attsContainer);
                                        }
                                    });

                            return f;
                        }

                        return null;
                    }
                };
        attributes.setPageable(false);
        attributes.setFilterable(false);
        attributes.setSortable(false);
        attsContainer.add(attributes);
    }

    protected void updateAttributeStats(DataAttribute attribute) throws IOException {
        FeatureTypeInfo featureType =
                GeoServerApplication.get().getCatalog().getFeatureType(featureTypeId);
        FeatureSource<?, ?> fs = featureType.getFeatureSource(null, null);

        // check we can compute min and max
        PropertyDescriptor pd = fs.getSchema().getDescriptor(attribute.getName());
        Class<?> binding = pd.getType().getBinding();
        if (pd == null
                || !Comparable.class.isAssignableFrom(binding)
                || Geometry.class.isAssignableFrom(binding)) {
            return;
        }

        // grab the feature collection and run the min/max visitors (this will move the
        // query to the dbms in case of such data source)
        Query q = new Query();
        q.setPropertyNames(attribute.getName());
        FeatureCollection<?, ?> fc = fs.getFeatures(q);
        MinVisitor minVisitor = new MinVisitor(attribute.getName());
        MaxVisitor maxVisitor = new MaxVisitor(attribute.getName());
        fc.accepts(minVisitor, null);
        fc.accepts(maxVisitor, null);
        Object min = minVisitor.getResult().getValue();
        attribute.setMin(Converters.convert(min, String.class));
        Object max = maxVisitor.getResult().getValue();
        attribute.setMax(Converters.convert(max, String.class));
    }

    private Feature getSampleFeature(FeatureTypeInfo layerInfo) throws IOException {
        FeatureSource<?, ?> fs = layerInfo.getFeatureSource(null, null);
        Query q = new Query();
        q.setMaxFeatures(1);
        FeatureCollection<?, ?> features = fs.getFeatures(q);
        try (FeatureIterator<?> fi = features.features()) {
            return fi.next();
        }
    }
}
