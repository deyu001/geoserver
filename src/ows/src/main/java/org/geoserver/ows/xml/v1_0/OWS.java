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

package org.geoserver.ows.xml.v1_0;

import java.util.Set;
import javax.xml.namespace.QName;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.XSD;

/**
 * This interface contains the qualified names of all the types,elements, and attributes in the
 * http://www.opengis.net/ows schema.
 *
 * @generated
 */
public class OWS extends XSD {
    /** singleton instance */
    private static OWS instance = new OWS();

    /** Returns the singleton instance. */
    public static OWS getInstance() {
        return instance;
    }

    /** @generated */
    public static final String NAMESPACE = "http://www.opengis.net/ows";

    /* Type Definitions */
    /** @generated */
    public static final QName ACCEPTFORMATSTYPE =
            new QName("http://www.opengis.net/ows", "AcceptFormatsType");

    /** @generated */
    public static final QName ACCEPTVERSIONSTYPE =
            new QName("http://www.opengis.net/ows", "AcceptVersionsType");

    /** @generated */
    public static final QName ADDRESSTYPE = new QName("http://www.opengis.net/ows", "AddressType");

    /** @generated */
    public static final QName BOUNDINGBOXTYPE =
            new QName("http://www.opengis.net/ows", "BoundingBoxType");

    /** @generated */
    public static final QName CAPABILITIESBASETYPE =
            new QName("http://www.opengis.net/ows", "CapabilitiesBaseType");

    /** @generated */
    public static final QName CODETYPE = new QName("http://www.opengis.net/ows", "CodeType");

    /** @generated */
    public static final QName CONTACTTYPE = new QName("http://www.opengis.net/ows", "ContactType");

    /** @generated */
    public static final QName DESCRIPTIONTYPE =
            new QName("http://www.opengis.net/ows", "DescriptionType");

    /** @generated */
    public static final QName DOMAINTYPE = new QName("http://www.opengis.net/ows", "DomainType");

    /** @generated */
    public static final QName EXCEPTIONTYPE =
            new QName("http://www.opengis.net/ows", "ExceptionType");

    /** @generated */
    public static final QName GETCAPABILITIESTYPE =
            new QName("http://www.opengis.net/ows", "GetCapabilitiesType");

    /** @generated */
    public static final QName IDENTIFICATIONTYPE =
            new QName("http://www.opengis.net/ows", "IdentificationType");

    /** @generated */
    public static final QName KEYWORDSTYPE =
            new QName("http://www.opengis.net/ows", "KeywordsType");

    /** @generated */
    public static final QName METADATATYPE =
            new QName("http://www.opengis.net/ows", "MetadataType");

    /** @generated */
    public static final QName MIMETYPE = new QName("http://www.opengis.net/ows", "MimeType");

    /** @generated */
    public static final QName ONLINERESOURCETYPE =
            new QName("http://www.opengis.net/ows", "OnlineResourceType");

    /** @generated */
    public static final QName POSITIONTYPE =
            new QName("http://www.opengis.net/ows", "PositionType");

    /** @generated */
    public static final QName POSITIONTYPE2D =
            new QName("http://www.opengis.net/ows", "PositionType2D");

    /** @generated */
    public static final QName REQUESTMETHODTYPE =
            new QName("http://www.opengis.net/ows", "RequestMethodType");

    /** @generated */
    public static final QName RESPONSIBLEPARTYSUBSETTYPE =
            new QName("http://www.opengis.net/ows", "ResponsiblePartySubsetType");

    /** @generated */
    public static final QName RESPONSIBLEPARTYTYPE =
            new QName("http://www.opengis.net/ows", "ResponsiblePartyType");

    /** @generated */
    public static final QName SECTIONSTYPE =
            new QName("http://www.opengis.net/ows", "SectionsType");

    /** @generated */
    public static final QName SERVICETYPE = new QName("http://www.opengis.net/ows", "ServiceType");

    /** @generated */
    public static final QName TELEPHONETYPE =
            new QName("http://www.opengis.net/ows", "TelephoneType");

    /** @generated */
    public static final QName UPDATESEQUENCETYPE =
            new QName("http://www.opengis.net/ows", "UpdateSequenceType");

    /** @generated */
    public static final QName VERSIONTYPE = new QName("http://www.opengis.net/ows", "VersionType");

    /** @generated */
    public static final QName WGS84BOUNDINGBOXTYPE =
            new QName("http://www.opengis.net/ows", "WGS84BoundingBoxType");

