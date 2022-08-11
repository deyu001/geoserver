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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.cat.csw20.GetDomainType;
import net.opengis.ows10.DomainType;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Runs the GetDomain request
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetDomain {

    CSWInfo csw;

    CatalogStore store;

    Map<Name, Name> attributeTypeMap = new HashMap<>();

    NamespaceSupport ns = new NamespaceSupport();

    public GetDomain(CSWInfo csw, CatalogStore store) {
        this.csw = csw;
        this.store = store;

        try {
            for (RecordDescriptor rd : store.getRecordDescriptors()) {
                for (Name prop :
                        store.getCapabilities()
                                .getDomainQueriables(rd.getFeatureDescriptor().getName())) {
                    attributeTypeMap.put(prop, rd.getFeatureDescriptor().getName());
                    Enumeration declaredPrefixes = rd.getNamespaceSupport().getDeclaredPrefixes();
                    while (declaredPrefixes.hasMoreElements()) {
                        String prefix = (String) declaredPrefixes.nextElement();
                        String uri = rd.getNamespaceSupport().getURI(prefix);
                        ns.declarePrefix(prefix, uri);
                    }
                }
            }
        } catch (IOException e) {
            throw new ServiceException(
                    e, "Failed to retrieve the domain values", ServiceException.NO_APPLICABLE_CODE);
        }
    }

    /** Returns the requested feature types */
    public CloseableIterator<String> run(GetDomainType request) {
        try {
            List<String> result = new ArrayList<>();
            if (request.getParameterName() != null && !request.getParameterName().isEmpty()) {
                String parameterName = request.getParameterName();
                if (parameterName.indexOf(".") > 0) {
                    final String operation = parameterName.split("\\.")[0];
                    final String parameter = parameterName.split("\\.")[1];

                    if (store.getCapabilities().getOperationParameters().get(operation) != null) {
                        for (DomainType param :
                                store.getCapabilities().getOperationParameters().get(operation)) {
                            if (param.getName().equalsIgnoreCase(parameter)) {
                                for (Object value : param.getValue()) {
                                    result.add((String) value);
                                }
                            }
                        }
                    }
                }
            }

            if (request.getPropertyName() != null && !request.getPropertyName().isEmpty()) {
                final String propertyName = request.getPropertyName();
                String nameSpace = "";
                String localPart = null;
                if (propertyName.indexOf(":") > 0) {
                    nameSpace = propertyName.split(":")[0];
                    localPart = propertyName.split(":")[1];
                } else {
                    if (propertyName.equalsIgnoreCase("anyText")) {
                        nameSpace = ns.getURI("csw");
                    }
                    localPart = propertyName;
                }

                Name attName = new NameImpl(ns.getURI(nameSpace), localPart);

                Name typeName = attributeTypeMap.get(attName);
                if (typeName != null) {
                    return this.store.getDomain(typeName, attName);
                }
            }

            return new CloseableIteratorAdapter<>(result.iterator());
        } catch (Exception e) {
            throw new ServiceException(
                    e, "Failed to retrieve the domain values", ServiceException.NO_APPLICABLE_CODE);
        }
    }
}
