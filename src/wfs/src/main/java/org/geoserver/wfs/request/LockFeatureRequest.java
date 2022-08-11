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

package org.geoserver.wfs.request;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.Wfs20Factory;
import net.opengis.wfs20.Wfs20Package;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * WFS LockFeature request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class LockFeatureRequest extends RequestObject {

    public static LockFeatureRequest adapt(Object request) {
        if (request instanceof LockFeatureType) {
            return new WFS11((EObject) request);
        } else if (request instanceof net.opengis.wfs20.LockFeatureType) {
            return new WFS20((EObject) request);
        }
        return null;
    }

    protected LockFeatureRequest(EObject adaptee) {
        super(adaptee);
    }

    public BigInteger getExpiry() {
        return eGet(adaptee, "expiry", BigInteger.class);
    }

    public void setExpiry(BigInteger expiry) {
        eSet(adaptee, "expiry", expiry);
    }

    public abstract List<Lock> getLocks();

    public abstract void addLock(Lock lock);

    public abstract boolean isLockActionSome();

    public abstract void setLockActionSome();

    public abstract boolean isLockActionAll();

    public abstract void setLockActionAll();

    public abstract Lock createLock();

    public abstract LockFeatureResponse createResponse();

    public abstract List<EObject> getAdaptedQueries();

    public abstract RequestObject createQuery();

    public abstract List<Query> getQueries();

    public static class WFS11 extends LockFeatureRequest {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Lock> getLocks() {
            List<Lock> locks = new ArrayList<>();
            for (Object lock : eGet(adaptee, "lock", List.class)) {
                locks.add(new Lock.WFS11((EObject) lock));
            }
            return locks;
        }

        @Override
        public void addLock(Lock lock) {
            @SuppressWarnings("unchecked")
            List<EObject> locks = eGet(adaptee, "lock", List.class);
            locks.add(lock.getAdaptee());
        }

        @Override
        public boolean isLockActionAll() {
            return ((LockFeatureType) adaptee).getLockAction() == AllSomeType.ALL_LITERAL;
        }

        @Override
        public void setLockActionAll() {
            ((LockFeatureType) adaptee).setLockAction(AllSomeType.ALL_LITERAL);
        }

        @Override
        public boolean isLockActionSome() {
            return ((LockFeatureType) adaptee).getLockAction() == AllSomeType.SOME_LITERAL;
        }

        @Override
        public void setLockActionSome() {
            ((LockFeatureType) adaptee).setLockAction(AllSomeType.SOME_LITERAL);
        }

        @Override
        public Lock createLock() {
            return new Lock.WFS11(((WfsFactory) getFactory()).createLockType());
        }

        @Override
        public LockFeatureResponse createResponse() {
            return new LockFeatureResponse.WFS11(
                    ((WfsFactory) getFactory()).createLockFeatureResponseType());
        }

        @Override
        public List<EObject> getAdaptedQueries() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RequestObject createQuery() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Query> getQueries() {
            throw new UnsupportedOperationException();
        }
    }

    public static class WFS20 extends LockFeatureRequest {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Lock> getLocks() {
            List<Lock> locks = new ArrayList<>();
            for (Object lock : eGet(adaptee, "abstractQueryExpression", List.class)) {
                locks.add(new Lock.WFS20((EObject) lock));
            }
            return locks;
        }

        @Override
        public void addLock(Lock lock) {
            ((FeatureMap) eGet(adaptee, "abstractQueryExpressionGroup", List.class))
                    .add(Wfs20Package.Literals.DOCUMENT_ROOT__QUERY, lock.getAdaptee());
        }

        @Override
        public boolean isLockActionAll() {
            return ((net.opengis.wfs20.LockFeatureType) adaptee).getLockAction()
                    == net.opengis.wfs20.AllSomeType.ALL;
        }

        @Override
        public void setLockActionAll() {
            ((net.opengis.wfs20.LockFeatureType) adaptee)
                    .setLockAction(net.opengis.wfs20.AllSomeType.ALL);
        }

        @Override
        public boolean isLockActionSome() {
            return ((net.opengis.wfs20.LockFeatureType) adaptee).getLockAction()
                    == net.opengis.wfs20.AllSomeType.SOME;
        }

        @Override
        public void setLockActionSome() {
            ((net.opengis.wfs20.LockFeatureType) adaptee)
                    .setLockAction(net.opengis.wfs20.AllSomeType.SOME);
        }

        @Override
        public Lock createLock() {
            return new Lock.WFS20(((Wfs20Factory) getFactory()).createQueryType());
        }

        @Override
        public LockFeatureResponse createResponse() {
            return new LockFeatureResponse.WFS20(
                    ((Wfs20Factory) getFactory()).createLockFeatureResponseType());
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<EObject> getAdaptedQueries() {
            return eGet(adaptee, "abstractQueryExpression", List.class);
        }

        @Override
        public Query createQuery() {
            return new Query.WFS20(((Wfs20Factory) getFactory()).createQueryType());
        }

        @Override
        public List<Query> getQueries() {
            List<EObject> adaptedQueries = getAdaptedQueries();
            return GetFeatureRequest.WFS20.getQueries(adaptedQueries);
        }
    }
}
