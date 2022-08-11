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

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import net.opengis.cat.csw20.GetDomainType;
import net.opengis.cat.csw20.RequestBaseType;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.ows.v1_1.OWS;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a CloseableIterator<String> containing domain values into the specified XML Domain
 * Response
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class CSWDomainValuesTransformer extends AbstractRecordTransformer {

    static final String CSW_ROOT_LOCATION = "http://schemas.opengis.net/csw/2.0.2/";

    public CSWDomainValuesTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        super(request, canonicalSchemaLocation, null);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new CSWDomainValueTranslator(handler);
    }

    @Override
    public boolean canHandleRespose(CSWRecordsResult response) {
        return true;
    }

    class CSWDomainValueTranslator extends TranslatorSupport {

        public CSWDomainValueTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {

            try (@SuppressWarnings("unchecked")
                    CloseableIterator<String> response = (CloseableIterator<String>) o) {

                AttributesImpl attributes = new AttributesImpl();
                addAttribute(attributes, "xmlns:csw", CSW.NAMESPACE);
                addAttribute(attributes, "xmlns:dc", DC.NAMESPACE);
                addAttribute(attributes, "xmlns:dct", DCT.NAMESPACE);
                addAttribute(attributes, "xmlns:ows", OWS.NAMESPACE);
                addAttribute(attributes, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

                String locationAtt = "xsi:schemaLocation";
                StringBuilder locationDef = new StringBuilder();
                locationDef.append(CSW.NAMESPACE).append(" ");
                locationDef.append(cswSchemaLocation("CSW-discovery.xsd"));
                addAttribute(attributes, locationAtt, locationDef.toString());

                start("csw:GetDomainResponse", attributes);

                String domainValuesElement = "csw:DomainValues";
                AttributesImpl domainValuesElementAtts = new AttributesImpl();
                addAttribute(domainValuesElementAtts, "type", "csw:Record");
                start(domainValuesElement, domainValuesElementAtts);

                if (((GetDomainType) request).getParameterName() != null
                        && !((GetDomainType) request).getParameterName().isEmpty()) {
                    String parameterNameElement = "csw:ParameterName";
                    element(parameterNameElement, ((GetDomainType) request).getParameterName());
                } else if (((GetDomainType) request).getPropertyName() != null
                        && !((GetDomainType) request).getPropertyName().isEmpty()) {
                    String propertyNameElement = "csw:PropertyName";
                    element(propertyNameElement, ((GetDomainType) request).getPropertyName());
                }

                String valuesElementType = "csw:ListOfValues";
                start(valuesElementType);

                while (response.hasNext()) {
                    String value = response.next();
                    element("csw:Value", value);
                }

                end(valuesElementType);

                end(domainValuesElement);
                end("csw:GetDomainResponse");
            }
        }
    }

    public void addAttribute(AttributesImpl attributes, String name, Object value) {
        if (value != null) {
            attributes.addAttribute(
                    "",
                    name,
                    name,
                    "",
                    value instanceof String ? (String) value : String.valueOf(value));
        }
    }

    private String cswSchemaLocation(String schema) {
        if (canonicalSchemaLocation) {
            return CSW_ROOT_LOCATION + schema;
        } else {
            return buildSchemaURL(request.getBaseUrl(), "csw/2.0.2/" + schema);
        }
    }
}
