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

package org.geoserver.wfs;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.wfs.request.GetCapabilitiesRequest;

/**
 * Web Feature Service GetCapabilities operation.
 *
 * <p>This operation returns a {@link org.geotools.xml.transform.TransformerBase} instance which
 * will serialize the wfs capabilities document. This class uses ows version negotiation to
 * determine which version of the wfs capabilities document to return.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GetCapabilities {
    /** WFS service configuration */
    WFSInfo wfs;

    /** The catalog */
    Catalog catalog;

    private final Collection<WFSExtendedCapabilitiesProvider> extendedCapabilitiesProviders;

    /**
     * Creates a new wfs 1.0/1.1 GetCapabilitis operation.
     *
     * @param wfs The wfs configuration
     * @param catalog The geoserver catalog.
     * @param extendedCapabilitiesProviders the providers for adding extra metadata to the
     *     capabilities documents
     */
    public GetCapabilities(
            WFSInfo wfs,
            Catalog catalog,
            Collection<WFSExtendedCapabilitiesProvider> extendedCapabilitiesProviders) {
        this.wfs = wfs;
        this.catalog = catalog;
        this.extendedCapabilitiesProviders = extendedCapabilitiesProviders;
    }

    public CapabilitiesTransformer run(GetCapabilitiesRequest request) throws WFSException {
        // cite requires that we fail when we see an "invalid" update sequence,
        // since we dont support update sequences, all are invalid, but we take
        // our more lax approach and just ignore it when not doint the cite thing
        if (wfs.isCiteCompliant()) {
            if (request.getUpdateSequence() != null) {
                throw new WFSException(request, "Invalid update sequence", "InvalidUpdateSequence");
            }
        }

        // TODO: the rest of this routine should be done by the dispatcher
        // make sure service is set, cite conformance thing
        // JD - We wrap this in a cite conformance check because cite stricly
        // tests that every request includes the 'service=WFS' key value pair.
        // However often the the context of the request is good enough to
        // determine what the service is, like in 'geoserver/wfs?request=GetCapabilities'
        if (wfs.isCiteCompliant()) {
            if (!request.isSetService()) {
                // give up
                throw new WFSException("Service not set", "MissingParameterValue", "service");
            }
        }

        String version = version(request);

        String baseUrl = request.getBaseUrl();
        final CapabilitiesTransformer capsTransformer;
        if ("1.0.0".equals(version)) {
            capsTransformer = new CapabilitiesTransformer.WFS1_0(wfs, catalog);
        } else {
            if ("1.1.0".equals(version)) {
                capsTransformer =
                        new CapabilitiesTransformer.WFS1_1(
                                wfs, baseUrl, catalog, extendedCapabilitiesProviders);
            } else if ("2.0.0".equals(version)) {
                capsTransformer =
                        new CapabilitiesTransformer.WFS2_0(
                                wfs, baseUrl, catalog, extendedCapabilitiesProviders);
            } else {
                throw new WFSException(request, "Could not understand version:" + version);
            }
        }
        capsTransformer.setEncoding(Charset.forName(wfs.getGeoServer().getSettings().getCharset()));
        return capsTransformer;
    }

    public static String version(GetCapabilitiesRequest request) {
        // do the version negotiation dance
        List<String> provided = new ArrayList<>();
        provided.add("1.0.0");
        provided.add("1.1.0");

        if (request instanceof GetCapabilitiesRequest.WFS20) {
            provided.add("2.0.0");
        }

        List<String> accepted = request.getAcceptVersions();

        String version = RequestUtils.getVersionPreOws(provided, accepted);
        return version;
    }
}
