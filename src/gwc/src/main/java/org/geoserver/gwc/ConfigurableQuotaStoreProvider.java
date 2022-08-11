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

package org.geoserver.gwc;

import static org.geowebcache.diskquota.DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.QuotaStoreProvider;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStoreFactory;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.layer.TileLayer;
import org.springframework.context.ApplicationContext;

/**
 * A quota store whose store is a {@link ConfigurableQuotaStore} whose delegate can be reloaded by
 * calling onto {@link #reloadQuotaStore()}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ConfigurableQuotaStoreProvider extends QuotaStoreProvider {

    static final Logger LOGGER = Logging.getLogger(ConfigurableQuotaStoreProvider.class);

    Exception exception;
    TilePageCalculator calculator;

    boolean diskQuotaEnabled;

    private JDBCConfigurationStorage jdbcConfigManager;

    public ConfigurableQuotaStoreProvider(
            ConfigLoader loader,
            TilePageCalculator calculator,
            JDBCConfigurationStorage jdbcConfigManager) {
        super(loader);
        this.calculator = calculator;
        this.jdbcConfigManager = jdbcConfigManager;

        boolean disabled =
                Boolean.valueOf(GeoServerExtensions.getProperty(GWC_DISKQUOTA_DISABLED))
                        .booleanValue();
        if (disabled) {
            LOGGER.warning(
                    " -- Found environment variable "
                            + GWC_DISKQUOTA_DISABLED
                            + " set to true. DiskQuotaMonitor is disabled.");
        }
        this.diskQuotaEnabled = !disabled;
    }

    @Override
    public void reloadQuotaStore() throws ConfigurationException, IOException {
        if (!diskQuotaEnabled) {
            store = null;
            return;
        }

        // get the quota store name
        DiskQuotaConfig config = loader.loadConfig();
        QuotaStore store = null;
        if (!config.isEnabled()) {
            // it would be nice to just return null, but the other portions of the
            // disk quota system will throw exceptions if we did while the quota store
            // is not disable via system variable. Let's just give it a dummy quota store instead.
            store = new DummyQuotaStore(calculator);
        } else {
            String quotaStoreName = config.getQuotaStore();
            // in case it's null GeoServer defaults to H2 store, we don't have the
            // BDB store in the classpath
            if (quotaStoreName == null) {
                quotaStoreName = JDBCQuotaStoreFactory.H2_STORE;
            }

            try {
                store = getQuotaStoreByName(quotaStoreName);
                exception = null;
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Failed to get a quota store, "
                                + "the GeoWebCache disk quota subsystem will stop working now",
                        e);
                this.exception = e;
                store = new DummyQuotaStore(calculator);
            }
        }

        if (this.store == null) {
            this.store = new ConfigurableQuotaStore(store);
        } else {
            ConfigurableQuotaStore configurable = (ConfigurableQuotaStore) this.store;
            QuotaStore oldStore = configurable.getStore();
            configurable.setStore(store);
            // clean up the quota information gathered so far, otherwise when re-enabling
            // we'll have in the db stale information
            if (!(oldStore instanceof DummyQuotaStore)) {
                try {
                    for (TileLayer tl : GWC.get().getTileLayers()) {
                        oldStore.deleteLayer(tl.getName());
                    }
                } finally {
                    try {
                        oldStore.close();
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.SEVERE,
                                "An error occurred while closing up the previous quota store",
                                e);
                    }
                }
            }
        }
    }

    /** The exception occurred during the last attempt to load the quota store, if any */
    public Exception getException() {
        return exception;
    }

    @Override
    protected QuotaStore getQuotaStoreByName(String quotaStoreName)
            throws ConfigurationException, IOException {
        if ("JDBC".equals(quotaStoreName)) {
            return loadJDBCQuotaStore(applicationContext, quotaStoreName);
        } else {
            return super.getQuotaStoreByName(quotaStoreName);
        }
    }

    private QuotaStore loadJDBCQuotaStore(
            ApplicationContext applicationContext, String quotaStoreName)
            throws ConfigurationException, IOException {
        // special case for the JDBC quota store, allows us to unencrypt passwords before
        // creating the GUI
        JDBCConfiguration config = jdbcConfigManager.getJDBCDiskQuotaConfig();
        JDBCQuotaStoreFactory factory = new JDBCQuotaStoreFactory();
        factory.setApplicationContext(applicationContext);
        return factory.getJDBCStore(applicationContext, config);
    }
}
