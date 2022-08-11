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

package org.geoserver.wfs;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import net.opengis.wfs20.FeatureCollectionType;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.GetPropertyValueType;
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.ValueCollectionType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.wfs.PropertyValueCollection;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

public class GetPropertyValue {

    Pattern FEATURE_ID_PATTERN = Pattern.compile("@(\\w+:)?id");

    GetFeature delegate;

    Catalog catalog;

    FilterFactory2 filterFactory;

    public GetPropertyValue(WFSInfo info, Catalog catalog, FilterFactory2 filterFactory) {
        delegate = new GetFeature(info, catalog);
        delegate.setFilterFactory(filterFactory);

        this.catalog = catalog;
        this.filterFactory = filterFactory;
    }

    /** @return NamespaceSupport from Catalog */
    public NamespaceSupport getNamespaceSupport() {
        NamespaceSupport ns = new NamespaceSupport();
        Iterator<NamespaceInfo> it = catalog.getNamespaces().iterator();
        while (it.hasNext()) {
            NamespaceInfo ni = it.next();
            ns.declarePrefix(ni.getPrefix(), ni.getURI());
        }
        return ns;
    }

    public ValueCollectionType run(GetPropertyValueType request) throws WFSException {

        if (request.getValueReference() == null) {
            throw new WFSException(request, "No valueReference specified", "MissingParameterValue")
                    .locator("valueReference");
        } else if ("".equals(request.getValueReference().trim())) {
            throw new WFSException(
                            request,
                            "ValueReference cannot be empty",
                            ServiceException.INVALID_PARAMETER_VALUE)
                    .locator("valueReference");
        }

        // do a getFeature request
        GetFeatureType getFeature = Wfs20Factory.eINSTANCE.createGetFeatureType();
        getFeature.setBaseUrl(request.getBaseUrl());
        getFeature.getAbstractQueryExpression().add(request.getAbstractQueryExpression());
        getFeature.setResolve(request.getResolve());
        getFeature.setResolveDepth(request.getResolveDepth());
        getFeature.setResolveTimeout(request.getResolveTimeout());
        getFeature.setCount(request.getCount());

        FeatureCollectionType fc =
                (FeatureCollectionType)
                        delegate.run(GetFeatureRequest.adapt(getFeature)).getAdaptee();

        QueryType query = (QueryType) request.getAbstractQueryExpression();
        QName typeName = (QName) query.getTypeNames().iterator().next();
        FeatureTypeInfo featureType =
                catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());

        try {

            PropertyName propertyName =
                    filterFactory.property(request.getValueReference(), getNamespaceSupport());
            PropertyName propertyNameNoIndexes =
                    filterFactory.property(
                            request.getValueReference().replaceAll("\\[.*\\]", ""),
                            getNamespaceSupport());
            AttributeDescriptor descriptor =
                    (AttributeDescriptor)
                            propertyNameNoIndexes.evaluate(featureType.getFeatureType());
            boolean featureIdRequest =
                    FEATURE_ID_PATTERN.matcher(request.getValueReference()).matches();
            if (descriptor == null && !featureIdRequest) {
                throw new WFSException(
                        request, "No such attribute: " + request.getValueReference());
            }

            // create value collection type from feature collection
            ValueCollectionType vc = Wfs20Factory.eINSTANCE.createValueCollectionType();
            vc.setTimeStamp(fc.getTimeStamp());
            vc.setNumberMatched(fc.getNumberMatched());
            vc.setNumberReturned(fc.getNumberReturned());
            vc.getMember()
                    .add(
                            new PropertyValueCollection(
                                    fc.getMember().iterator().next(), descriptor, propertyName));
            return vc;
        } catch (IOException e) {
            throw new WFSException(request, e);
        }
    }

    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }
}
