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

package org.geoserver.wfs.xml.v1_0_0;

import java.util.Map;
import java.util.logging.Logger;
import net.opengis.ows10.Ows10Factory;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.wfs.CatalogFeatureTypeCache;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.PropertyTypePropertyExtractor;
import org.geoserver.wfs.xml.WFSHandlerFactory;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geoserver.wfs.xml.gml2.GMLBoxTypeBinding;
import org.geotools.data.DataAccess;
import org.geotools.filter.v1_0.OGCBBOXTypeBinding;
import org.geotools.filter.v1_0.OGCConfiguration;
import org.geotools.filter.v1_1.OGC;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.GML;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.OptionalComponentParameter;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;

/**
 * Parser configuration for wfs 1.0.
 *
 * @author Justin Deoliveira, The Open Planning Project TODO: this class duplicates a lot of what is
 *     is in the 1.1 configuration, merge them
 */
public class WFSConfiguration extends Configuration {
    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.wfs");

    Catalog catalog;
    FeatureTypeSchemaBuilder schemaBuilder;

    public WFSConfiguration(
            Catalog catalog, FeatureTypeSchemaBuilder schemaBuilder, final WFS wfs) {
        super(wfs);

        this.catalog = catalog;
        this.schemaBuilder = schemaBuilder;

        catalog.addListener(
                new CatalogListener() {

                    public void handleAddEvent(CatalogAddEvent event) {
                        if (event.getSource() instanceof FeatureTypeInfo) {
                            reloaded();
                        }
                    }

                    public void handleModifyEvent(CatalogModifyEvent event) {
                        if (event.getSource() instanceof DataStoreInfo
                                || event.getSource() instanceof FeatureTypeInfo
                                || event.getSource() instanceof NamespaceInfo) {
                            reloaded();
                        }
                    }

                    public void handlePostModifyEvent(CatalogPostModifyEvent event) {}

                    public void handleRemoveEvent(CatalogRemoveEvent event) {}

                    public void reloaded() {
                        wfs.dispose();
                    }
                });
        catalog.getResourcePool()
                .addListener(
                        new ResourcePool.Listener() {

                            public void disposed(FeatureTypeInfo featureType, FeatureType ft) {}

                            public void disposed(
                                    CoverageStoreInfo coverageStore, GridCoverageReader gcr) {}

                            public void disposed(DataStoreInfo dataStore, DataAccess da) {
                                wfs.dispose();
                            }
                        });

        addDependency(new OGCConfiguration());
        addDependency(new GMLConfiguration());
    }

    public Catalog getCatalog() {
        return catalog;
    }

    protected void registerBindings(MutablePicoContainer container) {
        // Types
        container.registerComponentImplementation(WFS.ALLSOMETYPE, AllSomeTypeBinding.class);
        container.registerComponentImplementation(
                WFS.DELETEELEMENTTYPE, DeleteElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.DESCRIBEFEATURETYPETYPE, DescribeFeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFS.EMPTYTYPE, EmptyTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURECOLLECTIONTYPE, FeatureCollectionTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURESLOCKEDTYPE, FeaturesLockedTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURESNOTLOCKEDTYPE, FeaturesNotLockedTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GETCAPABILITIESTYPE, GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS.GETFEATURETYPE, GetFeatureTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GETFEATUREWITHLOCKTYPE, GetFeatureWithLockTypeBinding.class);
        container.registerComponentImplementation(
                WFS.INSERTELEMENTTYPE, InsertElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.INSERTRESULTTYPE, InsertResultTypeBinding.class);
        container.registerComponentImplementation(
                WFS.LOCKFEATURETYPE, LockFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS.LOCKTYPE, LockTypeBinding.class);
        container.registerComponentImplementation(WFS.NATIVETYPE, NativeTypeBinding.class);
        container.registerComponentImplementation(WFS.PROPERTYTYPE, PropertyTypeBinding.class);
        container.registerComponentImplementation(WFS.QUERYTYPE, QueryTypeBinding.class);
        container.registerComponentImplementation(WFS.STATUSTYPE, StatusTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONRESULTTYPE, TransactionResultTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONTYPE, TransactionTypeBinding.class);
        container.registerComponentImplementation(
                WFS.UPDATEELEMENTTYPE, UpdateElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.WFS_LOCKFEATURERESPONSETYPE, WFS_LockFeatureResponseTypeBinding.class);
        container.registerComponentImplementation(
                WFS.WFS_TRANSACTIONRESPONSETYPE, WFS_TransactionResponseTypeBinding.class);
    }

    public void configureContext(MutablePicoContainer context) {
        super.configureContext(context);

        context.registerComponentInstance(Ows10Factory.eINSTANCE);
        context.registerComponentInstance(WfsFactory.eINSTANCE);
        context.registerComponentInstance(new WFSHandlerFactory(catalog, schemaBuilder));
        context.registerComponentInstance(catalog);
        context.registerComponentImplementation(PropertyTypePropertyExtractor.class);

        // TODO: this code is copied from the 1.1 configuration, FACTOR IT OUT!!!
        // seed the cache with entries from the catalog
        context.registerComponentInstance(
                FeatureTypeCache.class, new CatalogFeatureTypeCache(getCatalog()));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureBindings(Map bindings) {
        // override the GMLAbstractFeatureTypeBinding
        bindings.put(GML.AbstractFeatureType, GMLAbstractFeatureTypeBinding.class);

        WFSXmlUtils.registerAbstractGeometryTypeBinding(this, bindings, GML.AbstractGeometryType);

        bindings.put(
                GML.BoxType,
                new SetterInjectionComponentAdapter(
                        GML.BoxType,
                        GMLBoxTypeBinding.class,
                        new Parameter[] {
                            new OptionalComponentParameter(CoordinateReferenceSystem.class)
                        }));

        // use setter injection for OGCBBoxTypeBinding to allow an
        // optional crs to be set in teh binding context for parsing, this crs
        // is set by the binding of a parent element.
        // note: it is important that this component adapter is non-caching so
        // that the setter property gets updated properly every time
        bindings.put(
                OGC.BBOXType,
                new SetterInjectionComponentAdapter(
                        OGC.BBOXType,
                        OGCBBOXTypeBinding.class,
                        new Parameter[] {
                            new OptionalComponentParameter(CoordinateReferenceSystem.class)
                        }));
    }
}
