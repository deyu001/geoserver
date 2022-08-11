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

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.Version;

/**
 * Generic / abstract service configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ServiceInfo extends Info {

    /** Identifer. */
    String getId();

    /**
     * Name of the service.
     *
     * <p>This value is unique among all instances of ServiceInfo and can be used as an identifier.
     *
     * @uml.property name="name"
     */
    String getName();

    /**
     * Sets the name of the service.
     *
     * @uml.property name="name"
     */
    void setName(String name);

    /**
     * The workspace the service is specific or local to, or <code>null</code> if the service is
     * global.
     */
    WorkspaceInfo getWorkspace();

    /** Sets the workspace the service is specific or local to. */
    void setWorkspace(WorkspaceInfo workspace);

    /**
     * The global geoserver configuration.
     *
     * @uml.property name="geoServer"
     * @uml.associationEnd inverse="service:org.geoserver.config.GeoServerInfo"
     */
    GeoServer getGeoServer();

    /**
     * Sets the global geoserver configuration.
     *
     * @uml.property name="geoServer"
     */
    void setGeoServer(GeoServer geoServer);

    /** @uml.property name="citeCompliant" */
    boolean isCiteCompliant();

    /** @uml.property name="citeCompliant" */
    void setCiteCompliant(boolean citeCompliant);

    /** @uml.property name="enabled" */
    boolean isEnabled();

    /** @uml.property name="enabled" */
    void setEnabled(boolean enabled);

    /** @uml.property name="onlineResource" */
    String getOnlineResource();

    /** @uml.property name="onlineResource" */
    void setOnlineResource(String onlineResource);

    /** @uml.property name="title" */
    String getTitle();

    /** @uml.property name="title" */
    void setTitle(String title);

    /** @uml.property name="abstract" */
    String getAbstract();

    /** @uml.property name="abstract" */
    void setAbstract(String abstrct);

    /** @uml.property name="maintainer" */
    String getMaintainer();

    /** @uml.property name="maintainer" */
    void setMaintainer(String maintainer);

    /** @uml.property name="fees" */
    String getFees();

    /** @uml.property name="fees" */
    void setFees(String fees);

    /** @uml.property name="accessConstraints" */
    String getAccessConstraints();

    /** @uml.property name="accessConstraints" */
    void setAccessConstraints(String accessConstraints);

    /**
     * The versions of the service that are available.
     *
     * <p>This list contains objects of type {@link Version}.
     *
     * @uml.property name="versions"
     */
    List<Version> getVersions();

    /**
     * Keywords associated with the service.
     *
     * @uml.property name="keywords"
     */
    List<KeywordInfo> getKeywords();

    /** List of keyword values, derived from {@link #getKeywords()}. */
    List<String> keywordValues();

    /**
     * Exception formats the service can provide.
     *
     * @uml.property name="exceptionFormats"
     */
    List<String> getExceptionFormats();

    /**
     * The service metadata link.
     *
     * @uml.property name="metadataLink"
     * @uml.associationEnd inverse="service:org.geoserver.catalog.MetadataLinkInfo"
     */
    MetadataLinkInfo getMetadataLink();

    /**
     * Setter of the property <tt>metadataLink</tt>
     *
     * @uml.property name="metadataLink"
     */
    void setMetadataLink(MetadataLinkInfo metadataLink);

    /**
     * Sets the output strategy used by the service.
     *
     * <p>This value is an identifier which indicates how the output of a response should behave. An
     * example might be "performance", indicating that the response should be encoded as quickly as
     * possible.
     */
    String getOutputStrategy();

    /** Sets the output strategy. */
    void setOutputStrategy(String outputStrategy);

    /** The base url for the schemas describing the service. */
    String getSchemaBaseURL();

    /** Sets the base url for the schemas describing the service. */
    void setSchemaBaseURL(String schemaBaseURL);

    /** Flag indicating if the service should be verbose or not. */
    boolean isVerbose();

    /** Sets the flag indicating if the service should be verbose or not. */
    void setVerbose(boolean verbose);

    /** @uml.property name="metadata" */
    MetadataMap getMetadata();

    Map<Object, Object> getClientProperties();
}
