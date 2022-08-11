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

package org.geoserver.wps;

import java.util.Locale;
import net.opengis.ows11.CodeType;
import net.opengis.wps10.DescribeProcessType;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.transmute.ComplexTransmuter;
import org.geoserver.wps.transmute.LiteralTransmuter;
import org.geoserver.wps.transmute.Transmuter;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.type.Name;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * DescribeProcess request response transformer
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public abstract class DescribeProcessTransformer extends TransformerBase {
    protected WPSInfo wps;

    protected static final String WPS_URI = "http://www.opengis.net/wps";
    protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    public DescribeProcessTransformer(WPSInfo wps) {
        super();

        this.wps = wps;
    }

    /**
     * WPS 1.0.0 specific implementation
     *
     * @author Lucas Reed, Refractions Research Inc
     */
    public static class WPS1_0 extends DescribeProcessTransformer {
        public WPS1_0(WPSInfo wps) {
            super(wps);
        }

        public Translator createTranslator(ContentHandler handler) {
            return new DescribeProcessTranslator1_0(handler);
        }

        public class DescribeProcessTranslator1_0 extends TranslatorSupport {
            public DescribeProcessType request;

            private Locale locale;

            private DataTransformer dataTransformer;

            public DescribeProcessTranslator1_0(ContentHandler handler) {
                super(handler, null, null);
            }

            public void encode(Object object) throws IllegalArgumentException {
                this.request = (DescribeProcessType) object;

                if (null == this.request.getLanguage()) {
                    this.locale = new Locale("en-CA");
                } else {
                    this.locale = new Locale(this.request.getLanguage());
                }

                this.dataTransformer = new DataTransformer(request.getBaseUrl());

                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute(
                        "", "xmlns:xsi", "xmlns:xsi", "", DescribeProcessTransformer.XSI_URI);
                attrs.addAttribute("", "xmlns", "xmlns", "", DescribeProcessTransformer.WPS_URI);
                attrs.addAttribute(
                        "", "xmlns:wps", "xmlns:wps", "", DescribeProcessTransformer.WPS_URI);
                attrs.addAttribute("", "xmlns:ows", "xmlns:ows", "", OWS.NAMESPACE);
                attrs.addAttribute("", "version", "version", "", "1.0.0");
                attrs.addAttribute(
                        "",
                        "xsi:schemaLocation",
                        "xsi:schemaLocation",
                        "",
                        "http://www.opengis.net/wps/1.0.0 ../wpsDescribeProcess_request.xsd");

                start("wps:ProcessDescriptions", attrs);

                if (null == this.request.getIdentifier()
                        || this.request.getIdentifier().isEmpty()) {
                    throw new WPSException("Invalid identifier", "No identifier present");
                }

                for (Object identifier : this.request.getIdentifier()) {
                    CodeType ct = (CodeType) identifier;
                    this.processDescription(Ows11Util.name(ct));
                }

                end("wps:ProcessDescriptions");
            }

            private void processDescription(Name identifier) {
                if ("all".equalsIgnoreCase(identifier.getLocalPart())
                        && identifier.getNamespaceURI() == null) {
                    this.processDescriptionAll();

                    return;
                }

                ProcessFactory pf = GeoServerProcessors.createProcessFactory(identifier, false);

                if (null == pf) {
                    throw new WPSException("Invalid identifier", "InvalidParameterValue");
                }

                if (false == this.dataTransformer.isTransmutable(pf, identifier)) {
                    throw new WPSException("Invalid identifier", "InvalidParameterValue");
                }

                this.processDescription(pf, identifier);
            }

            private void processDescription(ProcessFactory pf, Name identifier) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute(
                        "",
                        "wps:processVersion",
                        "wps:processVersion",
                        "",
                        pf.getVersion(identifier));
                attributes.addAttribute(
                        "",
                        "statusSupported",
                        "statusSupported",
                        "",
                        Boolean.toString(pf.supportsProgress(identifier)));

                start("ProcessDescription", attributes);
                element("ows:Identifier", identifier.getURI());
                element("ows:Title", pf.getTitle(identifier).toString(this.locale));
                element("ows:Abstract", pf.getDescription(identifier).toString(this.locale));
                this.dataInputs(pf, identifier);
                this.processOutputs(pf, identifier);
                end("ProcessDescription");
            }

            private void processDescriptionAll() {
                for (ProcessFactory pf : GeoServerProcessors.getProcessFactories()) {
                    for (Name processName : pf.getNames()) {
                        if (false == this.dataTransformer.isTransmutable(pf, processName)) {
                            continue;
                        }
                        this.processDescription(pf, processName);
                    }
                }
            }

            private void dataInputs(ProcessFactory pf, Name processName) {
                start("DataInputs");
                for (Parameter<?> inputIdentifier : pf.getParameterInfo(processName).values()) {
                    AttributesImpl attributes = new AttributesImpl();

                    // WPS spec specifies non-negative for unlimited inputs, so -1 -> 0
                    int maxOccurs = inputIdentifier.maxOccurs;
                    if (-1 == maxOccurs) {
                        maxOccurs = Integer.MAX_VALUE;
                    }

                    attributes.addAttribute(
                            "", "minOccurs", "minOccurs", "", "" + inputIdentifier.minOccurs);
                    attributes.addAttribute("", "maxOccurs", "maxOccurs", "", "" + maxOccurs);

                    start("Input", attributes);
                    element("ows:Identifier", inputIdentifier.key);
                    element("ows:Title", inputIdentifier.title.toString(this.locale));
                    element("ows:Abstract", inputIdentifier.description.toString(this.locale));
                    Transmuter transmuter =
                            this.dataTransformer.getDefaultTransmuter(inputIdentifier.type);
                    if (transmuter instanceof ComplexTransmuter) {
                        start("ComplexData");
                        this.complexParameter((ComplexTransmuter) transmuter);
                        end("ComplexData");
                    } else {
                        this.literalData((LiteralTransmuter) transmuter);
                    }
                    end("Input");
                }
                end("DataInputs");
            }

            private void processOutputs(ProcessFactory pf, Name processName) {
                start("ProcessOutputs");
                for (Parameter<?> outputIdentifier : pf.getResultInfo(processName, null).values()) {
                    start("Output");
                    element("ows:Identifier", outputIdentifier.key);
                    element("ows:Title", outputIdentifier.title.toString(this.locale));
                    element("ows:Abstract", outputIdentifier.description.toString(this.locale));
                    Transmuter transmuter =
                            this.dataTransformer.getDefaultTransmuter(outputIdentifier.type);
                    if (transmuter instanceof ComplexTransmuter) {
                        start("ComplexOutput");
                        this.complexParameter((ComplexTransmuter) transmuter);
                        end("ComplexOutput");
                    } else {
                        this.literalData((LiteralTransmuter) transmuter);
                    }
                    end("Output");
                }
                end("ProcessOutputs");
            }

            private void literalData(LiteralTransmuter transmuter) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute(
                        "", "ows:reference", "ows:reference", "", transmuter.getEncodedType());

                start("LiteralData");
                start("ows:DataType", attributes);
                end("ows:DataType");
                end("LiteralData");
            }

            private void complexParameter(ComplexTransmuter transmuter) {
                start("Default");
                this.format(transmuter); // In future, this should select the default format
                end("Default");
                start("Supported");
                this.format(transmuter); // In future, this should iterate over all formats
                end("Supported");
            }

            private void format(ComplexTransmuter transmuter) {
                start("Format");
                element("MimeType", transmuter.getMimeType());
                element("Schema", transmuter.getSchema(this.request.getBaseUrl()));
                end("Format");
            }
        }
    }
}
