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

package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Represents a JSON/XML link */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Link {

    public static final String REL_SERVICE = "service";
    public static final String REL_SELF = "self";
    public static final String REL_ALTERNATE = "alternate";
    public static final String REL_ABOUT = "about";
    public static final String REL_ITEM = "item";
    public static final String REL_ITEMS = "items";
    public static final String REL_DESCRIBEDBY = "describedBy";
    public static final String REL_DATA = "data";
    /**
     * Refers to the root resource of a dataset in an API.
     *
     * <p>This is an OGC definition, from OGC API Common Part 1: Core specification.
     */
    public static final String REL_DATA_URI = "http://www.opengis.net/def/rel/ogc/1.0/data";

    public static final String REL_COLLECTION = "collection";
    public static final String REL_SERVICE_DESC = "service-desc";
    public static final String REL_SERVICE_DOC = "service-doc";
    public static final String REL_CONFORMANCE = "conformance";
    /**
     * Refers to a resource that identifies the specifications that the link’s context conforms to.
     *
     * <p>This is an OGC definition, from OGC API Common Part 1: Core specification.
     */
    public static final String REL_CONFORMANCE_URI =
            "http://www.opengis.net/def/rel/ogc/1.0/conformance";

    public static final String ATOM_NS = "http://www.w3.org/2005/Atom";

    String href;
    String rel;
    String type;
    String title;
    String classification;
    Boolean templated;

    public Link() {}

    public Link(String href, String rel, String type, String title, String classification) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.title = title;
        this.classification = classification;
    }

    public Link(String href, String rel, String type, String title) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.title = title;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonIgnore
    public String getClassification() {
        return classification == null ? rel : classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Boolean isTemplated() {
        return templated;
    }

    public void setTemplated(Boolean templated) {
        this.templated = templated;
    }

    @Override
    public String toString() {
        return "Link{"
                + "href='"
                + href
                + '\''
                + ", rel='"
                + rel
                + '\''
                + ", type='"
                + type
                + '\''
                + ", title='"
                + title
                + '\''
                + ", classification='"
                + classification
                + '\''
                + ", templated="
                + templated
                + '}';
    }
}
