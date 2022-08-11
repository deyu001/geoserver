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

package org.geoserver.params.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class EchoParametersDao {

    private static final Logger LOGGER = Logging.getLogger(EchoParametersDao.class);
    private static SecureXStream xStream;

    static {
        xStream = new SecureXStream();
        xStream.registerConverter(new EchoParameterConverter());
        xStream.alias("EchoParameter", EchoParameter.class);
        xStream.alias("EchoParameters", EchoParametersDao.EchoParametersList.class);
        xStream.addImplicitCollection(EchoParametersDao.EchoParametersList.class, "parameters");
        xStream.allowTypes(
                new Class[] {EchoParameter.class, EchoParametersDao.EchoParametersList.class});
    }

    public static String getEchoParametersPath() {
        return "params-extractor/echo-parameters.xml";
    }

    public static String getTmpEchoParametersPath() {
        return String.format("params-extractor/%s-echo-parameters.xml", UUID.randomUUID());
    }

    public static List<EchoParameter> getEchoParameters() {
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        return getEchoParameters(echoParameters.in());
    }

    private static GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");
    }

    @SuppressWarnings("PMD.UseTryWithResources") // cannot use try-with-resources here
    public static List<EchoParameter> getEchoParameters(InputStream inputStream) {
        try {
            if (inputStream.available() == 0) {
                Utils.debug(LOGGER, "Echo parameters file seems to be empty.");
                return new ArrayList<>();
            }
            EchoParametersList list = (EchoParametersList) xStream.fromXML(inputStream);
            return list.parameters == null ? new ArrayList<>() : list.parameters;
        } catch (Exception exception) {
            throw Utils.exception(exception, "Error parsing echo parameters files.");
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

    public static void saveOrUpdateEchoParameter(EchoParameter echoParameter) {
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        Resource tmpEchoParameters = getDataDirectory().get(getTmpEchoParametersPath());
        saveOrUpdateEchoParameter(echoParameter, echoParameters, tmpEchoParameters);
        echoParameters.delete();
        tmpEchoParameters.renameTo(echoParameters);
    }

    public static void saveOrUpdateEchoParameter(
            EchoParameter echoParameter, Resource input, Resource output) {
        try (InputStream inputStream = input.in();
                OutputStream outputStream = output.out()) {
            List<EchoParameter> echoParameters = getEchoParameters(inputStream);
            boolean exists = false;
            for (int i = 0; i < echoParameters.size() && !exists; i++) {
                if (echoParameters.get(i).getId().equals(echoParameter.getId())) {
                    echoParameters.set(i, echoParameter);
                    exists = true;
                }
            }
            if (!exists) {
                echoParameters.add(echoParameter);
            }
            writeEchoParameters(echoParameters, outputStream);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, "Failed to cleanly close resources", e);
        }
    }

    public static void deleteEchoParameters(String... echoParametersIds) {
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        Resource tmpEchoParameters = getDataDirectory().get(getTmpEchoParametersPath());
        try (InputStream in = echoParameters.in();
                OutputStream os = tmpEchoParameters.out()) {
            deleteEchoParameters(in, os, echoParametersIds);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, "Failed to cleanly close param files", e);
        }
        echoParameters.delete();
        tmpEchoParameters.renameTo(echoParameters);
    }

    public static void deleteEchoParameters(
            InputStream inputStream, OutputStream outputStream, String... forwardParameterIds) {
        writeEchoParameters(
                getEchoParameters(inputStream)
                        .stream()
                        .filter(
                                forwardParameter ->
                                        !Arrays.stream(forwardParameterIds)
                                                .anyMatch(
                                                        forwardParameterId ->
                                                                forwardParameterId.equals(
                                                                        forwardParameter.getId())))
                        .collect(Collectors.toList()),
                outputStream);
    }

    private static void writeEchoParameters(
            List<EchoParameter> echoParameters, OutputStream outputStream) {
        try {
            xStream.toXML(new EchoParametersList(echoParameters), outputStream);
        } catch (Exception exception) {
            throw Utils.exception(exception, "Something bad happen when writing echo parameters.");
        }
    }

    private static final class EchoParameterHandler extends DefaultHandler {

        final List<EchoParameter> echoParameters = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (!qName.equalsIgnoreCase("EchoParameter")) {
                return;
            }
            Utils.debug(LOGGER, "Start parsing echo parameter.");
            EchoParameterBuilder echoParameterBuilder = new EchoParameterBuilder();
            getAttribute("id", attributes, echoParameterBuilder::withId);
            getAttribute("parameter", attributes, echoParameterBuilder::withParameter);
            getAttribute(
                    "activated",
                    attributes,
                    compose(Boolean::valueOf, echoParameterBuilder::withActivated));
            echoParameters.add(echoParameterBuilder.build());
            Utils.debug(LOGGER, "End parsing echo parameter.");
        }

        private static <T> Consumer<String> compose(
                Function<String, T> convert, Consumer<T> setter) {
            return (value) -> setter.accept(convert.apply(value));
        }

        private void getAttribute(
                String attributeName, Attributes attributes, Consumer<String> setter) {
            String attributeValue = attributes.getValue(attributeName);
            if (attributeValue == null) {
                Utils.debug(LOGGER, "Echo parameter attribute %s is NULL.", attributeName);
                return;
            }
            Utils.debug(
                    LOGGER, "Echo parameter attribute %s is %s.", attributeName, attributeValue);
            try {
                setter.accept(attributeValue);
            } catch (Exception exception) {
                throw Utils.exception(
                        exception,
                        "Error setting attribute '%s' with value '%s'.",
                        attributeName,
                        attributeValue);
            }
        }
    }

    /** Support class for XStream serialization */
    static final class EchoParametersList {
        List<EchoParameter> parameters;

        public EchoParametersList(List<EchoParameter> rules) {
            this.parameters = rules;
        }
    }
}