    /* Elements */
    /** @generated */
    public static final QName ABSTRACT = new QName("http://www.opengis.net/ows", "Abstract");

    /** @generated */
    public static final QName ABSTRACTMETADATA =
            new QName("http://www.opengis.net/ows", "AbstractMetaData");

    /** @generated */
    public static final QName ACCESSCONSTRAINTS =
            new QName("http://www.opengis.net/ows", "AccessConstraints");

    /** @generated */
    public static final QName AVAILABLECRS =
            new QName("http://www.opengis.net/ows", "AvailableCRS");

    /** @generated */
    public static final QName BOUNDINGBOX = new QName("http://www.opengis.net/ows", "BoundingBox");

    /** @generated */
    public static final QName CONTACTINFO = new QName("http://www.opengis.net/ows", "ContactInfo");

    /** @generated */
    public static final QName DCP = new QName("http://www.opengis.net/ows", "DCP");

    /** @generated */
    public static final QName EXCEPTION = new QName("http://www.opengis.net/ows", "Exception");

    /** @generated */
    public static final QName EXCEPTIONREPORT =
            new QName("http://www.opengis.net/ows", "ExceptionReport");

    /** @generated */
    public static final QName EXTENDEDCAPABILITIES =
            new QName("http://www.opengis.net/ows", "ExtendedCapabilities");

    /** @generated */
    public static final QName FEES = new QName("http://www.opengis.net/ows", "Fees");

    /** @generated */
    public static final QName GETCAPABILITIES =
            new QName("http://www.opengis.net/ows", "GetCapabilities");

    /** @generated */
    public static final QName HTTP = new QName("http://www.opengis.net/ows", "HTTP");

    /** @generated */
    public static final QName IDENTIFIER = new QName("http://www.opengis.net/ows", "Identifier");

    /** @generated */
    public static final QName INDIVIDUALNAME =
            new QName("http://www.opengis.net/ows", "IndividualName");

    /** @generated */
    public static final QName KEYWORDS = new QName("http://www.opengis.net/ows", "Keywords");

    /** @generated */
    public static final QName LANGUAGE = new QName("http://www.opengis.net/ows", "Language");

    /** @generated */
    public static final QName METADATA = new QName("http://www.opengis.net/ows", "Metadata");

    /** @generated */
    public static final QName OPERATION = new QName("http://www.opengis.net/ows", "Operation");

    /** @generated */
    public static final QName OPERATIONSMETADATA =
            new QName("http://www.opengis.net/ows", "OperationsMetadata");

    /** @generated */
    public static final QName ORGANISATIONNAME =
            new QName("http://www.opengis.net/ows", "OrganisationName");

    /** @generated */
    public static final QName OUTPUTFORMAT =
            new QName("http://www.opengis.net/ows", "OutputFormat");

    /** @generated */
    public static final QName POINTOFCONTACT =
            new QName("http://www.opengis.net/ows", "PointOfContact");

    /** @generated */
    public static final QName POSITIONNAME =
            new QName("http://www.opengis.net/ows", "PositionName");

    /** @generated */
    public static final QName ROLE = new QName("http://www.opengis.net/ows", "Role");

    /** @generated */
    public static final QName SERVICEIDENTIFICATION =
            new QName("http://www.opengis.net/ows", "ServiceIdentification");

    /** @generated */
    public static final QName SERVICEPROVIDER =
            new QName("http://www.opengis.net/ows", "ServiceProvider");

    /** @generated */
    public static final QName SUPPORTEDCRS =
            new QName("http://www.opengis.net/ows", "SupportedCRS");

    /** @generated */
    public static final QName TITLE = new QName("http://www.opengis.net/ows", "Title");

    /** @generated */
    public static final QName WGS84BOUNDINGBOX =
            new QName("http://www.opengis.net/ows", "WGS84BoundingBox");

    /* Attributes */

    /** Private constructor. */
    private OWS() {}

    /** Adds a dependency on the xlink schema. */
    protected void addDependencies(Set<XSD> dependencies) {
        super.addDependencies(dependencies);

        dependencies.add(XLINK.getInstance());
    }

    /** Returns 'http://www.opengis.net/ows' */
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    /** Returns the location of 'owsAll.xsd' */
    public String getSchemaLocation() {
        return getClass().getResource("owsAll.xsd").toString();
    }
}
