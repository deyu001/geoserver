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

package org.geoserver.wps.sextante;

import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.AbstractVectorLayer;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.dataObjects.vectorFilters.IVectorLayerFilter;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import java.io.File;
import java.io.IOException;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GTVectorLayer extends AbstractVectorLayer {

    private FeatureSource m_FeatureSource;
    private DefaultFeatureCollection m_FeatureCollection;
    private String m_sName;
    private String m_sFilename;
    private int m_iShapeType;
    private Class[] m_Types;
    private String[] m_sFields;
    private Object m_CRS;
    private SimpleFeatureType m_SFT;

    public void create(final FeatureSource fs) {

        m_FeatureSource = fs;
        try {
            final FeatureType ft = fs.getSchema();
            final Class<?> type = ft.getGeometryDescriptor().getType().getBinding();
            if (type.isAssignableFrom(Polygon.class) || type.isAssignableFrom(MultiPolygon.class)) {
                m_iShapeType = IVectorLayer.SHAPE_TYPE_POLYGON;
            } else if (type.isAssignableFrom(LineString.class)
                    || type.isAssignableFrom(MultiLineString.class)) {
                m_iShapeType = IVectorLayer.SHAPE_TYPE_LINE;
            } else {
                m_iShapeType = IVectorLayer.SHAPE_TYPE_POINT;
            }
        } catch (final Exception e) {
            Sextante.addErrorToLog(e);
            m_iShapeType = IVectorLayer.SHAPE_TYPE_POLYGON;
        }

        try {
            final SimpleFeatureType ft = (SimpleFeatureType) fs.getSchema();
            m_CRS = ft.getCoordinateReferenceSystem();
            m_sFields = new String[ft.getAttributeCount() - 1];
            m_Types = new Class[ft.getAttributeCount() - 1];
            for (int j = 0; j < m_sFields.length; j++) {
                final AttributeType at = ft.getType(j + 1);
                m_sFields[j] = at.getName().getLocalPart();
                m_Types[j] = at.getBinding();
            }
        } catch (final Exception e) {
            Sextante.addErrorToLog(e);
        }
    }

    public void create(
            final String name,
            final int shapeType,
            final Class[] types,
            final String[] fields,
            final String filename,
            final Object crs) {

        try {
            m_sName = name;
            m_sFilename = filename;
            m_iShapeType = shapeType;
            m_Types = types;
            m_sFields = fields;
            m_CRS = crs;

            m_FeatureCollection = new DefaultFeatureCollection();

            final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("Location");
            builder.setCRS((CoordinateReferenceSystem) m_CRS);

            if (m_iShapeType == IVectorLayer.SHAPE_TYPE_POINT) {
                builder.add("geom", MultiPoint.class);
            } else if (m_iShapeType == IVectorLayer.SHAPE_TYPE_LINE) {
                builder.add("geom", MultiLineString.class);
            } else {
                builder.add("geom", MultiPolygon.class);
            }

            for (int i = 0; i < m_sFields.length; i++) {
                builder.add(m_sFields[i], m_Types[i]);
            }

            m_SFT = builder.buildFeatureType();

        } catch (final Exception e) {
            // TODO:handle this
        }
    }

    @Override
    public void addFeature(final Geometry g, final Object[] attributes) {

        Geometry geom;
        final GeometryFactory gf = new GeometryFactory();
        if (g instanceof Point) {
            geom = gf.createMultiPoint(new Point[] {(Point) g});
        } else if (g instanceof Polygon) {
            geom = gf.createMultiPolygon(new Polygon[] {(Polygon) g});
        } else if (g instanceof LineString) {
            geom = gf.createMultiLineString(new LineString[] {(LineString) g});
        } else {
            geom = g;
        }

        final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(m_SFT);
        featureBuilder.add(geom);
        featureBuilder.addAll(attributes);
        final SimpleFeature feature = featureBuilder.buildFeature(null);
        m_FeatureCollection.add(feature);
    }

    @Override
    public void addFeature(final IFeature feature) {

        addFeature(feature.getGeometry(), feature.getRecord().getValues());
    }

    @Override
    public void addFilter(final IVectorLayerFilter filter) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canBeEdited() {

        return true;
    }

    @Override
    public int getFieldCount() {

        return m_sFields.length;
    }

    @Override
    public String getFieldName(final int index) {

        return m_sFields[index];
    }

    @Override
    public Class getFieldType(final int index) {

        return m_Types[index];
    }

    @Override
    public int getShapeType() {

        return m_iShapeType;
    }

    @Override
    public IFeatureIterator iterator() {

        if (m_FeatureSource != null) {
            FeatureCollection fc;
            try {
                fc = m_FeatureSource.getFeatures();
            } catch (final IOException e) {
                return null;
            }
            return new GTFeatureIterator(fc);
        } else {
            return null;
        }
    }

    @Override
    public Object getCRS() {

        return m_CRS;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void free() {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getBaseDataObject() {

        if (m_FeatureCollection != null) {
            return m_FeatureCollection;
        } else {
            return m_FeatureSource;
        }
    }

    @Override
    public String getName() {

        return m_sName;
    }

    @Override
    public IOutputChannel getOutputChannel() {

        return new FileOutputChannel(m_sFilename);
    }

    @Override
    public void open() {

        try {
            if (m_FeatureCollection != null) {
                postProcess();
            }
            if (m_sFilename != null) {
                final FileDataStore store = FileDataStoreFinder.getDataStore(new File(m_sFilename));
                final SimpleFeatureSource featureSource = store.getFeatureSource();
                create(featureSource);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postProcess() {}

    @Override
    public void setName(final String name) {

        m_sName = name;
    }
}
