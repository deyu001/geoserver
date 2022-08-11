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

package org.geoserver.wfs.xml.v1_0_0;

import java.io.IOException;
import java.util.Set;
import javax.xml.namespace.QName;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geotools.filter.v1_0.OGC;
import org.geotools.gml2.GML;
import org.geotools.xsd.XSD;

/**
 * XSD object for GeoServer WFS 1.0.
 *
 * <p>This object is not a singleton in the conventional java sense as the other XSD subclasses
 * (GML,OGC,OWS,etc..) are. It is a singleton, but managed as such by the spring container. The
 * reason being that it requires the catalog to operate and build the underlying schema.
 */
public final class WFS extends XSD {

    /** @generated */
    public static final String NAMESPACE = "http://www.opengis.net/wfs";

    public static final String CANONICAL_SCHEMA_LOCATION_BASIC =
            "http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd";

    public static final String CANONICAL_SCHEMA_LOCATION_CAPABILITIES =
            "http://schemas.opengis.net/wfs/1.0.0/WFS-capabilities.xsd";

    /* Type Definitions */
    /** @generated */
    public static final QName ALLSOMETYPE = new QName("http://www.opengis.net/wfs", "AllSomeType");

    /** @generated */
    public static final QName DELETEELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "DeleteElementType");

    /** @generated */
    public static final QName DESCRIBEFEATURETYPETYPE =
            new QName("http://www.opengis.net/wfs", "DescribeFeatureTypeType");

    /** @generated */
    public static final QName EMPTYTYPE = new QName("http://www.opengis.net/wfs", "EmptyType");

    /** @generated */
    public static final QName FEATURECOLLECTIONTYPE =
            new QName("http://www.opengis.net/wfs", "FeatureCollectionType");

    /** @generated */
    public static final QName FEATURESLOCKEDTYPE =
            new QName("http://www.opengis.net/wfs", "FeaturesLockedType");

    /** @generated */
    public static final QName FEATURESNOTLOCKEDTYPE =
            new QName("http://www.opengis.net/wfs", "FeaturesNotLockedType");

    /** @generated */
    public static final QName GETCAPABILITIESTYPE =
            new QName("http://www.opengis.net/wfs", "GetCapabilitiesType");

    /** @generated */
    public static final QName GETFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "GetFeatureType");

    /** @generated */
    public static final QName GETFEATUREWITHLOCKTYPE =
            new QName("http://www.opengis.net/wfs", "GetFeatureWithLockType");

    /** @generated */
    public static final QName INSERTELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "InsertElementType");

    /** @generated */
    public static final QName INSERTRESULTTYPE =
            new QName("http://www.opengis.net/wfs", "InsertResultType");

    /** @generated */
    public static final QName LOCKFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "LockFeatureType");

    /** @generated */
    public static final QName LOCKTYPE = new QName("http://www.opengis.net/wfs", "LockType");

    /** @generated */
    public static final QName NATIVETYPE = new QName("http://www.opengis.net/wfs", "NativeType");

    /** @generated */
    public static final QName PROPERTYTYPE =
            new QName("http://www.opengis.net/wfs", "PropertyType");

    /** @generated */
    public static final QName QUERYTYPE = new QName("http://www.opengis.net/wfs", "QueryType");

    /** @generated */
    public static final QName STATUSTYPE = new QName("http://www.opengis.net/wfs", "StatusType");

    /** @generated */
    public static final QName TRANSACTIONRESULTTYPE =
            new QName("http://www.opengis.net/wfs", "TransactionResultType");

    /** @generated */
    public static final QName TRANSACTIONTYPE =
            new QName("http://www.opengis.net/wfs", "TransactionType");

    /** @generated */
    public static final QName UPDATEELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "UpdateElementType");

    /** @generated */
    public static final QName WFS_LOCKFEATURERESPONSETYPE =
            new QName("http://www.opengis.net/wfs", "WFS_LockFeatureResponseType");

    /** @generated */
    public static final QName WFS_TRANSACTIONRESPONSETYPE =
            new QName("http://www.opengis.net/wfs", "WFS_TransactionResponseType");

    /* Elements */
    /** @generated */
    public static final QName DELETE = new QName("http://www.opengis.net/wfs", "Delete");

    /** @generated */
    public static final QName DESCRIBEFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "DescribeFeatureType");

    /** @generated */
    public static final QName FAILED = new QName("http://www.opengis.net/wfs", "FAILED");

    /** @generated */
    public static final QName FEATURECOLLECTION =
            new QName("http://www.opengis.net/wfs", "FeatureCollection");

    /** @generated */
    public static final QName GETCAPABILITIES =
            new QName("http://www.opengis.net/wfs", "GetCapabilities");

    /** @generated */
    public static final QName GETFEATURE = new QName("http://www.opengis.net/wfs", "GetFeature");

    /** @generated */
    public static final QName GETFEATUREWITHLOCK =
            new QName("http://www.opengis.net/wfs", "GetFeatureWithLock");

    /** @generated */
    public static final QName INSERT = new QName("http://www.opengis.net/wfs", "Insert");

    /** @generated */
    public static final QName LOCKFEATURE = new QName("http://www.opengis.net/wfs", "LockFeature");

    /** @generated */
    public static final QName LOCKID = new QName("http://www.opengis.net/wfs", "LockId");

    /** @generated */
    public static final QName NATIVE = new QName("http://www.opengis.net/wfs", "Native");

    /** @generated */
    public static final QName PARTIAL = new QName("http://www.opengis.net/wfs", "PARTIAL");

    /** @generated */
    public static final QName PROPERTY = new QName("http://www.opengis.net/wfs", "Property");

    /** @generated */
    public static final QName QUERY = new QName("http://www.opengis.net/wfs", "Query");

    /** @generated */
    public static final QName SUCCESS = new QName("http://www.opengis.net/wfs", "SUCCESS");

    /** @generated */
    public static final QName TRANSACTION = new QName("http://www.opengis.net/wfs", "Transaction");

    /** @generated */
    public static final QName UPDATE = new QName("http://www.opengis.net/wfs", "Update");

    /** @generated */
    public static final QName WFS_LOCKFEATURERESPONSE =
            new QName("http://www.opengis.net/wfs", "WFS_LockFeatureResponse");

    /** @generated */
    public static final QName WFS_TRANSACTIONRESPONSE =
            new QName("http://www.opengis.net/wfs", "WFS_TransactionResponse");

    /* Attributes */

    /** schema type builder */
    FeatureTypeSchemaBuilder schemaBuilder;

    public WFS(FeatureTypeSchemaBuilder schemaBuilder) {
        this.schemaBuilder = schemaBuilder;
    }

    public FeatureTypeSchemaBuilder getSchemaBuilder() {
        return schemaBuilder;
    }

    /** Adds dependencies on the filter and gml schemas. */
    protected void addDependencies(Set<XSD> dependencies) {
        dependencies.add(OGC.getInstance());
        dependencies.add(GML.getInstance());
    }

    /** Returns 'http://www.opengis.net/wfs' */
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    /** Returns the location of 'WFS-transaction.xsd' */
    public String getSchemaLocation() {
        return getClass().getResource("WFS-transaction.xsd").toString();
    }

    /**
     * Suplements the schema built by the parent by adding hte aplication schema feature typs
     * defined in GeoServer.
     */
    protected XSDSchema buildSchema() throws IOException {
        XSDSchema wfsSchema = super.buildSchema();
        wfsSchema = schemaBuilder.addApplicationTypes(wfsSchema);
        return wfsSchema;
    }
}
