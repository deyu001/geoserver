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

package org.geoserver.kml.regionate;

import java.sql.Connection;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.spatial.BBOX;

/**
 * An attribute based regionating strategy assuming it's possible (and fast) to sort on the user
 * specified attribute. Features with higher values of the attribute will be found in higher tiles.
 *
 * @author Andrea Aime
 */
public class NativeSortRegionatingStrategy extends CachedHierarchyRegionatingStrategy {

    public NativeSortRegionatingStrategy(GeoServer gs) {
        super(gs);
    }

    static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    String attribute;

    FeatureSource fs;

    @Override
    protected String getDatabaseName(WMSMapContent con, Layer layer) throws Exception {
        fs = layer.getFeatureSource();
        SimpleFeatureType type = (SimpleFeatureType) fs.getSchema();

        // find out which attribute we're going to use
        Map options = con.getRequest().getFormatOptions();
        attribute = (String) options.get("regionateAttr");
        if (attribute == null) attribute = MapLayerInfo.getRegionateAttribute(featureType);
        if (attribute == null)
            throw new ServiceException("Regionating attribute has not been specified");

        // Make sure the attribute is actually there
        AttributeType attributeType = type.getType(attribute);
        if (attributeType == null) {
            throw new ServiceException(
                    "Could not find regionating attribute "
                            + attribute
                            + " in layer "
                            + featureType.getName());
        }

        // check we can actually sort on that attribute
        if (!fs.getQueryCapabilities().supportsSorting(ff.sort(attribute, SortOrder.DESCENDING)))
            throw new ServiceException(
                    "Native sorting on the "
                            + attribute
                            + " is not possible for layer "
                            + featureType.getName());

        // make sure a special db for this layer and attribute will be created
        return super.getDatabaseName(con, layer) + "_" + attribute;
    }

    @Override
    protected String getDatabaseName(FeatureTypeInfo cfg) throws Exception {
        return super.getDatabaseName(cfg) + "_" + MapLayerInfo.getRegionateAttribute(cfg);
    }

    public FeatureIterator getSortedFeatures(
            GeometryDescriptor geom,
            ReferencedEnvelope latLongEnv,
            ReferencedEnvelope nativeEnv,
            Connection cacheConn)
            throws Exception {
        // build the bbox filter
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        BBOX filter =
                ff.bbox(
                        geom.getLocalName(),
                        nativeEnv.getMinX(),
                        nativeEnv.getMinY(),
                        nativeEnv.getMaxX(),
                        nativeEnv.getMaxY(),
                        null);

        // build an optimized query (only the necessary attributes
        Query q = new Query();
        q.setFilter(filter);
        q.setPropertyNames(geom.getLocalName(), attribute);
        // TODO: enable this when JTS learns how to compute centroids
        // without triggering the
        // generation of Coordinate[] out of the sequences...
        // q.setHints(new Hints(Hints.JTS_COORDINATE_SEQUENCE_FACTORY,
        // PackedCoordinateSequenceFactory.class));
        q.setSortBy(ff.sort(attribute, SortOrder.DESCENDING));

        // return the reader
        return fs.getFeatures(q).features();
    }
}
