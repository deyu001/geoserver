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

package org.geoserver.config;

import java.util.Map;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.platform.resource.LockProvider;

/**
 * Global GeoServer configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface GeoServerInfo extends Info {

    /** Identifier. */
    String getId();

    /**
     * The global settings.
     *
     * <p>Generally client code shoudl not call this method directly, and rather call {@link
     * GeoServer#getSettings()}.
     */
    SettingsInfo getSettings();

    /** Sets the global settings. */
    void setSettings(SettingsInfo settings);

    /** The flag to use request headers for the proxy URL */
    Boolean isUseHeadersProxyURL();

    /** Sets the flag to use request headers for the proxy URL */
    void setUseHeadersProxyURL(Boolean useHeadersProxyURL);

    /** The Java Advanced Imaging configuration. */
    JAIInfo getJAI();

    /** Sets the Java Advanced Imaging configuration. */
    void setJAI(JAIInfo jai);

    /** The Coverage Access configuration. */
    CoverageAccessInfo getCoverageAccess();

    /** Sets the Coverage Access configuration. */
    void setCoverageAccess(CoverageAccessInfo coverageInfo);

    /** Sets the administrator username. */
    String getAdminUsername();

    /** The administrator username. */
    void setAdminUsername(String adminUsername);

    /** The administrator password. */
    String getAdminPassword();

    /** Sets the administrator password. */
    void setAdminPassword(String adminPassword);

    /**
     * Set the XML error handling mode for the server.
     *
     * @see ResourceErrorHandling
     */
    void setResourceErrorHandling(ResourceErrorHandling mode);

    /** Get the XML error handling mode for the server. */
    ResourceErrorHandling getResourceErrorHandling();

    /**
     * The update sequence.
     *
     * <p>This value is used by various ogc services to track changes to a capabilities document.
     */
    long getUpdateSequence();

    /** Sets the update sequence. */
    void setUpdateSequence(long updateSequence);

    /** The size of the cache for feature type objects. */
    int getFeatureTypeCacheSize();

    /** Sets the size of the cache for feature type objects. */
    void setFeatureTypeCacheSize(int featureTypeCacheSize);

    /** Flag determining if access to services should occur only through "virtual services". */
    Boolean isGlobalServices();

    /** Sets the flag forcing access to services only through virtual services. */
    void setGlobalServices(Boolean globalServices);

    /** Sets logging buffer size of incoming XML Post Requests for WFS,WMS,... */
    void setXmlPostRequestLogBufferSize(Integer requestBufferSize);

    /** Gets log buffer size of XML Post Request for WFS,WMS,... */
    Integer getXmlPostRequestLogBufferSize();

    /**
     * If true it enables evaluation of XML entities contained in XML files received in a service
     * (WMS, WFS, ...) request. Default is FALSE. Enabling this feature is a security risk.
     */
    void setXmlExternalEntitiesEnabled(Boolean xmlExternalEntitiesEnabled);

    /**
     * If true it enables evaluation of XML entities contained in XML files received in a service
     * (WMS, WFS, ...) request. Default is FALSE. Enabling this feature is a security risk.
     */
    Boolean isXmlExternalEntitiesEnabled();

    /**
     * Name of lock provider used for resource access.
     *
     * @return name of spring bean to use as lock provider
     */
    public String getLockProviderName();

    /**
     * Sets the name of the {@link LockProvider} to use for resoruce access.
     *
     * <p>The following spring bean names are initially provided with the application:
     *
     * <ul>
     *   <li>nullLockProvider
     *   <li>memoryLockProvider
     *   <li>fileLockProvider
     * </ul>
     *
     * @param lockProviderName Name of lock provider used for resource access.
     */
    public void setLockProviderName(String lockProviderName);

    /**
     * A map of metadata for services.
     *
     * @uml.property name="metadata"
     */
    MetadataMap getMetadata();

    /**
     * Client properties for services.
     *
     * <p>These values are transient, and not persistent.
     */
    Map<Object, Object> getClientProperties();

    /** Disposes the global configuration object. */
    void dispose();

    /** WebUIMode choices */
    public enum WebUIMode {
        /** Let GeoServer determine the best mode. */
        DEFAULT,
        /**
         * Always redirect to persist page state (prevent double submit problem but doesn't support
         * clustering)
         */
        REDIRECT,
        /**
         * Never redirect to persist page state (supports clustering but doesn't prevent double
         * submit problem)
         */
        DO_NOT_REDIRECT
    };

    /**
     * Get the WebUIMode
     *
     * @return the WebUIMode
     */
    public WebUIMode getWebUIMode();

    /** Set the WebUIMode */
    public void setWebUIMode(WebUIMode mode);

    /** Determines if Per-workspace Stores Queries are activated. */
    Boolean isAllowStoredQueriesPerWorkspace();

    /** Sets if Per-workspace Stores Queries are activated. */
    void setAllowStoredQueriesPerWorkspace(Boolean allowStoredQueriesPerWorkspace);
}
