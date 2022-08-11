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

package org.geoserver.csw;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.QueryType;
import net.opengis.cat.csw20.ResultType;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.response.CSWRecordsResult;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.feature.CompositeFeatureCollection;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.Types;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

/**
 * Runs the GetRecords request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GetRecords {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public static final Hints.Key KEY_BASEURL = new Hints.Key(String.class);

    CSWInfo csw;

    CatalogStore store;

    private List<RecordDescriptor> recordDescriptors;

    protected static class WrappedQuery {
        Query query;
        RecordDescriptor rd;

        public WrappedQuery(Query query, RecordDescriptor rd) {
            this.query = query;
            this.rd = rd;
        }
    }

    public GetRecords(CSWInfo csw, CatalogStore store, List<RecordDescriptor> recordDescriptors) {
        this.csw = csw;
        this.store = store;
        this.recordDescriptors = recordDescriptors;
    }

    public CSWRecordsResult run(GetRecordsType request) {
        // mark the time the request started
        Date timestamp = new Date();

        try {
            // build the queries
            List<RecordDescriptor> outputRd = getRecordDescriptors(request);
            QueryType cswQuery = (QueryType) request.getQuery();
            List<WrappedQuery> queries = toGtQueries(outputRd, cswQuery, request);
            // see how many records we have to return
            int maxRecords;
            if (request.getMaxRecords() == null) {
                maxRecords = 10;
            } else {
                maxRecords = request.getMaxRecords();
            }

            // get and check the offset (which is 1 based, but our API is 0 based)
            int offset = request.getStartPosition() == null ? 0 : request.getStartPosition() - 1;
            if (offset < 0) {
                throw new ServiceException(
                        "startPosition must be a positive number",
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "startPosition");
            }

            // and check what kind of result is desired
            ResultType resultType = request.getResultType();
            if (maxRecords == 0 && resultType == ResultType.RESULTS) {
                resultType = ResultType.HITS;
            }

            // compute the number of records matched (in validate mode this is also a quick way
            // to check the request)
            int numberOfRecordsMatched = 0;
            int[] counts = new int[queries.size()];
            for (int i = 0; i < queries.size(); i++) {
                counts[i] =
                        store.getRecordsCount(
                                queries.get(i).query, Transaction.AUTO_COMMIT, queries.get(i).rd);
                numberOfRecordsMatched += counts[i];
            }

            ElementSetType elementSet = getElementSet(cswQuery);

            int numberOfRecordsReturned = 0;
            int nextRecord = 0;
            FeatureCollection records = null;
            if (resultType != ResultType.VALIDATE) {
                // compute the number of records we're returning and the next record
                if (offset > numberOfRecordsMatched) {
                    numberOfRecordsReturned = 0;
                    nextRecord = 0;
                } else if (numberOfRecordsMatched - offset <= maxRecords) {
                    numberOfRecordsReturned = numberOfRecordsMatched - offset;
                    nextRecord = 0;
                } else {
                    numberOfRecordsReturned = maxRecords;
                    // mind, nextRecord is 1 based too
                    nextRecord = offset + numberOfRecordsReturned + 1;
                }

                // time to run the queries if we are not in hits mode
                if (resultType == ResultType.RESULTS) {
                    if (resultType != ResultType.HITS) {
                        List<FeatureCollection<FeatureType, Feature>> results = new ArrayList<>();
                        for (int i = 0; i < queries.size() && maxRecords > 0; i++) {
                            WrappedQuery q = queries.get(i);
                            int remaining = counts[i] - offset;
                            if (offset > 0) {
                                if (offset > counts[i]) {
                                    // skip the query altogether
                                    offset -= counts[i];
                                    continue;
                                } else {
                                    q.query.setStartIndex(offset);
                                    offset = 0;
                                }
                            }

                            if (maxRecords > 0) {
                                q.query.setMaxFeatures(maxRecords);
                                maxRecords -= remaining;
                            } else {
                                // skip the query, we already have enough results
                                continue;
                            }

                            results.add(store.getRecords(q.query, Transaction.AUTO_COMMIT, q.rd));
                        }

                        if (results.size() == 1) {
                            records = results.get(0);
                        } else if (results.size() > 1) {
                            records = new CompositeFeatureCollection<>(results);
                        }
                    }
                }
            }

            // in case this is a hits request we are actually not returning any record
            if (resultType == ResultType.HITS) {
                numberOfRecordsReturned = 0;
            }

            CSWRecordsResult result =
                    new CSWRecordsResult(
                            elementSet,
                            request.getOutputSchema(),
                            numberOfRecordsMatched,
                            numberOfRecordsReturned,
                            nextRecord,
                            timestamp,
                            records);
            return result;
        } catch (IOException e) {
            throw new ServiceException("Request failed due to: " + e.getMessage(), e);
        }
    }

    private List<WrappedQuery> toGtQueries(
            List<RecordDescriptor> outputRds, QueryType query, GetRecordsType request)
            throws IOException {
        // prepare to build the queries
        Filter filter = query.getConstraint() != null ? query.getConstraint().getFilter() : null;
        Set<Name> supportedTypes = getSupportedTypes();

        // the CSW specification expects like filters to be case insensitive (by CITE tests)
        // but we default to have filters case sensitive instead
        if (filter != null) {
            filter = (Filter) filter.accept(new CaseInsenstiveFilterTransformer(), null);
        }

        // build one query per type name, forgetting about paging for the time being
        List<WrappedQuery> result = new ArrayList<>();
        for (RecordDescriptor outputRd : outputRds) {
            for (QName qName : query.getTypeNames()) {
                Name typeName = new NameImpl(qName);
                if (!supportedTypes.contains(typeName)) {
                    throw new ServiceException(
                            "Unsupported record type " + typeName,
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "typeNames");
                }

                RecordDescriptor rd = getRecordDescriptor(typeName);

                Query q = new Query(typeName.getLocalPart());
                q.setFilter(filter);
                q.setProperties(getPropertyNames(outputRd, query));
                q.setSortBy(query.getSortBy());
                try {
                    q.setNamespace(new URI(typeName.getNamespaceURI()));
                } catch (URISyntaxException e) {
                }

                // perform some necessary query adjustments
                Query adapted = rd.adaptQuery(q);

                // the specification demands that we throw an error if a spatial operator
                // is used against a non spatial property
                if (q.getFilter() != null) {
                    rd.verifySpatialFilters(q.getFilter());
                }

                // smuggle base url
                adapted.getHints().put(KEY_BASEURL, request.getBaseUrl());

                result.add(new WrappedQuery(adapted, outputRd));
            }
        }

        return result;
    }

    private List<PropertyName> getPropertyNames(RecordDescriptor rd, QueryType query) {
        if (query.getElementName() != null && !query.getElementName().isEmpty()) {
            // turn the QName into PropertyName. We don't do any verification cause the
            // elements in the actual feature could be parts of substitution groups
            // of the elements in the feature's schema
            List<PropertyName> result = new ArrayList<>();
            for (QName qn : query.getElementName()) {
                result.add(store.translateProperty(rd, Types.toTypeName(qn)));
            }
            return result;
        } else {
            ElementSetType elementSet = getElementSet(query);
            List<Name> properties = rd.getPropertiesForElementSet(elementSet);
            if (properties != null) {
                List<PropertyName> result = new ArrayList<>();
                for (Name pn : properties) {
                    result.add(store.translateProperty(rd, pn));
                }
                return result;
            } else {
                // the profile is the full one
                return null;
            }
        }
    }

    private ElementSetType getElementSet(QueryType query) {
        if (query.getElementName() != null && query.getElementName().size() > 0) {
            return ElementSetType.FULL;
        }
        ElementSetType elementSet =
                query.getElementSetName() != null ? query.getElementSetName().getValue() : null;
        if (elementSet == null) {
            // the default is "summary"
            elementSet = ElementSetType.SUMMARY;
        }
        return elementSet;
    }

    private Set<Name> getSupportedTypes() throws IOException {
        Set<Name> result = new HashSet<>();
        for (RecordDescriptor rd : store.getRecordDescriptors()) {
            result.add(rd.getFeatureDescriptor().getName());
        }

        return result;
    }

    /**
     * Search for the record descriptor maching the typename, throws a service exception in case
     * none is found
     */
    private RecordDescriptor getRecordDescriptor(Name typeName) {
        if (typeName == null) {
            return CSWRecordDescriptor.getInstance();
        }

        for (RecordDescriptor rd : recordDescriptors) {
            if (typeName.equals(rd.getFeatureDescriptor().getName())) {
                return rd;
            }
        }

        throw new ServiceException(
                "Unknown type: " + typeName, ServiceException.INVALID_PARAMETER_VALUE, "typeNames");
    }

    /**
     * Search for the record descriptor maching the request, throws a service exception in case none
     * is found
     */
    protected List<RecordDescriptor> getRecordDescriptors(GetRecordsType request) {
        String outputSchema = request.getOutputSchema();
        if (outputSchema == null) {
            outputSchema = CSW.NAMESPACE;
            request.setOutputFormat(CSW.NAMESPACE);
        }

        List<RecordDescriptor> list = new ArrayList<>();
        for (RecordDescriptor rd : recordDescriptors) {
            if (outputSchema.equals(rd.getOutputSchema())) {
                list.add(rd);
            }
        }

        if (list.isEmpty()) {
            throw new ServiceException(
                    "Cannot encode records in output schema " + outputSchema,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "outputSchema");
        }
        return list;
    }
}
