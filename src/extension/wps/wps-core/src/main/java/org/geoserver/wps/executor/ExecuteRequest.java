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

package org.geoserver.wps.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.opengis.wps10.DocumentOutputDefinitionType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseDocumentType;
import net.opengis.wps10.ResponseFormType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.Ows11Util;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.AbstractRawData;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.validator.ProcessLimitsFilter;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;
import org.springframework.validation.Validator;

/**
 * Centralizes some common request parsing activities
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ExecuteRequest {

    private final Name processName;
    private final ProcessFactory pf;
    private ExecuteType request;

    private LazyInputMap inputs;

    public ExecuteRequest(ExecuteType request) {
        this.request = request;

        processName = Ows11Util.name(request.getIdentifier());
        pf = GeoServerProcessors.createProcessFactory(processName, true);
        if (pf == null) {
            throw new WPSException("Unknown process " + processName);
        }
    }

    /** The wrapped WPS 1.0 request */
    public ExecuteType getRequest() {
        return request;
    }

    /** True if the request is asynchronous */
    public boolean isAsynchronous() {
        return request.getResponseForm() != null
                && request.getResponseForm().getResponseDocument() != null
                && request.getResponseForm().getResponseDocument().isStoreExecuteResponse();
    }

    /** Returns true if status update is requested */
    public boolean isStatusEnabled() {
        return isAsynchronous() && request.getResponseForm().getResponseDocument().isStatus();
    }

    /** Returns the process name according to the GeoTools API */
    public Name getProcessName() {
        return Ows11Util.name(request.getIdentifier());
    }

    /** Returns the process inputs according to the GeoTools API expectations */
    public LazyInputMap getProcessInputs(WPSExecutionManager manager) {
        if (inputs == null) {
            inputs = getInputsInternal(manager);
        }
        return inputs;
    }

    LazyInputMap getInputsInternal(WPSExecutionManager manager) {
        // get the input descriptors
        final Map<String, Parameter<?>> parameters = pf.getParameterInfo(processName);
        Map<String, InputProvider> providers = new LinkedHashMap<>();

        // see what output raw data we have that need the user chosen mime type to be
        // sent back to the process as an input
        Map<String, String> outputMimeParameters =
                AbstractRawData.getOutputMimeParameters(processName, pf);
        if (!outputMimeParameters.isEmpty()) {
            Map<String, String> requestedRawDataMimeTypes =
                    getRequestedRawDataMimeTypes(outputMimeParameters.keySet(), processName, pf);
            for (Map.Entry<String, String> param : outputMimeParameters.entrySet()) {
                String outputName = param.getKey();
                String inputParameter = param.getValue();
                String mime = requestedRawDataMimeTypes.get(outputName);
                StringInputProvider provider = new StringInputProvider(mime, inputParameter);
                providers.put(inputParameter, provider);
            }
        }

        // turn them into a map of input providers
        for (Object o : request.getDataInputs().getInput()) {
            InputType input = (InputType) o;
            String inputId = input.getIdentifier().getValue();

            // locate the parameter for this request
            Parameter p = parameters.get(inputId);
            if (p == null) {
                throw new WPSException("No such parameter: " + inputId);
            }

            // find the ppio
            String mime = null;
            if (input.getData() != null && input.getData().getComplexData() != null) {
                mime = input.getData().getComplexData().getMimeType();
            } else if (input.getReference() != null) {
                mime = input.getReference().getMimeType();
            }
            ProcessParameterIO ppio = ProcessParameterIO.find(p, manager.applicationContext, mime);
            if (ppio == null) {
                throw new WPSException("Unable to decode input: " + inputId);
            }

            // get the validators
            @SuppressWarnings("unchecked")
            Collection<Validator> validators =
                    (Collection<Validator>) p.metadata.get(ProcessLimitsFilter.VALIDATORS_KEY);
            // we handle multiplicity validation here, before the parsing even starts

            // build the provider
            try {
                InputProvider provider =
                        AbstractInputProvider.getInputProvider(
                                input, ppio, manager, manager.applicationContext, validators);

                // store the input
                if (p.maxOccurs > 1) {
                    ListInputProvider lp = (ListInputProvider) providers.get(p.key);
                    if (lp == null) {
                        lp = new ListInputProvider(provider, p.getMaxOccurs());
                        providers.put(p.key, lp);
                    } else {
                        lp.add(provider);
                    }
                } else {
                    providers.put(p.key, provider);
                }
            } catch (Exception e) {
                throw new WPSException("Failed to parse process inputs", e);
            }
        }

        return new LazyInputMap(providers);
    }

    private Map<String, String> getRequestedRawDataMimeTypes(
            Collection<String> rawResults, Name name, ProcessFactory pf) {
        Map<String, String> result = new HashMap<>();
        ResponseFormType form = request.getResponseForm();
        OutputDefinitionType raw = form.getRawDataOutput();
        ResponseDocumentType document = form.getResponseDocument();
        if (form == null || (raw == null && document == null)) {
            // all outputs using their default mime
            for (String rawResult : rawResults) {
                String mime = AbstractRawData.getDefaultMime(name, pf, rawResult);
                result.put(rawResult, mime);
            }
        } else if (raw != null) {
            // just one output type
            String output = raw.getIdentifier().getValue();
            String mime;
            if (raw.getMimeType() != null) {
                mime = raw.getMimeType();
            } else {
                mime = AbstractRawData.getDefaultMime(name, pf, output);
            }
            result.put(output, mime);
        } else {
            // the response document form
            for (Object o : document.getOutput()) {
                OutputDefinitionType out = (OutputDefinitionType) o;
                String outputName = out.getIdentifier().getValue();
                if (rawResults.contains(outputName)) {
                    // was the output mime specified?
                    String mime = out.getMimeType();
                    if (mime == null || mime.trim().isEmpty()) {
                        mime = AbstractRawData.getDefaultMime(name, pf, outputName);
                    }
                    result.put(outputName, mime);
                }
            }
        }

        return result;
    }

    public boolean isLineageRequested() {
        return request.getResponseForm() != null
                && request.getResponseForm().getResponseDocument() != null
                && request.getResponseForm().getResponseDocument().isLineage();
    }

    /** Returns null if nothing specific was requested, the list otherwise */
    public List<OutputDefinitionType> getRequestedOutputs() {
        // in case nothing specific was requested
        ResponseFormType responseForm = request.getResponseForm();
        if (responseForm == null) {
            return null;
        }

        if (responseForm.getRawDataOutput() != null) {
            return Collections.singletonList(responseForm.getRawDataOutput());
        } else if (responseForm.getResponseDocument() != null
                && responseForm.getResponseDocument().getOutput() != null) {
            List<OutputDefinitionType> result = new ArrayList<>();
            EList outputs = responseForm.getResponseDocument().getOutput();
            for (Object output : outputs) {
                result.add((DocumentOutputDefinitionType) output);
            }

            return result;
        }

        return null;
    }

    /** Ensures the requested output are valid */
    public void validateOutputs(Map<String, Object> inputs) {
        Map<String, Parameter<?>> resultInfo = pf.getResultInfo(getProcessName(), inputs);

        List<OutputDefinitionType> requestedOutputs = getRequestedOutputs();
        if (requestedOutputs != null) {
            for (OutputDefinitionType output : requestedOutputs) {
                String outputIdentifier = output.getIdentifier().getValue();
                if (!resultInfo.containsKey(outputIdentifier)) {
                    String locator =
                            output instanceof DocumentOutputDefinitionType
                                    ? "ResponseDocument"
                                    : "RawDataOutput";
                    throw new WPSException(
                            "Unknow output " + outputIdentifier,
                            ServiceException.INVALID_PARAMETER_VALUE,
                            locator);
                }
            }
        }
    }
}
