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

package org.geoserver.ows.kvp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import javax.xml.XMLConstants;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Parses a list of namespace declarations of the form {@code
 * <xmlns(foo=http://name.space1)[,xmlns(bar=http://name.space2)]+> } into a {@link
 * NamespaceSupport}. Using the {@link PrefixNamespaceSeparator#COMMA} it's also possible to handle
 * the WFS 2.0 suggested syntax, {@code
 * <xmlns(foo,http://name.space1)[,xmlns(bar,http://name.space2)]+> }
 *
 * @author groldan
 */
public class NamespaceKvpParser extends KvpParser {

    private final boolean useComma;

    public NamespaceKvpParser(String key) {
        this(key, false);
    }

    public NamespaceKvpParser(String key, boolean useComma) {
        super(key, NamespaceSupport.class);
        this.useComma = useComma;
    }

    /**
     * @param value a list of namespace declarations of the form {@code
     *     <xmlns(foo=http://name.space1)[,xmlns(bar=http://name.space2)]+> }
     */
    @SuppressWarnings("unchecked")
    @Override
    public NamespaceSupport parse(final String value) throws Exception {
        List<String> declarations;
        if (useComma) {
            String[] parts = value.split(",(?![^()]*+\\))");
            declarations = Arrays.asList(parts);
        } else {
            declarations = (List<String>) new FlatKvpParser("", String.class).parse(value);
        }
        NamespaceSupport ctx = new NamespaceSupport();

        String[] parts;
        String prefix;
        String uri;
        for (String decl : declarations) {
            decl = decl.trim();
            if (!decl.startsWith("xmlns(") || !decl.endsWith(")")) {
                throw new ServiceException(
                        "Illegal namespace declaration, "
                                + "should be of the form xmlns(<prefix>=<ns uri>): "
                                + decl,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        getKey());
            }
            decl = decl.substring("xmlns(".length());
            decl = decl.substring(0, decl.length() - 1);
            String separator = useComma ? "," : "=";
            parts = decl.split(separator);
            if (parts.length == 1) {
                prefix = XMLConstants.DEFAULT_NS_PREFIX;
                uri = parts[0];
            } else if (parts.length == 2) {
                prefix = parts[0];
                uri = parts[1];
            } else {
                throw new ServiceException(
                        "Illegal namespace declaration, "
                                + "should be of the form prefix"
                                + separator
                                + "<namespace uri>: "
                                + decl,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        getKey());
            }

            try {
                new URI(uri);
            } catch (URISyntaxException e) {
                throw new ServiceException(
                        "Illegal namespace declaration: " + decl,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        getKey());
            }
            ctx.declarePrefix(prefix, uri);
        }

        return ctx;
    }
}
