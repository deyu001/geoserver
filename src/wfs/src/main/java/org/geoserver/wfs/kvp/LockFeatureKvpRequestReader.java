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

package org.geoserver.wfs.kvp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.LockType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.Query;
import org.geotools.xsd.EMFUtils;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class LockFeatureKvpRequestReader extends BaseFeatureKvpRequestReader {

    public LockFeatureKvpRequestReader(GeoServer geoServer, FilterFactory filterFactory) {
        super(LockFeatureType.class, WfsFactory.eINSTANCE, geoServer, filterFactory);
    }

    protected <T> void querySet(EObject request, String property, List<T> values)
            throws WFSException {
        // no values specified, do nothing
        if (values == null) {
            return;
        }

        if ("typeName".equalsIgnoreCase(property)) {
            values = typenameWorkaround(values);
        }

        LockFeatureType lockFeature = (LockFeatureType) request;
        @SuppressWarnings("unchecked")
        EList<LockType> lock = lockFeature.getLock();

        int m = values.size();
        int n = lock.size();

        if ((m == 1) && (n > 1)) {
            // apply single value to all queries
            EMFUtils.set(lock, property, values.get(0));

            return;
        }

        // WfsFactory wfsFactory = (WfsFactory) getFactory();
        // match up sizes
        if (m > n) {
            if (n == 0) {
                // make same size, with empty objects
                for (int i = 0; i < m; i++) {
                    lock.add(WfsFactory.eINSTANCE.createLockType());
                }
            } else if (n == 1) {
                // clone single object up to
                EObject q = lock.get(0);

                for (int i = 1; i < m; i++) {
                    lock.add((LockType) EMFUtils.clone(q, WfsFactory.eINSTANCE, false));
                }

                return;
            } else {
                // illegal
                String msg = "Specified " + m + " " + property + " for " + n + " locks.";
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

        EMFUtils.set(lock, property, values);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> typenameWorkaround(List<T> values) {
        // in lock typename is not a list, it's a single qname
        values = (List) values.stream().map(o -> ((List) o).get(0)).collect(Collectors.toList());
        return values;
    }

    protected void buildStoredQueries(
            EObject request, List<URI> storedQueryIds, Map<String, Object> kvp) {
        throw new UnsupportedOperationException("No stored queries in WFS 1.0 or 1.1");
    }

    protected List<Query> getQueries(EObject eObject) {
        throw new UnsupportedOperationException();
    }

    protected void handleBBOX(Map kvp, EObject eObject) throws Exception {
        // set filter from bbox
        Envelope bbox = (Envelope) kvp.get("bbox");

        @SuppressWarnings("unchecked")
        List<LockType> queries = ((LockFeatureType) eObject).getLock();
        for (LockType lock : queries) {
            Filter filter = bboxFilter(bbox);
            lock.setFilter(filter);
        }
    }
}
