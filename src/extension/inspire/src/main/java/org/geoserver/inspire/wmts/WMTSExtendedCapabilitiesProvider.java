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

package org.geoserver.inspire.wmts;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;

import java.io.IOException;
import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.inspire.ViewServicesUtils;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.io.XMLBuilder;
import org.geowebcache.service.wmts.WMTSExtensionImpl;
import org.xml.sax.Attributes;

public class WMTSExtendedCapabilitiesProvider extends WMTSExtensionImpl {

    public static final String VS_VS_OWS_NAMESPACE =
            "http://inspire.ec.europa.eu/schemas/inspire_vs_ows11/1.0";
    public static final String VS_VS_OWS_SCHEMA =
            "http://inspire.ec.europa.eu/schemas/inspire_vs_ows11/1.0/inspire_vs_ows_11.xsd";

    private final GeoServer geoserver;

    public WMTSExtendedCapabilitiesProvider(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    @Override
    public String[] getSchemaLocations() {
        return new String[] {VS_VS_OWS_NAMESPACE, VS_VS_OWS_SCHEMA};
    }

    @Override
    public void registerNamespaces(XMLBuilder xml) throws IOException {
        xml.attribute("xmlns:inspire_vs", VS_VS_OWS_NAMESPACE);
        xml.attribute("xmlns:inspire_common", COMMON_NAMESPACE);
    }

    @Override
    public void encodedOperationsMetadata(XMLBuilder xml) throws IOException {

        // create custom translator
        ExtendedCapabilitiesProvider.Translator translator =
                new org.geoserver.ExtendedCapabilitiesProvider.Translator() {

                    @Override
                    public void start(String element) {
                        try {
                            xml.indentElement(element);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void start(String element, Attributes attributes) {
                        try {
                            xml.indentElement(element);
                            for (int i = 0; i < attributes.getLength(); i++) {
                                xml.attribute(attributes.getQName(i), attributes.getValue(i));
                            }
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void chars(String text) {
                        try {
                            xml.text(text);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void end(String element) {
                        try {
                            xml.endElement(element);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                };

        // write inspire scenario 1 metadata
        MetadataMap serviceMetadata = geoserver.getService(WMTSInfo.class).getMetadata();
        Boolean createExtendedCapabilities =
                serviceMetadata.get(CREATE_EXTENDED_CAPABILITIES.key, Boolean.class);
        String metadataURL = (String) serviceMetadata.get(SERVICE_METADATA_URL.key);
        if (metadataURL == null
                || createExtendedCapabilities != null && !createExtendedCapabilities) {
            return;
        }
        String mediaType = (String) serviceMetadata.get(SERVICE_METADATA_TYPE.key);
        String language = (String) serviceMetadata.get(LANGUAGE.key);
        ViewServicesUtils.addScenario1Elements(translator, metadataURL, mediaType, language);
    }

    @Override
    public ServiceInformation getServiceInformation() {
        return new ServiceInformation();
    }
}
