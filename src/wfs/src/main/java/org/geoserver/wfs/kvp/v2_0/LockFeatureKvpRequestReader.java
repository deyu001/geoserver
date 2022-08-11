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

package org.geoserver.wfs.kvp.v2_0;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.opengis.wfs20.LockFeatureType;
import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.ParameterType;
import net.opengis.wfs20.StoredQueryType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.StoredQuery;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.kvp.BaseFeatureKvpRequestReader;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.xsd.EMFUtils;
import org.opengis.filter.FilterFactory;

public class LockFeatureKvpRequestReader extends BaseFeatureKvpRequestReader {

    public LockFeatureKvpRequestReader(GeoServer geoServer, FilterFactory filterFactory) {
        super(LockFeatureType.class, Wfs20Factory.eINSTANCE, geoServer, filterFactory);
    }

    protected <T> void querySet(EObject request, String property, List<T> values)
            throws WFSException {
        // no values specified, do nothing
        if (values == null) {
            return;
        }

        LockFeatureRequest req = LockFeatureRequest.adapt(request);

        // this is a 2.0 only parser
        if ("typeName".equals(property)) {
            property = "typeNames";
        }

        List<EObject> query = req.getAdaptedQueries();

        int m = values.size();
        int n = query.size();

        if ((m == 1) && (n > 1)) {
            // apply single value to all queries
            EMFUtils.set(query, property, values.get(0));

            return;
        }

        // WfsFactory wfsFactory = (WfsFactory) getFactory();
        // match up sizes
        if (m > n) {
            if (n == 0) {
                // make same size, with empty objects
                for (int i = 0; i < m; i++) {
                    query.add(req.createQuery().getAdaptee());
                }
            } else if (n == 1) {
                // clone single object up to
                EObject q = query.get(0);

                for (int i = 1; i < m; i++) {
                    query.add(EMFUtils.clone(q, req.getFactory(), false));
                }

                return;
            } else {
                // illegal
                String msg = "Specified " + m + " " + property + " for " + n + " queries.";
                throw new WFSException(request, msg);
            }
        }
        if (m < n) {
            // fill the rest with nulls
            List<T> newValues = new ArrayList<>();
            newValues.addAll(values);
            for (int i = 0; i < n - m; i++) {
                newValues.add(null);
            }
            values = newValues;
        }

        EMFUtils.set(query, property, values);
    }

    protected void buildStoredQueries(
            EObject request, List<URI> storedQueryIds, Map<String, Object> kvp) {
        LockFeatureRequest req = LockFeatureRequest.adapt(request);

        if (!(req instanceof LockFeatureRequest.WFS20)) {
            throw new WFSException(req, "Stored queries only supported in WFS 2.0+");
        }

        StoredQueryProvider sqp =
                new StoredQueryProvider(
                        catalog,
                        getWFS(),
                        geoServer.getGlobal().isAllowStoredQueriesPerWorkspace());
        for (URI storedQueryId : storedQueryIds) {
            StoredQuery sq = sqp.getStoredQuery(storedQueryId.toString());
            if (sq == null) {
                WFSException exception =
                        new WFSException(
                                req,
                                "No such stored query: " + storedQueryId,
                                ServiceException.INVALID_PARAMETER_VALUE);
                exception.setLocator("STOREDQUERY_ID");
                throw exception;
            }

            // JD: since stored queries are 2.0 only we will create 2.0 model objects directly...
            // once
            // the next version of wfs comes out (and if they keep stored queries around) we will
            // have
            // to abstract stored query away with a request object adapter
            Wfs20Factory factory = (Wfs20Factory) req.getFactory();
            StoredQueryType storedQuery = factory.createStoredQueryType();
            storedQuery.setId(storedQueryId.toString());

            // look for parameters in the kvp map
            for (ParameterExpressionType p : sq.getQuery().getParameter()) {
                if (kvp.containsKey(p.getName())) {
                    ParameterType param = factory.createParameterType();
                    param.setName(p.getName());
                    param.setValue(kvp.get(p.getName()).toString());
                    storedQuery.getParameter().add(param);
                }
            }

            req.getAdaptedQueries().add(storedQuery);
        }
    }

    protected List<Query> getQueries(EObject eObject) {
        return LockFeatureRequest.adapt(eObject).getQueries();
    }
}
