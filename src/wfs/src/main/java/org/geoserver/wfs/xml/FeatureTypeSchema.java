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

package org.geoserver.wfs.xml;

import java.io.IOException;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.WFSInfo;

/**
 * An xml schema describing a wfs feature type.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class FeatureTypeSchema {
    /** The feature type metadata object. */
    protected FeatureTypeInfo featureType;

    /** The xsd schema builder. */
    protected FeatureTypeSchemaBuilder builder;

    /** The catalog */
    protected Catalog catalog;

    /** WFS configuration */
    protected WFSInfo wfs;

    protected FeatureTypeSchema(FeatureTypeInfo featureType, WFSInfo wfs, Catalog catalog) {
        this.featureType = featureType;
        this.catalog = catalog;
        this.wfs = wfs;
    }

    /** @return The feautre type info. */
    FeatureTypeInfo getFeatureType() {
        return featureType;
    }

    /** @return The {@link XSDSchema} representation of the schema. */
    public XSDSchema schema(String baseUrl) throws IOException {
        return builder.build(new FeatureTypeInfo[] {featureType}, baseUrl);
    }

    /** Converts the schema to a gml2 schema. */
    public FeatureTypeSchema toGML2() {
        if (this instanceof GML2) {
            return this;
        }

        return new GML2(featureType, wfs, catalog);
    }

    /** Converts the schema to a gml3 schema. */
    public FeatureTypeSchema toGML3() {
        if (this instanceof GML3) {
            return this;
        }

        return new GML3(featureType, wfs, catalog);
    }

    /**
     * GML2 based wfs feature type schema.
     *
     * @author Justin Deoliveira, The Open Planning Project
     */
    public static final class GML2 extends FeatureTypeSchema {
        public GML2(FeatureTypeInfo featureType, WFSInfo wfs, Catalog catalog) {
            super(featureType, wfs, catalog);
            builder = new FeatureTypeSchemaBuilder.GML2(wfs.getGeoServer());
        }
    }

    /**
     * GML3 based wfs feature type schema.
     *
     * @author Justin Deoliveira, The Open Planning Project
     */
    public static final class GML3 extends FeatureTypeSchema {
        protected GML3(FeatureTypeInfo featureType, WFSInfo wfs, Catalog catalog) {
            super(featureType, wfs, catalog);
            builder = new FeatureTypeSchemaBuilder.GML3(wfs.getGeoServer());
        }
    }
}
