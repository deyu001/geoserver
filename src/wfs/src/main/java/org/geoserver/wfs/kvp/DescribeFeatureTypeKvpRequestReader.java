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

package org.geoserver.wfs.kvp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.ecore.EFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.xml.sax.helpers.NamespaceSupport;

public class DescribeFeatureTypeKvpRequestReader extends WFSKvpRequestReader {

    private final Catalog catalog;

    public DescribeFeatureTypeKvpRequestReader(final Catalog catalog) {
        super(DescribeFeatureTypeType.class, WfsFactory.eINSTANCE);
        this.catalog = catalog;
    }

    public DescribeFeatureTypeKvpRequestReader(
            final Catalog catalog, Class requestBean, EFactory factory) {
        super(requestBean, factory);
        this.catalog = catalog;
    }

    @SuppressWarnings("unchecked")
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // let super do its thing
        request = super.read(request, kvp, rawKvp);

        // do an additional check for outputFormat, because the default
        // in wfs 1.1 is not the default for wfs 1.0
        DescribeFeatureTypeRequest req = DescribeFeatureTypeRequest.adapt(request);

        if (!req.isSetOutputFormat()) {
            switch (WFSInfo.Version.negotiate(req.getVersion())) {
                case V_10:
                    req.setOutputFormat("XMLSCHEMA");
                    break;
                case V_11:
                    req.setOutputFormat("text/xml; subtype=gml/3.1.1");
                    break;
                case V_20:
                default:
                    req.setOutputFormat("application/gml+xml; version=3.2");
            }
        }

        // handle the name differences in property names between 1.1 and 2.0
        // The specification here is inconsistent, the KVP param table says "TYPENAME",
        // but an explanation just below states KVP should be using TYPENAMES and CITE users the
        // latter
        // So let's support both...
        if (req instanceof DescribeFeatureTypeRequest.WFS20 && kvp.containsKey("typenames")) {
            List<List<QName>> typenames = (List<List<QName>>) kvp.get("typenames");
            req.getTypeNames().clear();
            req.getTypeNames().addAll(typenames.get(0));
        }

        // did the user supply alternate namespace prefixes?
        NamespaceSupport namespaces = null;
        if (kvp.containsKey("NAMESPACE") || kvp.containsKey("NAMESPACES")) {
            if (kvp.get("NAMESPACE") instanceof NamespaceSupport) {
                namespaces = (NamespaceSupport) kvp.get("namespace");
            } else if (kvp.get("NAMESPACES") instanceof NamespaceSupport) {
                namespaces = (NamespaceSupport) kvp.get("namespaces");
            } else {
                LOGGER.warning(
                        "There's a namespace parameter but it seems it wasn't parsed to a "
                                + NamespaceSupport.class.getName()
                                + ": "
                                + kvp.get("namespace"));
            }
        }
        if (namespaces != null) {
            List<QName> typeNames = req.getTypeNames();
            List<QName> newList = new ArrayList<>(typeNames.size());
            for (QName name : typeNames) {
                String localPart = name.getLocalPart();
                String prefix = name.getPrefix();
                String namespaceURI = name.getNamespaceURI();
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                    // no prefix specified, did the request specify a default namespace?
                    namespaceURI = namespaces.getURI(XMLConstants.DEFAULT_NS_PREFIX);
                } else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                    // prefix specified, does a namespace mapping were declared for it?
                    if (namespaces.getURI(prefix) != null) {
                        namespaceURI = namespaces.getURI(prefix);
                    }
                }
                if (catalog.getNamespaceByURI(namespaceURI) != null) {
                    prefix = catalog.getNamespaceByURI(namespaceURI).getPrefix();
                }
                newList.add(new QName(namespaceURI, localPart, prefix));
            }
            req.setTypeNames(newList);
        }
        return request;
    }
}
