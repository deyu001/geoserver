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

package org.geoserver.wms;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.renderer.style.FontCache;
import org.geotools.renderer.style.GraphicCache;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Drops imaging caches
 *
 * @author Andrea Aime - OpenGeo
 */
public class WMSLifecycleHandler implements GeoServerLifecycleHandler, ApplicationListener {

    static final Logger LOGGER = Logging.getLogger(WMSLifecycleHandler.class);

    GeoServerDataDirectory data;
    WMS wmsConfig;

    public WMSLifecycleHandler(GeoServerDataDirectory data, WMS wmsConfig) {
        this.data = data;
        this.wmsConfig = wmsConfig;
    }

    public void onDispose() {
        // dispose the WMS Animator Executor Service
        shutdownAnimatorExecutorService();
    }

    public void beforeReload() {
        // nothing to do
    }

    public void onReload() {
        // clear the caches for good measure
        onReset();
    }

    public void onReset() {
        // kill the image caches
        Iterator<ExternalGraphicFactory> it =
                DynamicSymbolFactoryFinder.getExternalGraphicFactories();
        while (it.hasNext()) {
            ExternalGraphicFactory egf = it.next();
            if (egf instanceof GraphicCache) {
                ((GraphicCache) egf).clearCache();
            }
        }

        // reloads the font cache
        reloadFontCache();

        // reset WMS Animator Executor Service
        resetAnimatorExecutorService();
    }

    /** Shutting down pending tasks and resetting the executor service timeout. */
    private void resetAnimatorExecutorService() {
        shutdownAnimatorExecutorService();

        Long framesTimeout =
                this.wmsConfig.getMaxAnimatorRenderingTime() != null
                        ? this.wmsConfig.getMaxAnimatorRenderingTime()
                        : Long.MAX_VALUE;
        ExecutorService animatorExecutorService =
                new ThreadPoolExecutor(
                        4, 20, framesTimeout, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        this.wmsConfig.setAnimatorExecutorService(animatorExecutorService);
    }

    /** Suddenly shuts down the Animator Executor Service */
    private void shutdownAnimatorExecutorService() {
        final ExecutorService animatorExecutorService = this.wmsConfig.getAnimatorExecutorService();
        if (animatorExecutorService != null && !animatorExecutorService.isShutdown()) {
            animatorExecutorService.shutdownNow();
        }
    }

    void reloadFontCache() {
        List<Font> fonts = loadFontsFromDataDirectory();
        final FontCache cache = FontCache.getDefaultInstance();
        cache.resetCache();
        for (Font font : fonts) {
            cache.registerFont(font);
        }
    }

    List<Font> loadFontsFromDataDirectory() {
        List<Font> result = new ArrayList<>();
        for (Resource file :
                Resources.list(
                        data.getStyles(), new Resources.ExtensionFilter("TTF", "OTF"), true)) {
            try {
                final Font font = Font.createFont(Font.TRUETYPE_FONT, file.file());
                result.add(font);
                LOGGER.log(
                        Level.INFO,
                        "Loaded font file "
                                + file
                                + ", loaded font '"
                                + font.getName()
                                + "' in family '"
                                + font.getFamily()
                                + "'");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load font file " + file, e);
            }
        }

        return result;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            reloadFontCache();

            // reset WMS Animator Executor Service
            resetAnimatorExecutorService();
        }
    }
}
