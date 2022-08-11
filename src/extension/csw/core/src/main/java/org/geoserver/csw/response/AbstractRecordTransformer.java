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
import java.util.Enumeration;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordByIdType;
import net.opengis.cat.csw20.RequestBaseType;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.geotools.util.Converters;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Encodes a FeatureCollection containing {@link CSWRecordDescriptor#RECORD} features into the
 * specified XML according to the chosen profile, brief, summary or full
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractRecordTransformer extends AbstractCSWTransformer {

    protected NamespaceSupport ns;

    public AbstractRecordTransformer(
            RequestBaseType request, boolean canonicalSchemaLocation, NamespaceSupport ns) {
        super(request, canonicalSchemaLocation);
        this.ns = ns;
    }

    /**
     * Returns true if the specified response can be handled by this transformer (it should check
     * the requested schema and the feature's type)
     */
    public abstract boolean canHandleRespose(CSWRecordsResult response);

    protected abstract class AbstractRecordTranslator extends AbstractCSWTranslator {

        public AbstractRecordTranslator(ContentHandler handler) {
            super(handler);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            final CSWRecordsResult response = (CSWRecordsResult) o;

            AttributesImpl attributes = new AttributesImpl();
            Enumeration declaredPrefixes = ns.getDeclaredPrefixes();
            while (declaredPrefixes.hasMoreElements()) {
                String prefix = (String) declaredPrefixes.nextElement();
                if (!"xml".equalsIgnoreCase(prefix)) {
                    String uri = ns.getURI(prefix);
                    addAttribute(
                            attributes,
                            StringUtils.isBlank(prefix) ? "xmlns" : "xmlns:" + prefix,
                            uri);
                }
            }
            addAttribute(attributes, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

            if (request instanceof GetRecordByIdType) {
                String locationAtt = "xsi:schemaLocation";
                StringBuilder locationDef = new StringBuilder();
                locationDef.append(CSW.NAMESPACE).append(" ");
                locationDef.append(cswSchemaLocation("CSW-discovery.xsd"));
                addAttribute(attributes, locationAtt, locationDef.toString());

                start("csw:GetRecordByIdResponse", attributes);
                encodeRecords(response);
                end("csw:GetRecordByIdResponse");
            } else {
                addAttribute(attributes, "version", "2.0.2");
                String locationAtt = "xsi:schemaLocation";
                StringBuilder locationDef = new StringBuilder();
                locationDef.append(CSW.NAMESPACE).append(" ");
                locationDef.append(cswSchemaLocation("record.xsd"));
                addAttribute(attributes, locationAtt, locationDef.toString());

                start("csw:GetRecordsResponse", attributes);

                attributes = new AttributesImpl();
                addAttribute(
                        attributes,
                        "timestamp",
                        Converters.convert(response.getTimestamp(), String.class));
                element("csw:SearchStatus", null, attributes);

                if (response.getElementSet() == null) {
                    response.setElementSet(ElementSetType.FULL);
                }

                attributes = new AttributesImpl();
                addAttribute(
                        attributes, "numberOfRecordsMatched", response.getNumberOfRecordsMatched());
                addAttribute(
                        attributes,
                        "numberOfRecordsReturned",
                        response.getNumberOfRecordsReturned());
                addAttribute(attributes, "nextRecord", response.getNextRecord());
                addAttribute(attributes, "recordSchema", response.getRecordSchema());
                addAttribute(attributes, "elementSet", response.getElementSet());
                start("csw:SearchResults", attributes);

                encodeRecords(response);

                end("csw:SearchResults");
                end("csw:GetRecordsResponse");
            }
        }

        private void encodeRecords(final CSWRecordsResult response) {
            // encode the records
            if (response.getRecords() != null) {
                try {
                    response.getRecords()
                            .accepts(
                                    new FeatureVisitor() {

                                        @Override
                                        public void visit(Feature feature) {
                                            encode(response, feature);
                                        }
                                    },
                                    new LoggingProgressListener());
                } catch (IOException e) {
                    throw new ServiceException("Failed to encoder records", e);
                }
            }
        }

        /** Encodes the feature in the desired xml format (e.g., csw:Record, ISO, ebRIM) */
        protected abstract void encode(CSWRecordsResult response, Feature f);
    }
}
