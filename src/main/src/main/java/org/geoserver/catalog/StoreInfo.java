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

package org.geoserver.catalog;

import java.io.Serializable;
import java.util.Map;

/**
 * A store of geoaspatial resources.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface StoreInfo extends CatalogInfo {

    /** The catalog the store is part of. */
    Catalog getCatalog();

    /**
     * The store name.
     *
     * <p>This value is unique among all stores and can be used to identify the store.
     *
     * @uml.property name="name"
     */
    String getName();

    /**
     * Sets the name of the store.
     *
     * @uml.property name="name"
     */
    void setName(String name);

    /**
     * The store description.
     *
     * <p>This is usually something that is used in a user interface.
     *
     * @uml.property name="description"
     */
    String getDescription(); // FIXME: InternationalString ?

    /**
     * Sets the store description.
     *
     * <p>This is usually something that is used in a user interface.
     *
     * @uml.property name="description"
     */
    void setDescription(String description);

    /**
     * The store type.
     *
     * <p>This value is a well known string representing the nature of the store. Examples include
     * "Shapefile", "Postgis", "GeoTIFF", etc...
     */
    String getType();

    /** Sets the type of the store. */
    void setType(String type);

    /**
     * Map of persistent properties associated with the store.
     *
     * <p>The intent of this map is for services to associate data with a particular store which
     * must be persisted.
     *
     * <p>Key values in this map are of type {@link String}, and values are of type {@link
     * Serializable}.
     *
     * @uml.property name="metadata"
     */
    MetadataMap getMetadata();

    /**
     * Flag indicating wether or not teh store is enabled or not.
     *
     * @uml.property name="enabled"
     */
    boolean isEnabled();

    /**
     * Sets the store enabled / disabled flag.
     *
     * @uml.property name="enabled"
     */
    void setEnabled(boolean enabled);

    /**
     * The namespace the store is part of.
     *
     * <p>This value is often used to set the namespace of {@link ResourceInfo}objects which are
     * associated to the store.
     *
     * @uml.property name="namespace"
     * @uml.associationEnd inverse="storeInfo:org.geoserver.catalog.NamespaceInfo"
     */
    // NamespaceInfo getNamespace();

    /**
     * Sets the namespace the store is part of.
     *
     * @uml.property name="namespace"
     */
    // void setNamespace(NamespaceInfo namespace);

    /** The workspace the store is part of. */
    WorkspaceInfo getWorkspace();

    /** Sets the workspace the store is part of. */
    void setWorkspace(WorkspaceInfo workspace);

    /**
     * The map of connection paramters specific to the store.
     *
     * <p>Key values in this map are of type {@link String}, and values are of type {@link
     * Serializable}.
     *
     * @uml.property name="connectionParameters"
     */
    Map<String, Serializable> getConnectionParameters();

    /**
     * An error associated with the store.
     *
     * <p>This value is used to store a problem that occured while attemping to connect to the
     * underlying resource of the store. It returns <code>null</code> if no such error exists.
     *
     * <p>This is a transient property of the store.
     */
    Throwable getError();

    /**
     * Associates an error with the store.
     *
     * @see #getError()
     */
    void setError(Throwable t);

    /**
     * Creates an adapter for the store.
     *
     * <p>
     *
     * @param adapterClass The class of the adapter.
     * @param hints Hints to use when creating the adapter.
     * @return The adapter, an intsanceof adapterClass, or <code>null</code>.
     */
    <T extends Object> T getAdapter(Class<T> adapterClass, Map<?, ?> hints);

    /**
     * @return Returns a resource with the specified name that is provided by the store, or <code>
     *     null</code> if no such resource exists.
     *     <p>The monitor is used to report the progress of loading resoures and report any warnings
     *     / errors that occur in doing so. Monitor may also be null.
     */
    // <T extends Resource> T getResource( String name, ProgressListener monitor)
    //    throws IOException;

    /**
     * @return Returns the resources provided by this store.
     *     <p>The monitor is used to report the progress of loading resoures and report any warnings
     *     / errors that occur in doing so. Monitor may also be null.
     * @uml.property name="resources"
     * @uml.associationEnd multiplicity="(0 -1)" inverse="storeInfo:org.geoserver.catalog.Resource"
     */
    // <T extends Resource> Iterator<T> getResources(ProgressListener monitor) throws IOException;

}
