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

package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkState;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.util.Modules;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.di.GeogigModule;
import org.locationtech.geogig.di.HintsModule;
import org.locationtech.geogig.di.PluginsModule;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.impl.ContextBuilder;
import org.locationtech.geogig.storage.ConflictsDatabase;
import org.locationtech.geogig.storage.IndexDatabase;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.geogig.storage.PluginDefaults;
import org.locationtech.geogig.storage.RefDatabase;
import org.locationtech.geogig.storage.StorageProvider;
import org.locationtech.geogig.storage.VersionedFormat;

public class GeoServerContextBuilder extends ContextBuilder {

    private static final Logger LOGGER = Logging.getLogger(GeoServerContextBuilder.class);

    @Override
    public Context build(Hints hints) {
        return Guice.createInjector(
                        Modules.override(new GeogigModule(), new HintsModule(hints))
                                .with(new PluginsModule(), new DefaultPlugins()))
                .getInstance(org.locationtech.geogig.repository.Context.class);
    }

    public static class DefaultPlugins extends AbstractModule {

        private static final StorageProvider DEFAULT_PROVIDER;

        static {
            // hack to set a PluginDefaults to rocksDB without having an explicit dependency
            // on RocksDB modules. This should be removed once the StroageProvider/Plugin
            // mechanisms are reworked.
            StorageProvider storageProvider = null;
            for (StorageProvider provider : StorageProvider.findProviders()) {
                if ("rocksdb".equals(provider.getName())) {
                    // we have a RocksDB provider available, use it as the default
                    storageProvider = provider;
                    break;
                }
            }
            // set the default to the provider found, or null
            DEFAULT_PROVIDER = storageProvider;
            if (null == DEFAULT_PROVIDER && LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "No Default StorageProvider available");
            }
        }

        @Override
        protected void configure() {

            if (null != DEFAULT_PROVIDER) {
                // set a PluginDefaults using the default provider
                PluginDefaults pluginDefaults = new PluginDefaults(DEFAULT_PROVIDER);
                bind(PluginDefaults.class).toInstance(pluginDefaults);
            }

            MapBinder<VersionedFormat, RefDatabase> refPlugins =
                    MapBinder.newMapBinder(binder(), VersionedFormat.class, RefDatabase.class)
                            .permitDuplicates();

            MapBinder<VersionedFormat, ObjectDatabase> objectPlugins =
                    MapBinder.newMapBinder(binder(), VersionedFormat.class, ObjectDatabase.class)
                            .permitDuplicates();

            MapBinder<VersionedFormat, IndexDatabase> indexPlugins =
                    MapBinder.newMapBinder(binder(), VersionedFormat.class, IndexDatabase.class)
                            .permitDuplicates();

            MapBinder<VersionedFormat, ConflictsDatabase> graphPlugins =
                    MapBinder.newMapBinder(binder(), VersionedFormat.class, ConflictsDatabase.class)
                            .permitDuplicates();

            Iterable<StorageProvider> providers = StorageProvider.findProviders();

            for (StorageProvider sp : providers) {
                VersionedFormat objectDatabaseFormat = sp.getObjectDatabaseFormat();
                VersionedFormat indexDatabaseFormat = sp.getIndexDatabaseFormat();
                VersionedFormat conflictsDatabaseFormat = sp.getConflictsDatabaseFormat();
                VersionedFormat refsDatabaseFormat = sp.getRefsDatabaseFormat();

                if (objectDatabaseFormat != null) {
                    GeoServerContextBuilder.bind(objectPlugins, objectDatabaseFormat);
                }
                if (indexDatabaseFormat != null) {
                    GeoServerContextBuilder.bind(indexPlugins, indexDatabaseFormat);
                }
                if (conflictsDatabaseFormat != null) {
                    GeoServerContextBuilder.bind(graphPlugins, conflictsDatabaseFormat);
                }
                if (refsDatabaseFormat != null) {
                    GeoServerContextBuilder.bind(refPlugins, refsDatabaseFormat);
                }
            }
        }
    }

    static <T> void bind(MapBinder<VersionedFormat, T> plugins, VersionedFormat format) {
        Class<?> implementingClass = format.getImplementingClass();
        checkState(
                implementingClass != null,
                "If singleton class not provided, this method must be overritten");
        @SuppressWarnings("unchecked")
        Class<? extends T> binding = (Class<? extends T>) implementingClass;
        plugins.addBinding(format).to(binding).in(Scopes.SINGLETON);
    }
}
