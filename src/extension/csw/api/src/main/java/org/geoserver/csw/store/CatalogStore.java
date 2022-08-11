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

package org.geoserver.csw.store;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;

/**
 * Interfaces to a storage for CSW record objects. By default it has to provide support for CSW
 * Dublin Core records (in their {@link CSWRecordDescriptor#RECORD_TYPE} form, but can publish more
 * feature types as well (e.g., ISO or ebRIM records)
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface CatalogStore {

    /** Returns the supported record types */
    RecordDescriptor[] getRecordDescriptors() throws IOException;

    /**
     * Queries a specific record type using the GeoTools Query object (which contains type name,
     * attribute selection
     */
    FeatureCollection<FeatureType, Feature> getRecords(
            Query q, Transaction t, RecordDescriptor outputRd) throws IOException;

    /**
     * Returns the number of records that {@link #getRecords(Query, Transaction, String)} would
     * return given the same query and transaction
     */
    int getRecordsCount(Query q, Transaction t, RecordDescriptor outputRd) throws IOException;

    /**
     * Returns the domain of an attribute in the specified record type.
     *
     * @param typeName The record type
     * @param attributeName The attribute
     * @return An iteration of possible values, or null if domain extraction for this attribute is
     *     not supported
     * @see {@link CatalogStoreCapabilities#getDomainQueriables(Name)} to get a list of the
     *     properties which the store supports the domain extraction from
     */
    CloseableIterator<String> getDomain(Name typeName, Name attributeName) throws IOException;

    /**
     * Adds a new record to the store. This method might not be supported, see {@link
     * CatalogStoreCapabilities#supportsTransactions()} to check if the store supports transactions
     */
    List<FeatureId> addRecord(Feature f, Transaction t) throws IOException;

    /**
     * Removes records from the store. This method might not be supported, see {@link
     * CatalogStoreCapabilities#supportsTransactions()} to check if the store supports transactions
     */
    void deleteRecord(Filter f, Transaction t) throws IOException;

    /**
     * Updates records in the store. This method might not be supported, see {@link
     * CatalogStoreCapabilities#supportsTransactions()} to check if the store supports transactions
     */
    void updateRecord(
            Name typeName,
            Name[] attributeNames,
            Object[] attributeValues,
            Filter filter,
            Transaction t)
            throws IOException;

    /**
     * Returns the repository item for the specified record id, or null if the repository item is
     * not found, or the operation is not supported
     */
    RepositoryItem getRepositoryItem(String recordId) throws IOException;

    /** Returns the store capabilities */
    CatalogStoreCapabilities getCapabilities();

    /** Maps a qualified name to it's equivalent property name for the backend store. */
    PropertyName translateProperty(RecordDescriptor rd, Name name);
}
