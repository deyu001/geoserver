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
import java.util.Calendar;
import java.util.List;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.WFSException;
import org.geotools.feature.FeatureCollection;

/**
 * Response object for a feature collection, most notably from a GetFeature request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class FeatureCollectionResponse extends RequestObject {

    private boolean getFeatureById = false;

    public static FeatureCollectionResponse adapt(Object adaptee) {
        if (adaptee instanceof FeatureCollectionType) {
            return new WFS11((EObject) adaptee);
        } else if (adaptee instanceof net.opengis.wfs20.FeatureCollectionType) {
            return new WFS20((EObject) adaptee);
        }
        return null;
    }

    protected FeatureCollectionResponse(EObject adaptee) {
        super(adaptee);
    }

    public String getLockId() {
        return eGet(adaptee, "lockId", String.class);
    }

    public void setLockId(String lockId) {
        eSet(adaptee, "lockId", lockId);
    }

    public Calendar getTimeStamp() {
        return eGet(adaptee, "timeStamp", Calendar.class);
    }

    public void setTimeStamp(Calendar timeStamp) {
        eSet(adaptee, "timeStamp", timeStamp);
    }

    public abstract FeatureCollectionResponse create();

    public abstract BigInteger getNumberOfFeatures();

    public abstract void setNumberOfFeatures(BigInteger n);

    public abstract BigInteger getTotalNumberOfFeatures();

    public abstract void setTotalNumberOfFeatures(BigInteger n);

    public abstract void setPrevious(String previous);

    public abstract String getPrevious();

    public abstract void setNext(String next);

    public abstract String getNext();

    public abstract List<FeatureCollection> getFeatures();

    public abstract void setFeatures(List<FeatureCollection> features);

    public abstract Object unadapt(Class target);

    public List<FeatureCollection> getFeature() {
        // alias
        return getFeatures();
    }

    public void setGetFeatureById(boolean getFeatureById) {
        this.getFeatureById = getFeatureById;
    }

    public boolean isGetFeatureById() {
        return getFeatureById;
    }

    public static class WFS11 extends FeatureCollectionResponse {
        BigInteger totalNumberOfFeatures;

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public FeatureCollectionResponse create() {
            return FeatureCollectionResponse.adapt(
                    ((WfsFactory) getFactory()).createFeatureCollectionType());
        }

        @Override
        public BigInteger getNumberOfFeatures() {
            return eGet(adaptee, "numberOfFeatures", BigInteger.class);
        }

        @Override
        public void setNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberOfFeatures", n);
        }

        @Override
        public BigInteger getTotalNumberOfFeatures() {
            return totalNumberOfFeatures;
        }

        @Override
        public void setTotalNumberOfFeatures(BigInteger n) {
            this.totalNumberOfFeatures = n;
        }

        @Override
        public String getPrevious() {
            // noop
            return null;
        }

        @Override
        public void setPrevious(String previous) {
            // noop
        }

        @Override
        public String getNext() {
            // noop
            return null;
        }

        @Override
        public void setNext(String next) {
            // noop
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<FeatureCollection> getFeatures() {
            return eGet(adaptee, "feature", List.class);
        }

        @Override
        public void setFeatures(List<FeatureCollection> features) {
            eSet(adaptee, "feature", features);
        }

        @Override
        @SuppressWarnings("unchecked") // EMF model without generics
        public Object unadapt(Class target) {
            if (target.equals(FeatureCollectionType.class)) {
                return adaptee;
            } else if (target.equals(net.opengis.wfs20.FeatureCollectionType.class)) {
                FeatureCollectionType source = (FeatureCollectionType) adaptee;
                net.opengis.wfs20.FeatureCollectionType result =
                        Wfs20Factory.eINSTANCE.createFeatureCollectionType();
                result.getMember().addAll(source.getFeature());
                result.setNumberReturned(source.getNumberOfFeatures());
                result.setLockId(source.getLockId());
                result.setTimeStamp(source.getTimeStamp());
                return result;
            } else {
                throw new WFSException(
                        "Cannot transform " + adaptee + " to the specified target class " + target);
            }
        }
    }

    public static class WFS20 extends FeatureCollectionResponse {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public FeatureCollectionResponse create() {
            return FeatureCollectionResponse.adapt(
                    ((Wfs20Factory) getFactory()).createFeatureCollectionType());
        }

        @Override
        public BigInteger getNumberOfFeatures() {
            return eGet(adaptee, "numberReturned", BigInteger.class);
        }

        @Override
        public void setNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberReturned", n);
        }

        @Override
        public BigInteger getTotalNumberOfFeatures() {
            BigInteger result = eGet(adaptee, "numberMatched", BigInteger.class);
            if (result != null && result.signum() < 0) return null;
            return result;
        }

        @Override
        public void setTotalNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberMatched", n);
        }

        @Override
        public String getPrevious() {
            return eGet(adaptee, "previous", String.class);
        }

        @Override
        public void setPrevious(String previous) {
            eSet(adaptee, "previous", previous);
        }

        @Override
        public String getNext() {
            return eGet(adaptee, "next", String.class);
        }

        @Override
        public void setNext(String next) {
            eSet(adaptee, "next", next);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<FeatureCollection> getFeatures() {
            return eGet(adaptee, "member", List.class);
        }

        @Override
        public void setFeatures(List<FeatureCollection> features) {
            eSet(adaptee, "member", features);
        }

        @Override
        @SuppressWarnings("unchecked") // EMF model without generics
        public Object unadapt(Class target) {
            if (target.equals(net.opengis.wfs20.FeatureCollectionType.class)) {
                return adaptee;
            } else if (target.equals(FeatureCollectionType.class)) {
                net.opengis.wfs20.FeatureCollectionType source =
                        (net.opengis.wfs20.FeatureCollectionType) adaptee;
                FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
                result.getFeature().addAll(source.getMember());
                result.setNumberOfFeatures(source.getNumberReturned());
                result.setLockId(source.getLockId());
                result.setTimeStamp(source.getTimeStamp());
                return result;
            } else {
                throw new WFSException(
                        "Cannot transform " + adaptee + " to the specified target class " + target);
            }
        }
    }
}
