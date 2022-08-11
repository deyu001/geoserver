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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.opengis.ows10.DomainType;
import net.opengis.ows10.Ows10Factory;
import org.geoserver.csw.records.RecordDescriptor;
import org.opengis.feature.type.Name;

/**
 * Represents the capabilities of a {@link CatalogStore}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CatalogStoreCapabilities {

    public static final String CSW_VERSION = "2.0.2";

    protected Map<Name, RecordDescriptor> descriptors;

    protected Map<String, List<DomainType>> operationParameters = new HashMap<>();
    protected Map<String, List<DomainType>> operationConstraints = new HashMap<>();

    public CatalogStoreCapabilities(Map<Name, RecordDescriptor> descriptors) {
        this.descriptors = descriptors;
        initialize();
    }

    private void initialize() {
        Ows10Factory owsf = Ows10Factory.eINSTANCE;

        /** OperationMetadata */
        operationParameters.put("OperationsMetadata", new LinkedList<>());
        operationConstraints.put("OperationsMetadata", new LinkedList<>());

        // - Parameters
        DomainType opMetadataParam1 = owsf.createDomainType();
        DomainType opMetadataParam2 = owsf.createDomainType();
        opMetadataParam1.setName("service");
        opMetadataParam1.getValue().add("http://www.opengis.net/cat/csw/2.0.2");
        opMetadataParam2.setName("version");
        opMetadataParam2.getValue().add(CSW_VERSION);
        operationParameters.get("OperationsMetadata").add(opMetadataParam1);
        operationParameters.get("OperationsMetadata").add(opMetadataParam2);

        // - Constraints
        DomainType opMetadataConstraint1 = owsf.createDomainType();
        opMetadataConstraint1.setName("PostEncoding");
        opMetadataConstraint1.getValue().add("XML");
        operationConstraints.get("OperationsMetadata").add(opMetadataConstraint1);

        /** GetCapabilities */
        operationParameters.put("GetCapabilities", new LinkedList<>());
        operationConstraints.put("GetCapabilities", new LinkedList<>());

        // - Parameters
        DomainType getCapabilitiesParam = owsf.createDomainType();
        getCapabilitiesParam.setName("sections");
        getCapabilitiesParam.getValue().add("ServiceIdentification");
        getCapabilitiesParam.getValue().add("ServiceProvider");
        getCapabilitiesParam.getValue().add("OperationsMetadata");
        getCapabilitiesParam.getValue().add("Filter_Capabilities");
        operationParameters.get("GetCapabilities").add(getCapabilitiesParam);

        // - Constraints
        DomainType getCapabilitiesConstraint = owsf.createDomainType();
        getCapabilitiesConstraint.setName("PostEncoding");
        getCapabilitiesConstraint.getValue().add("XML");
        operationConstraints.get("GetCapabilities").add(getCapabilitiesConstraint);

        /** DescribeRecord */
        operationParameters.put("DescribeRecord", new LinkedList<>());
        operationConstraints.put("DescribeRecord", new LinkedList<>());

        // prepare typenames and schema's
        List<String> typeNames = new ArrayList<>();
        List<String> outputSchemas = new ArrayList<>();
        for (RecordDescriptor rd : descriptors.values()) {
            typeNames.add(rd.getFeatureDescriptor().getName().toString());
            outputSchemas.add(rd.getOutputSchema());
        }

        // - Parameters
        DomainType describeRecordParam1 = owsf.createDomainType();
        DomainType describeRecordParam2 = owsf.createDomainType();
        DomainType describeRecordParam3 = owsf.createDomainType();
        describeRecordParam1.setName("typeName");
        describeRecordParam1.getValue().addAll(typeNames);
        describeRecordParam2.setName("outputFormat");
        describeRecordParam2.getValue().add("application/xml");
        describeRecordParam3.setName("schemaLanguage");
        describeRecordParam3.getValue().add("http://www.w3.org/TR/xmlschema-1/");
        operationParameters.get("DescribeRecord").add(describeRecordParam1);
        operationParameters.get("DescribeRecord").add(describeRecordParam2);
        operationParameters.get("DescribeRecord").add(describeRecordParam3);

        // - Constraints
        DomainType describeRecordConstraint = owsf.createDomainType();
        describeRecordConstraint.setName("PostEncoding");
        describeRecordConstraint.getValue().add("XML");
        operationConstraints.get("DescribeRecord").add(describeRecordConstraint);

        /** GetRecords */
        operationParameters.put("GetRecords", new LinkedList<>());
        operationConstraints.put("GetRecords", new LinkedList<>());

        // - Parameters
        DomainType getRecordsParam1 = owsf.createDomainType();
        DomainType getRecordsParam2 = owsf.createDomainType();
        DomainType getRecordsParam3 = owsf.createDomainType();
        DomainType getRecordsParam4 = owsf.createDomainType();
        DomainType getRecordsParam5 = owsf.createDomainType();
        getRecordsParam1.setName("resultType");
        getRecordsParam1.getValue().add("hits");
        getRecordsParam1.getValue().add("results");
        getRecordsParam1.getValue().add("validate");
        getRecordsParam2.setName("outputFormat");
        getRecordsParam2.getValue().add("application/xml");
        getRecordsParam3.setName("outputSchema");
        getRecordsParam3.getValue().addAll(outputSchemas);
        getRecordsParam4.setName("typeNames");
        getRecordsParam4.getValue().addAll(typeNames);
        getRecordsParam5.setName("CONSTRAINTLANGUAGE");
        getRecordsParam5.getValue().add("FILTER");
        getRecordsParam5.getValue().add("CQL_TEXT");
        operationParameters.get("GetRecords").add(getRecordsParam1);
        operationParameters.get("GetRecords").add(getRecordsParam2);
        operationParameters.get("GetRecords").add(getRecordsParam3);
        operationParameters.get("GetRecords").add(getRecordsParam4);
        operationParameters.get("GetRecords").add(getRecordsParam5);

        // - Constraints
        DomainType getRecordConstraint1 = owsf.createDomainType();
        getRecordConstraint1.setName("PostEncoding");
        getRecordConstraint1.getValue().add("XML");
        operationConstraints.get("GetRecords").add(getRecordConstraint1);

        /** GetRecordById */
        operationParameters.put("GetRecordById", new LinkedList<>());
        operationConstraints.put("GetRecordById", new LinkedList<>());

        // - Parameters
        DomainType getRecordByIdParam1 = owsf.createDomainType();
        DomainType getRecordByIdParam2 = owsf.createDomainType();
        DomainType getRecordByIdParam3 = owsf.createDomainType();
        DomainType getRecordByIdParam4 = owsf.createDomainType();
        getRecordByIdParam1.setName("resultType");
        getRecordByIdParam1.getValue().add("hits");
        getRecordByIdParam1.getValue().add("results");
        getRecordByIdParam1.getValue().add("validate");
        getRecordByIdParam2.setName("outputFormat");
        getRecordByIdParam2.getValue().add("application/xml");
        getRecordByIdParam3.setName("outputSchema");
        getRecordByIdParam3.getValue().addAll(outputSchemas);
        getRecordByIdParam4.setName("ElementSetName");
        getRecordByIdParam4.getValue().add("brief");
        getRecordByIdParam4.getValue().add("summary");
        getRecordByIdParam4.getValue().add("full");
        operationParameters.get("GetRecordById").add(getRecordByIdParam1);
        operationParameters.get("GetRecordById").add(getRecordByIdParam2);
        operationParameters.get("GetRecordById").add(getRecordByIdParam3);
        operationParameters.get("GetRecordById").add(getRecordByIdParam4);

        // - Constraints
        DomainType getRecordByIdConstraint1 = owsf.createDomainType();
        getRecordByIdConstraint1.setName("PostEncoding");
        getRecordByIdConstraint1.getValue().add("XML");
        operationConstraints.get("GetRecordById").add(getRecordByIdConstraint1);

        /** GetDomain */
        operationParameters.put("GetDomain", new LinkedList<>());
        operationConstraints.put("GetDomain", new LinkedList<>());

        // - Parameters
        DomainType getDomainParam1 = owsf.createDomainType();
        DomainType getDomainParam2 = owsf.createDomainType();
        getDomainParam1.setName("parameterName");
        getDomainParam1.getValue().add("xsd:anyURI");
        getDomainParam2.setName("propertyName");
        getDomainParam2.getValue().add("xsd:anyURI");
        operationParameters.get("GetDomain").add(getDomainParam1);
        operationParameters.get("GetDomain").add(getDomainParam2);

        /** Transaction */
        operationParameters.put("Transaction", new LinkedList<>());
        operationConstraints.put("Transaction", new LinkedList<>());
    }

    /** True if the store supports transactions (insert, update, delete), false otherwise */
    public boolean supportsTransactions() {
        return false;
    }

    /**
     * Returns the list of queriable properties supported by this implementation for the given type
     * name (empty by default)
     *
     * @param typeName Qualified name (with namespace)
     */
    public List<Name> getQueriables(Name typeName) {
        return descriptors.get(typeName).getQueryables();
    }

    /**
     * Returns the list of queriable properties for which an enumeration of the domain makes sense
     *
     * @param typeName Qualified name (with namespace)
     */
    public List<Name> getDomainQueriables(Name typeName) {
        return descriptors.get(typeName).getQueryables();
    }

    /**
     * Returns true if GetRepositoryItem is supported on the specified type
     *
     * @param typeName Qualified name (with namespace)
     */
    public boolean supportsGetRepositoryItem(Name typeName) {
        return false;
    }

    public Map<String, List<DomainType>> getOperationParameters() {
        return operationParameters;
    }

    public Map<String, List<DomainType>> getOperationConstraints() {
        return operationConstraints;
    }
}
