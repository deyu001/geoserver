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

package org.geoserver.csw.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import javax.xml.transform.TransformerException;
import net.opengis.cat.csw20.GetRecordByIdType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.RequestBaseType;
import net.opengis.cat.csw20.ResultType;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.opengis.feature.type.FeatureType;

/**
 * Base class for XML based CSW record responses
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractRecordsResponse extends Response {

    String schema;

    GeoServer gs;

    FeatureType recordType;

    public AbstractRecordsResponse(FeatureType recordType, String schema, GeoServer gs) {
        this(recordType, schema, Collections.singleton("application/xml"), gs);
    }

    public AbstractRecordsResponse(
            FeatureType recordType, String schema, Set<String> outputFormats, GeoServer gs) {
        super(CSWRecordsResult.class, outputFormats);
        this.schema = schema;
        this.gs = gs;
        this.recordType = recordType;
    }

    @Override
    public boolean canHandle(Operation operation) {
        String requestedSchema = getRequestedSchema(operation);
        if (requestedSchema == null) {
            requestedSchema = CSW.NAMESPACE;
        }
        return requestedSchema.equals(schema);
    }

    private String getRequestedSchema(Operation operation) {
        Object request = operation.getParameters()[0];
        if (request instanceof GetRecordByIdType) {
            GetRecordByIdType gr = (GetRecordByIdType) request;
            return gr.getOutputSchema();
        } else if (request instanceof GetRecordsType) {
            GetRecordsType gr = (GetRecordsType) request;
            return gr.getOutputSchema();
        } else {
            throw new IllegalArgumentException("Unsupported request object type: " + request);
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        CSWRecordsResult result = (CSWRecordsResult) value;
        RequestBaseType request = (RequestBaseType) operation.getParameters()[0];
        CSWInfo csw = gs.getService(CSWInfo.class);

        // check the output schema is valid
        if (result.getRecords() != null) {
            FeatureType recordSchema = result.getRecords().getSchema();
            if (recordSchema != null && !recordType.equals(recordSchema)) {
                throw new IllegalArgumentException(
                        "Cannot encode this kind of record "
                                + recordSchema.getName()
                                + " into schema "
                                + schema);
            }
        }

        if (getResultType(request) == ResultType.VALIDATE) {
            // this one is output schema independent
            transformAcknowledgement(output, request, csw);
        } else {
            transformResponse(output, result, request, csw);
        }
    }

    private ResultType getResultType(RequestBaseType request) {
        if (request instanceof GetRecordsType) {
            return ((GetRecordsType) request).getResultType();
        } else {
            return ResultType.RESULTS;
        }
    }

    private void transformAcknowledgement(
            OutputStream output, RequestBaseType request, CSWInfo csw) {
        AcknowledgementTransformer transformer =
                new AcknowledgementTransformer(request, csw.isCanonicalSchemaLocation());
        transformer.setIndentation(2);
        try {
            transformer.transform(null, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }

    /** Actually encodes the response into a set of records */
    protected abstract void transformResponse(
            OutputStream output, CSWRecordsResult result, RequestBaseType request, CSWInfo csw);
}
