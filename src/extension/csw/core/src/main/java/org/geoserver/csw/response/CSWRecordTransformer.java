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

import java.util.Collection;
import java.util.List;
import net.opengis.cat.csw20.RequestBaseType;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.GenericRecordBuilder;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a FeatureCollection containing {@link CSWRecordDescriptor#RECORD} features into the
 * specified XML according to the chosen profile, brief, summary or full
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordTransformer extends AbstractRecordTransformer {

    static final String CSW_ROOT_LOCATION = "http://schemas.opengis.net/csw/2.0.2/";

    private static final AttributeDescriptor DC_TITLE = CSWRecordDescriptor.getDescriptor("title");

    public CSWRecordTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        super(request, canonicalSchemaLocation, CSWRecordDescriptor.NAMESPACES);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new CSWRecordTranslator(handler);
    }

    @Override
    public boolean canHandleRespose(CSWRecordsResult response) {
        return true;
    }

    class CSWRecordTranslator extends AbstractRecordTranslator {

        public CSWRecordTranslator(ContentHandler handler) {
            super(handler);
        }

        public void encode(CSWRecordsResult response, Feature f) {
            String element = "csw:" + getRecordElement(response);
            start(element);
            List<Name> elements = getElements(response);

            // encode all elements besides bbox
            if (elements != null && !element.isEmpty()) {
                // brief and summary have a specific order
                for (Name name : elements) {
                    Collection<Property> properties = f.getProperties(name);
                    if (properties != null && !properties.isEmpty()) {
                        for (Property p : properties) {
                            encodeProperty(f, p);
                        }
                    } else if (DC_TITLE.getName().equals(name)) {
                        // dc:title is mandatory even if we don't have a value for it
                        element("dc:title", null);
                    }
                }
            } else {
                // csw:Record has freeform order
                for (Property p : f.getProperties()) {
                    if (elements == null || elements.contains(p.getName())) {
                        encodeProperty(f, p);
                    }
                }
            }

            // encode the bbox if present
            if (elements == null || elements.contains(CSWRecordDescriptor.RECORD_BBOX_NAME)) {
                Property bboxes = f.getProperty(CSWRecordDescriptor.RECORD_BBOX_NAME);
                if (bboxes != null) {
                    // grab the original bounding boxes from the user data (the geometry is an
                    // aggregate)
                    @SuppressWarnings("unchecked")
                    List<ReferencedEnvelope> originalBoxes =
                            (List<ReferencedEnvelope>)
                                    bboxes.getUserData().get(GenericRecordBuilder.ORIGINAL_BBOXES);
                    for (ReferencedEnvelope re : originalBoxes) {
                        try {
                            ReferencedEnvelope wgs84re =
                                    re.transform(
                                            CRS.decode(CSWRecordDescriptor.DEFAULT_CRS_NAME), true);

                            String minx = String.valueOf(wgs84re.getMinX());
                            String miny = String.valueOf(wgs84re.getMinY());
                            String maxx = String.valueOf(wgs84re.getMaxX());
                            String maxy = String.valueOf(wgs84re.getMaxY());

                            AttributesImpl attributes = new AttributesImpl();
                            addAttribute(attributes, "crs", CSWRecordDescriptor.DEFAULT_CRS_NAME);
                            start("ows:BoundingBox", attributes);
                            element("ows:LowerCorner", minx + " " + miny);
                            element("ows:UpperCorner", maxx + " " + maxy);
                            end("ows:BoundingBox");
                        } catch (Exception e) {
                            throw new ServiceException(
                                    "Failed to encode the current record: " + f, e);
                        }
                    }
                }
            }
            end(element);
        }

        private void encodeProperty(Feature f, Property p) {
            if (p.getType() == CSWRecordDescriptor.SIMPLE_LITERAL) {
                encodeSimpleLiteral(p);
            } else if (!CSWRecordDescriptor.RECORD_BBOX_NAME.equals(p.getName())) {
                throw new IllegalArgumentException(
                        "Don't know how to encode property " + p + " in record " + f);
            }
        }

        private void encodeSimpleLiteral(Property p) {
            ComplexAttribute sl = (ComplexAttribute) p;
            String scheme =
                    sl.getProperty("scheme") == null
                            ? null
                            : (String) sl.getProperty("scheme").getValue();
            String value =
                    sl.getProperty("value") == null
                            ? ""
                            : (String) sl.getProperty("value").getValue();
            Name dn = p.getDescriptor().getName();
            String name = dn.getLocalPart();
            String prefix = CSWRecordDescriptor.NAMESPACES.getPrefix(dn.getNamespaceURI());
            if (scheme == null) {
                element(prefix + ":" + name, value);
            } else {
                AttributesImpl attributes = new AttributesImpl();
                addAttribute(attributes, "scheme", scheme);
                element(prefix + ":" + name, value, attributes);
            }
        }

        private String getRecordElement(CSWRecordsResult response) {
            switch (response.getElementSet()) {
                case BRIEF:
                    return "BriefRecord";
                case SUMMARY:
                    return "SummaryRecord";
                default:
                    return "Record";
            }
        }

        private List<Name> getElements(CSWRecordsResult response) {
            switch (response.getElementSet()) {
                case BRIEF:
                    return CSWRecordDescriptor.BRIEF_ELEMENTS;
                case SUMMARY:
                    return CSWRecordDescriptor.SUMMARY_ELEMENTS;
                default:
                    return null;
            }
        }
    }
}
