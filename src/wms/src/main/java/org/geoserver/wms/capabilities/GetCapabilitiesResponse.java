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

package org.geoserver.wms.capabilities;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.CapabilitiesTransformer;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilities;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * OWS {@link Response} bean to handle WMS {@link GetCapabilities} results.
 *
 * <p>Note since the XSLT API does not support declaring internal DTDs, and we may need to in order
 * for {@link ExtendedCapabilitiesProvider}s to contribute to the document type definition, if
 * there's any {@code ExtendedCapabilitiesProvider} that contributes to this capabilities document,
 * the plain document as created by {@link CapabilitiesTransformer} is gonna be run through an XSLT
 * transformation that will insert the proper internal DTD declaration.
 *
 * <p>Each {@link ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesRoots()} is added to the
 * list of direct children of the {@code VendorSpecificCapabilities} element, and each {@link
 * ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()} is added to the list of
 * internal DTD elements, like in the following example:
 *
 * <pre>
 * <code>
 * &lt;!DOCTYPE WMT_MS_Capabilities SYSTEM "BASE_URL/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd"[
 * &lt;!ELEMENT VendorSpecificCapabilities (TileSet*, Test?) &gt;
 * &lt;!ELEMENT Resolutions (#PCDATA) &gt;
 * &lt;!ELEMENT TestChild (#PCDATA) &gt;
 * ]&gt;
 * </code>
 * </pre>
 *
 * Where BASE_URL is the {@link GetMapRequest#getBaseUrl()}, {@code TileSet*} and {@code Test?} are
 * contributed through {@link ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesRoots()},
 * and {@code <!ELEMENT Resolutions (#PCDATA) >} and {@code <!ELEMENT TestChild (#PCDATA) >} through
 * {@link ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()}
 *
 * @author groldan
 */
public class GetCapabilitiesResponse extends BaseCapabilitiesResponse {

    private WMS wms;

    /**
     * @param wms needed for {@link WMS#getAvailableExtendedCapabilitiesProviders()} in order to
     *     check of internal DTD elements shall be added to the output document
     */
    public GetCapabilitiesResponse(final WMS wms) {
        super(GetCapabilitiesTransformer.class, GetCapabilitiesTransformer.WMS_CAPS_DEFAULT_MIME);
        this.wms = wms;
    }

    /**
     * @param value {@link GetCapabilitiesTransformer}
     * @param output destination
     * @param operation The operation identifier which resulted in <code>value</code>
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public void write(final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {

        final GetCapabilitiesTransformer transformer = (GetCapabilitiesTransformer) value;
        final GetCapabilitiesRequest request =
                (GetCapabilitiesRequest) operation.getParameters()[0];

        final String internalDTDDeclaration = getInternalDTDDeclaration(request);

        if (internalDTDDeclaration == null) {
            // transform directly to output
            try {
                transformer.transform(request, output);
            } catch (TransformerException e) {
                throw new ServiceException(e);
            }
        } else {
            // we need to add internal DTD elements, and need to use an XSL to do that,
            // since the XSLT API does not support it out of the box
            byte[] rawCapabilities;
            Transformer dtdIncludeTransformer;
            {
                ByteArrayOutputStream target = new ByteArrayOutputStream();
                try {
                    transformer.transform(request, target);
                } catch (TransformerException e) {
                    throw new ServiceException(e);
                }
                rawCapabilities = target.toByteArray();
            }

            {
                // Explicitly use SAXON's transformer factory. For some reason xalan's does not
                // work
                TransformerFactory tFactory = TransformerFactory.newInstance();
                String xsltSystemId =
                        getClass().getResource("getcaps_111_internalDTD.xsl").toExternalForm();

                Source tsource = new StreamSource(xsltSystemId);
                try {
                    dtdIncludeTransformer = tFactory.newTransformer(tsource);
                } catch (TransformerConfigurationException e) {
                    throw new ServiceException(e);
                }
            }

            // Set the full DTD declaration, including internal elements provided by
            // ExtendedCapabilitiesProviders, as an stylesheet parameter
            dtdIncludeTransformer.setParameter("DTDDeclaration", internalDTDDeclaration);

            Source source;
            try {
                /*
                 * As per GEOS-4945, we need to provide the XSL transformer a namespace aware input
                 * source that doesn't complain if the resulting DTD location is unreachable. To do
                 * so, a SAX XMLReader with an EntityResolver that resolves to the local copy of the
                 * DTD will be used.
                 */
                SAXParserFactory spf = SAXParserFactory.newInstance();
                spf.setNamespaceAware(true); // xslt _needs_ namespace aware input source
                SAXParser sp = spf.newSAXParser();
                XMLReader rawCapsReader = sp.getXMLReader();
                rawCapsReader.setEntityResolver(
                        new EntityResolver() {
                            @Override
                            public InputSource resolveEntity(String publicId, String systemId)
                                    throws SAXException {
                                final String dtdLocation =
                                        "/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd";
                                String dtdSystemId =
                                        getClass().getResource(dtdLocation).toExternalForm();
                                return new InputSource(dtdSystemId);
                            }
                        });

                source =
                        new SAXSource(
                                rawCapsReader,
                                new InputSource(new ByteArrayInputStream(rawCapabilities)));
            } catch (Exception e) {
                throw new ServiceException(e);
            }
            Result result = new StreamResult(output);
            try {
                dtdIncludeTransformer.transform(source, result);
            } catch (TransformerException e) {
                throw new ServiceException(e);
            }
        }
    }

    /**
     * Builds a full WMS 1.1.1 GetCapabilities internal DTD declaration by asking the configured
     * {@link ExtendedCapabilitiesProvider}s for the elements to contribute to the DTD.
     *
     * <p>Each {@link ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesRoots()} is added to
     * the list of direct children of the {@code VendorSpecificCapabilities} element, and each
     * {@link ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()} is added to
     * the list of internal DTD elements, like in the following example:
     *
     * <pre>
     * <code>
     * <!DOCTYPE WMT_MS_Capabilities SYSTEM "BASE_URL/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd"[
     * <!ELEMENT VendorSpecificCapabilities (TileSet*, Test?) >
     * <!ELEMENT Resolutions (#PCDATA) >
     * <!ELEMENT TestChild (#PCDATA) >
     * ]>
     * </code>
     * </pre>
     *
     * Where BASE_URL is the {@link GetMapRequest#getBaseUrl()}, {@code TileSet*} and {@code Test?}
     * are contributed through {@link
     * ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesRoots()}, and {@code <!ELEMENT
     * Resolutions (#PCDATA) >} and {@code <!ELEMENT TestChild (#PCDATA) >} through {@link
     * ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()}
     */
    private String getInternalDTDDeclaration(final GetCapabilitiesRequest request) {

        // do we need to add internal DTD declarations?
        List<ExtendedCapabilitiesProvider> providers;
        providers = wms.getAvailableExtendedCapabilitiesProviders();

        StringBuilder vendorSpecificCapsElements =
                new StringBuilder("<!ELEMENT VendorSpecificCapabilities (");
        StringBuilder internalDTDElements = new StringBuilder();

        int numRoots = 0;

        for (ExtendedCapabilitiesProvider provider : providers) {
            List<String> roots = provider.getVendorSpecificCapabilitiesRoots(request);
            if (roots != null && !roots.isEmpty()) {
                for (String vendorRoot : roots) {
                    numRoots++;
                    if (numRoots > 1) {
                        vendorSpecificCapsElements.append(", ");
                    }
                    vendorSpecificCapsElements.append(vendorRoot);
                }
                List<String> childDecls = provider.getVendorSpecificCapabilitiesChildDecls(request);
                for (String internalElement : childDecls) {
                    internalDTDElements.append(internalElement);
                    internalDTDElements.append('\n');
                }
            }
        }
        vendorSpecificCapsElements.append(") >\n");

        String fullDTDDeclaration = null;

        if (numRoots > 0) {
            final String baseURL = request.getBaseUrl();
            String dtdUrl = buildSchemaURL(baseURL, "wms/1.1.1/WMS_MS_Capabilities.dtd");

            StringBuilder builder = new StringBuilder("<!DOCTYPE WMT_MS_Capabilities SYSTEM \"");
            builder.append(dtdUrl).append("\"[\n");
            builder.append(vendorSpecificCapsElements);
            builder.append(internalDTDElements);
            builder.append("]>\n");
            fullDTDDeclaration = builder.toString();
        }

        return fullDTDDeclaration;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        // check that we have a valid value (same check as the super method)
        if (value == null || !value.getClass().isAssignableFrom(super.getBinding())) {
            // this is not good (same error message as the super method)
            String message =
                    String.format(
                            "%s/%s",
                            value == null ? "null" : value.getClass().getName(), operation.getId());
            throw new IllegalArgumentException(message);
        }
        // search for the get capabilities object
        GetCapabilitiesRequest request = null;
        for (Object parameter : operation.getParameters()) {
            if (parameter instanceof GetCapabilitiesRequest) {
                // we found our request
                request = (GetCapabilitiesRequest) parameter;
            }
        }
        if (request == null) {
            // unlikely but no get capabilities request was found, fall back to the default behavior
            return super.getMimeType(value, operation);
        }
        // let's see if we have the format parameter
        String format = request.getRawKvp().get("FORMAT");
        if (format == null || format.isEmpty()) {
            // no format parameter, fall back to default behavior
            return super.getMimeType(value, operation);
        }
        // if the requested format is a valid mime type return it
        for (String mimeType : GetCapabilitiesTransformer.WMS_CAPS_AVAIL_MIME) {
            if (format.equalsIgnoreCase(mimeType)) {
                // the format parameter value maps to a valid mime type, returning the associate
                // mime type
                return mimeType;
            }
        }
        // the requested format is not supported, throw an exception
        throw new RuntimeException(
                String.format("The request format '%s' is not supported.", format));
    }
}
