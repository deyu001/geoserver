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
import java.util.Collection;
import java.util.List;
import net.opengis.wfs.ActionType;
import net.opengis.wfs.InsertedFeatureType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.CreatedOrModifiedFeatureType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.opengis.filter.identity.FeatureId;

public abstract class TransactionResponse extends RequestObject {

    public static TransactionResponse adapt(Object request) {
        if (request instanceof TransactionResponseType) {
            return new WFS11((EObject) request);
        } else if (request instanceof net.opengis.wfs20.TransactionResponseType) {
            return new WFS20((EObject) request);
        }
        return null;
    }

    protected TransactionResponse(EObject adaptee) {
        super(adaptee);
    }

    public BigInteger getTotalInserted() {
        return eGet(adaptee, "transactionSummary.totalInserted", BigInteger.class);
    }

    public void setTotalInserted(BigInteger inserted) {
        eSet(adaptee, "transactionSummary.totalInserted", inserted);
    }

    public BigInteger getTotalUpdated() {
        return eGet(adaptee, "transactionSummary.totalUpdated", BigInteger.class);
    }

    public void setTotalUpdated(BigInteger updated) {
        eSet(adaptee, "transactionSummary.totalUpdated", updated);
    }

    public BigInteger getTotalDeleted() {
        return eGet(adaptee, "transactionSummary.totalDeleted", BigInteger.class);
    }

    public void setTotalDeleted(BigInteger deleted) {
        eSet(adaptee, "transactionSummary.totalDeleted", deleted);
    }

    public BigInteger getTotalReplaced() {
        return eGet(adaptee, "transactionSummary.totalReplaced", BigInteger.class);
    }

    public void setTotalReplaced(BigInteger replaced) {
        eSet(adaptee, "transactionSummary.totalReplaced", replaced);
    }

    public List getInsertedFeatures() {
        return eGet(adaptee, "insertResults.feature", List.class);
    }

    public abstract void setHandle(String handle);

    public abstract void addInsertedFeature(String handle, FeatureId id);

    public abstract void addUpdatedFeatures(String handle, Collection<FeatureId> ids);

    public abstract void addReplacedFeatures(String handle, Collection<FeatureId> ids);

    public abstract void addAction(String code, String locator, String message);

    public static class WFS11 extends TransactionResponse {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public void setHandle(String handle) {
            eSet(adaptee, "transactionResults.handle", handle);
        }

        @SuppressWarnings("unchecked") // EMF model without generics
        public void addInsertedFeature(String handle, FeatureId featureId) {
            InsertedFeatureType insertedFeature =
                    ((WfsFactory) getFactory()).createInsertedFeatureType();
            insertedFeature.setHandle(handle);
            insertedFeature.getFeatureId().add(featureId);

            ((TransactionResponseType) adaptee)
                    .getInsertResults()
                    .getFeature()
                    .add(insertedFeature);
        }

        @Override
        public void addUpdatedFeatures(String handle, Collection<FeatureId> id) {
            // no-op
        }

        @Override
        public void addReplacedFeatures(String handle, Collection<FeatureId> ids) {
            // no-op
        }

        @Override
        @SuppressWarnings("unchecked") // EMF model without generics
        public void addAction(String code, String locator, String message) {
            // transaction failed, rollback
            ActionType action = ((WfsFactory) getFactory()).createActionType();
            action.setCode(code);
            action.setLocator(locator);
            action.setMessage(message);

            ((TransactionResponseType) adaptee).getTransactionResults().getAction().add(action);
        }

        public static TransactionResponseType unadapt(TransactionResponse response) {
            if (response instanceof WFS11) {
                return (TransactionResponseType) response.getAdaptee();
            }
            return null;
        }
    }

    public static class WFS20 extends TransactionResponse {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public void setHandle(String handle) {
            // no-op
        }

        @Override
        public void addInsertedFeature(String handle, FeatureId featureId) {
            CreatedOrModifiedFeatureType inserted =
                    ((Wfs20Factory) getFactory()).createCreatedOrModifiedFeatureType();
            inserted.setHandle(handle);
            inserted.getResourceId().add(featureId);

            net.opengis.wfs20.TransactionResponseType tr =
                    (net.opengis.wfs20.TransactionResponseType) adaptee;
            if (tr.getInsertResults() == null) {
                tr.setInsertResults(((Wfs20Factory) getFactory()).createActionResultsType());
            }

            tr.getInsertResults().getFeature().add(inserted);
        }

        @Override
        public void addUpdatedFeatures(String handle, Collection<FeatureId> ids) {
            CreatedOrModifiedFeatureType updated =
                    ((Wfs20Factory) getFactory()).createCreatedOrModifiedFeatureType();
            updated.setHandle(handle);
            updated.getResourceId().addAll(ids);

            net.opengis.wfs20.TransactionResponseType tr =
                    (net.opengis.wfs20.TransactionResponseType) adaptee;
            if (tr.getUpdateResults() == null) {
                tr.setUpdateResults(((Wfs20Factory) getFactory()).createActionResultsType());
            }

            tr.getUpdateResults().getFeature().add(updated);
        }

        @Override
        public void addReplacedFeatures(String handle, Collection<FeatureId> ids) {
            CreatedOrModifiedFeatureType updated =
                    ((Wfs20Factory) getFactory()).createCreatedOrModifiedFeatureType();
            updated.setHandle(handle);
            updated.getResourceId().addAll(ids);

            net.opengis.wfs20.TransactionResponseType tr =
                    (net.opengis.wfs20.TransactionResponseType) adaptee;
            if (tr.getReplaceResults() == null) {
                tr.setReplaceResults(((Wfs20Factory) getFactory()).createActionResultsType());
            }

            tr.getReplaceResults().getFeature().add(updated);
        }

        @Override
        public void addAction(String code, String locator, String message) {
            // no-op
        }
    }
}
