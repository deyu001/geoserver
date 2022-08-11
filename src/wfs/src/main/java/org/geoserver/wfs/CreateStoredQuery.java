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

import net.opengis.wfs20.CreateStoredQueryResponseType;
import net.opengis.wfs20.CreateStoredQueryType;
import net.opengis.wfs20.QueryExpressionTextType;
import net.opengis.wfs20.StoredQueryDescriptionType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.platform.ServiceException;

/**
 * Web Feature Service CreateStoredQuery operation.
 *
 * @author Justin Deoliveira, OpenGeo
 * @version $Id$
 */
public class CreateStoredQuery {

    /** The default create stored query language, according to spec */
    public static final String DEFAULT_LANGUAGE =
            "urn:ogc:def:queryLanguage:OGC-WFS::WFSQueryExpression";

    /** service config */
    WFSInfo wfs;

    /** stored query provider */
    StoredQueryProvider storedQueryProvider;

    public CreateStoredQuery(WFSInfo wfs, StoredQueryProvider storedQueryProvider) {
        this.wfs = wfs;
        this.storedQueryProvider = storedQueryProvider;
    }

    public CreateStoredQueryResponseType run(CreateStoredQueryType request) throws WFSException {
        for (StoredQueryDescriptionType sqd : request.getStoredQueryDefinition()) {
            if (storedQueryProvider.getStoredQuery(sqd.getId()) != null) {
                WFSException e =
                        new WFSException(
                                request,
                                "Stored query already exists",
                                WFSException.DUPLICATE_STORED_QUERY_ID_VALUE);
                e.setLocator(sqd.getId());
                throw e;
            }

            validateStoredQuery(request, sqd);

            try {
                storedQueryProvider.createStoredQuery(sqd);
            } catch (Exception e) {
                throw new WFSException(request, "Error occured creating stored query", e);
            }
        }

        Wfs20Factory factory = Wfs20Factory.eINSTANCE;
        CreateStoredQueryResponseType response = factory.createCreateStoredQueryResponseType();
        response.setStatus("OK");
        return response;
    }

    void validateStoredQuery(CreateStoredQueryType request, StoredQueryDescriptionType sq)
            throws WFSException {
        if (sq.getQueryExpressionText().isEmpty()) {
            throw new WFSException(request, "Stored query does not specify any queries");
        }

        // check for supported language
        for (QueryExpressionTextType queryExpressionTextType : sq.getQueryExpressionText()) {
            String language = queryExpressionTextType.getLanguage();
            if (language != null && !storedQueryProvider.supportsLanguage(language)) {
                WFSException e =
                        new WFSException(
                                request,
                                "Invalid language " + queryExpressionTextType.getLanguage(),
                                ServiceException.INVALID_PARAMETER_VALUE);
                e.setLocator("language");
                throw e;
            }
        }

        // check for multiple languages
        String language = sq.getQueryExpressionText().get(0).getLanguage();
        for (int i = 1; i < sq.getQueryExpressionText().size(); i++) {
            if (!language.equals(sq.getQueryExpressionText().get(i).getLanguage())) {
                throw new WFSException(
                        request,
                        "Stored query specifies queries with multiple languages. "
                                + "Not supported");
            }
        }

        try {
            storedQueryProvider.createStoredQuery(sq, false).validate();
        } catch (WFSException e) {
            throw new WFSException(request, e.getMessage(), e, e.getCode());
        } catch (Exception e) {
            throw new WFSException(request, "Error validating stored query", e);
        }
    }
}
