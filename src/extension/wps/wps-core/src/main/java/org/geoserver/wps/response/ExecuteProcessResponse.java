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

package org.geoserver.wps.response;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.OutputDataType;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.Response;
import org.geoserver.ows.XmlObjectEncodingResponse;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.BinaryEncoderDelegate;
import org.geoserver.wps.CDataEncoderDelegate;
import org.geoserver.wps.Execute;
import org.geoserver.wps.RawDataEncoderDelegate;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.XMLEncoderDelegate;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.ows.v1_1.OWS;
import org.geotools.ows.v1_1.OWSConfiguration;
import org.geotools.process.ProcessFactory;
import org.geotools.xsd.Encoder;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationContext;

/**
 * Encodes the Execute response either in the normal XML format or in the raw format
 *
 * @author Andrea Aime
 */
public class ExecuteProcessResponse extends Response {

    XmlObjectEncodingResponse standardResponse;

    ApplicationContext ctx;

    public ExecuteProcessResponse(Class binding, String elementName, Class xmlConfiguration) {
        super(ExecuteResponseType.class);
        this.standardResponse =
                new XmlObjectEncodingResponse(binding, elementName, xmlConfiguration);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (isStandardDocumentResponse(operation)) {
            // normal execute response encoding
            return standardResponse.getMimeType(value, operation);
        } else {
            // raw response, let's see what the output is
            ExecuteResponseType response = (ExecuteResponseType) value;
            if (response.getProcessOutputs() == null) {
                // just a status report or a failure report
                return "text/xml";
            }
            OutputDataType result =
                    (OutputDataType) response.getProcessOutputs().getOutput().get(0);
            LiteralDataType literal = result.getData().getLiteralData();
            ComplexDataType complex = result.getData().getComplexData();
            if (literal != null) {
                // literals are encoded as plain strings
                return "text/plain";
            } else if (complex != null) {
                // Execute should have properly setup the mime type
                return complex.getMimeType();
            } else {
                // bbox
                return "text/xml";
            }
        }
    }

    private boolean isStandardDocumentResponse(Operation operation) {
        if (operation.getParameters()[0] instanceof ExecuteType) {
            ExecuteType execute = (ExecuteType) operation.getParameters()[0];
            return execute.getResponseForm() == null
                    || execute.getResponseForm().getRawDataOutput() == null;
        }
        return true;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        String mimeType = getMimeType(value, operation);
        String disposition = DISPOSITION_INLINE;
        if (mimeType != null) {
            // if there is a BinaryEncoder we could expose a method that allows
            // expressing preferred type as opposed to this ugliness
            if (mimeType.indexOf("image") == 0) {
                // tiff for download
                if (mimeType.indexOf("tiff") > 0) {
                    disposition = DISPOSITION_ATTACH;
                }
            } else if (mimeType.equals("application/zip")) {
                disposition = DISPOSITION_ATTACH;
            } else if (mimeType.equals("application/arcgrid")) {
                disposition = DISPOSITION_ATTACH;
            }
        }
        return disposition;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        if (isStandardDocumentResponse(operation)) {
            return "execute.xml";
        } else {
            ExecuteResponseType response = (ExecuteResponseType) value;
            if (response.getProcessOutputs() == null) {
                // just a status report or a failure report
                return "execute.xml";
            }
            OutputDataType result =
                    (OutputDataType) response.getProcessOutputs().getOutput().get(0);
            String fname = result.getIdentifier().getValue();
            LiteralDataType literal = result.getData().getLiteralData();
            ComplexDataType complex = result.getData().getComplexData();
            String fext = null;
            // if it's a literal, use text, otherwise get the complex ppio and ask for the extension
            if (literal != null) {
                fext = "txt";
            } else if (complex != null) {
                Name name = Ows11Util.name(response.getProcess().getIdentifier());
                ProcessFactory factory = GeoServerProcessors.createProcessFactory(name, true);
                if (factory != null) {
                    Map<String, Parameter<?>> resultInfo = factory.getResultInfo(name, null);
                    Parameter p = resultInfo.get(result.getIdentifier().getValue());
                    if (p != null) {
                        ProcessParameterIO ppio =
                                ProcessParameterIO.find(p, ctx, complex.getMimeType());
                        if (ppio instanceof ComplexPPIO) {
                            fext =
                                    ((ComplexPPIO) ppio)
                                            .getFileExtension(
                                                    result.getData()
                                                            .getComplexData()
                                                            .getData()
                                                            .get(0));
                        }
                    }
                }
            }

            // fallback
            if (fext == null) {
                fext = "bin";
            }
            return fname + "." + fext;
        }
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        ExecuteResponseType response = (ExecuteResponseType) value;

        // From the spec:
        // In the most primitive case, when a response form of ―RawDataOutput‖ is requested,
        // process execution is successful, and only one complex output is produced, then the
        // Execute operation response will consist simply of that one complex output in its raw form
        // returned directly to the client.
        // In all other cases, the response to a valid Execute operation request is an
        // ExecuteResponse XML document
        if (isStandardDocumentResponse(operation)
                || response.getStatus().getProcessSucceeded() == null) {
            // normal execute response encoding
            standardResponse.write(value, output, operation);
        } else {
            // raw response, let's see what the output is
            OutputDataType result =
                    (OutputDataType) response.getProcessOutputs().getOutput().get(0);
            LiteralDataType literal = result.getData().getLiteralData();
            BoundingBoxType bbox = result.getData().getBoundingBoxData();
            if (literal != null) {
                writeLiteral(output, literal);
            } else if (bbox != null) {
                writeBBox(output, bbox);
            } else {
                writeComplex(output, result);
            }
        }
    }

    private void writeBBox(OutputStream os, BoundingBoxType bbox) throws IOException {
        Encoder encoder = new Encoder(new OWSConfiguration());
        encoder.encode(bbox, OWS.BoundingBox, os);
    }

    /**
     * Write out complex data assuming {@link Execute} has set up the proper encoder as the output
     */
    void writeComplex(OutputStream output, OutputDataType result) throws IOException {
        Object rawResult = result.getData().getComplexData().getData().get(0);
        if (rawResult instanceof RawDataEncoderDelegate) {
            RawDataEncoderDelegate delegate = (RawDataEncoderDelegate) rawResult;
            delegate.encode(output);
        } else if (rawResult instanceof XMLEncoderDelegate) {
            XMLEncoderDelegate delegate = (XMLEncoderDelegate) rawResult;

            try {
                TransformerHandler xmls =
                        ((SAXTransformerFactory) SAXTransformerFactory.newInstance())
                                .newTransformerHandler();
                xmls.setResult(new StreamResult(output));
                delegate.encode(xmls);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new WPSException(
                        "An error occurred while encoding " + "the results of the process", e);
            }
        } else if (rawResult instanceof CDataEncoderDelegate) {
            try {
                ((CDataEncoderDelegate) rawResult).encode(output);
            } catch (Exception e) {
                throw new WPSException(
                        "An error occurred while encoding " + "the results of the process", e);
            }
        } else if (rawResult instanceof BinaryEncoderDelegate) {
            try {
                ((BinaryEncoderDelegate) rawResult).encode(output);
            } catch (Exception e) {
                throw new WPSException(
                        "An error occurred while encoding " + "the results of the process", e);
            }
        } else {
            throw new WPSException(
                    "Cannot encode an object of class " + rawResult.getClass() + " in raw form");
        }
    }

    /** Write out literal results by converting them to strings */
    void writeLiteral(OutputStream output, LiteralDataType literal) {
        PrintWriter writer = new PrintWriter(output);
        writer.write(literal.getValue());
        writer.flush();
    }
}
