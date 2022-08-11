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

package org.geoserver.jdbcconfig;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public class JDBCGeoServerLoader extends DefaultGeoServerLoader {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logging.getLogger(JDBCGeoServerLoader.class);

    private CatalogFacade catalogFacade;

    private GeoServerFacade geoServerFacade;

    private JDBCConfigProperties config;

    private int importSteps = 2;

    public JDBCGeoServerLoader(GeoServerResourceLoader resourceLoader, JDBCConfigProperties config)
            throws Exception {
        super(resourceLoader);
        this.config = config;
    }

    public void setCatalogFacade(CatalogFacade catalogFacade) throws IOException {
        this.catalogFacade = catalogFacade;

        if (!config.isEnabled()) {
            return;
        }

        ConfigDatabase configDatabase = ((JDBCCatalogFacade) catalogFacade).getConfigDatabase();

        Resource initScript = config.isInitDb() ? config.getInitScript() : null;
        configDatabase.initDb(initScript);

        config.setInitDb(false);
        config.save();
    }

    public void setGeoServerFacade(GeoServerFacade geoServerFacade) {
        this.geoServerFacade = geoServerFacade;
    }

    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        if (!config.isEnabled()) {
            super.loadCatalog(catalog, xp);
            return;
        }

        Stopwatch sw = Stopwatch.createStarted();
        loadCatalogInternal(catalog, xp);
        sw.stop();

        catalog.addListener(new GeoServerResourcePersister(catalog));
        // System.err.println("Loaded catalog in " + sw.toString());
    }

    private void loadCatalogInternal(Catalog catalog, XStreamPersister xp) throws Exception {
        ((CatalogImpl) catalog).setFacade(catalogFacade);

        // if this is the first time loading up with jdbc configuration, migrate from old
        // file based structure
        if (config.isImport()) {
            readCatalog(catalog, xp);
            decImportStep();
        }

        if (!config.isInitDb() && !config.isImport() && config.isRepopulate()) {
            ConfigDatabase configDatabase = ((JDBCCatalogFacade) catalogFacade).getConfigDatabase();
            configDatabase.repopulateQueryableProperties();
            config.setRepopulate(false);
            config.save();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void loadGeoServer(GeoServer geoServer, XStreamPersister xp) throws Exception {
        if (!config.isEnabled()) {
            super.loadGeoServer(geoServer, xp);
            return;
        }

        ((GeoServerImpl) geoServer).setFacade(geoServerFacade);

        // if this is the first time loading up with bdb je configuration, migrate from old
        // file based structure
        if (config.isImport()) {
            readConfiguration(geoServer, xp);
            decImportStep();
        }

        // do a post check to ensure things were loaded, for instance if we are starting from
        // an empty data directory all objects will be empty
        // TODO: this should really be moved elsewhere
        if (geoServer.getGlobal() == null) {
            geoServer.setGlobal(geoServer.getFactory().createGlobal());
        }
        if (geoServer.getLogging() == null) {
            geoServer.setLogging(geoServer.getFactory().createLogging());
        }

        // also ensure we have a service configuration for every service we know about
        final List<XStreamServiceLoader> loaders =
                GeoServerExtensions.extensions(XStreamServiceLoader.class);
        for (XStreamServiceLoader l : loaders) {
            ServiceInfo s = geoServer.getService(l.getServiceClass());
            if (s == null) {
                geoServer.add(l.create(geoServer));
            }
        }
    }

    private void decImportStep() throws IOException {
        if (--importSteps == 0) {

            // import completed, reset flag
            config.setImport(false);
            config.save();
        }
    }

    @Override
    public void reload() throws Exception {
        super.reload();
    }
}
