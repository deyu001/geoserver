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

package org.geoserver.wfs3.response;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.URLMangler;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.DefaultWebFeatureService30;
import org.geoserver.wfs3.LandingPageRequest;

/**
 * A class representing the WFS3 server "contents" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JacksonXmlRootElement(localName = "LandingPage")
public class LandingPageDocument extends AbstractDocument {

    public LandingPageDocument(LandingPageRequest request, WFSInfo wfs, Catalog catalog) {
        String baseUrl = request.getBaseUrl();

        // self and alternate representations of landing page
        addLinksFor(
                baseUrl,
                "wfs3/",
                LandingPageDocument.class,
                "This document as ",
                "landingPage",
                (format, link) -> {
                    String outputFormat = request.getOutputFormat();
                    if (format.equals(outputFormat)
                            || (outputFormat == null && BaseRequest.JSON_MIME.equals(format))) {
                        link.setRel(Link.REL_SELF);
                        link.setTitle("This document");
                    }
                },
                Link.REL_ALTERNATE);
        // api
        addLinksFor(
                baseUrl,
                "wfs3/api",
                OpenAPI.class,
                "API definition for this endpoint as ",
                "api",
                null,
                Link.REL_SERVICE);
        // conformance
        addLinksFor(
                baseUrl,
                "wfs3/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                "conformance");
        // collections
        addLinksFor(
                baseUrl,
                "wfs3/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                "data");
    }

    /** Builds service links for the given response types */
    private void addLinksFor(
            String baseUrl,
            String path,
            Class<?> responseType,
            String titlePrefix,
            String classification,
            BiConsumer<String, Link> linkUpdater,
            String rel) {
        for (String format : DefaultWebFeatureService30.getAvailableFormats(responseType)) {
            Map<String, String> params = Collections.singletonMap("f", format);
            String url = buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
            String linkTitle = titlePrefix + format;
            Link link = new Link(url, rel, format, linkTitle);
            link.setClassification(classification);
            if (linkUpdater != null) {
                linkUpdater.accept(format, link);
            }
            addLink(link);
        }
    }
}
