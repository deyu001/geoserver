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

import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.dataObjects.AbstractRasterLayer;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GTRasterLayer extends AbstractRasterLayer {

    private CoordinateReferenceSystem m_CRS;
    private String m_sFilename;
    private String m_sName = "";
    private PlanarImage m_image;
    private AnalysisExtent m_LayerExtent;
    private double m_dNoDataValue;
    private Object m_BaseDataObject;

    public void create(
            final String name,
            final String filename,
            final AnalysisExtent ge,
            final int dataType,
            final int numBands,
            Object crs,
            final double defaultNoDataValue) {

        if (!(crs instanceof CoordinateReferenceSystem) || (crs == null)) {
            crs = DefaultGeographicCRS.WGS84;
        }

        m_CRS = (CoordinateReferenceSystem) crs;

        final Raster m_Raster =
                RasterFactory.createBandedRaster(dataType, ge.getNX(), ge.getNY(), numBands, null);

        double width = ge.getXMax() - ge.getXMin();
        double height = ge.getYMax() - ge.getYMin();
        final Envelope envelope =
                new Envelope2D(
                        (CoordinateReferenceSystem) crs, ge.getXMin(), ge.getYMin(), width, height);

        final GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        final GridCoverage2D gc =
                factory.create(
                        name, (WritableRaster) m_Raster, envelope, null, null, null, null, null);
        m_BaseDataObject = gc;
        m_sName = name;
        m_sFilename = filename;
        m_LayerExtent = ge;
        m_image = (PlanarImage) gc.getRenderedImage();
        m_dNoDataValue = defaultNoDataValue;
        initNoData((GridCoverage2D) m_BaseDataObject);
    }

    private void initNoData(final GridCoverage2D gc) {
        final Object value = gc.getProperty("GC_NODATA");
        if ((value != null) && (value instanceof Number)) {
            m_dNoDataValue = ((Number) value).doubleValue();
            return;
        } else {
            final GridSampleDimension[] dimList = gc.getSampleDimensions();
            double[] noDataList;
            for (int i = 0; i < dimList.length; i++) {
                noDataList = dimList[i].getNoDataValues();
                if ((noDataList != null) && (noDataList.length > 0)) {
                    m_dNoDataValue = noDataList[0];
                    return;
                }
            }
        }
        // ensure we got a sensible result, because some GridCoverage2D are not able to provide
        // no-data values
        // setNoDataValue(CommandLineData.getDefaultNoData());
    }

    public void create(final Object obj) {

        if (obj instanceof GridCoverage2D) {
            m_BaseDataObject = obj;
            final GridCoverage2D gc = ((GridCoverage2D) obj);
            m_CRS = gc.getCoordinateReferenceSystem();
            final Envelope2D env = gc.getEnvelope2D();
            m_LayerExtent = new AnalysisExtent();
            m_LayerExtent.setCellSize(
                    (env.getMaxX() - env.getMinX()) / gc.getRenderedImage().getWidth());
            m_LayerExtent.setXRange(env.getMinX(), env.getMaxX(), true);
            m_LayerExtent.setYRange(env.getMinY(), env.getMaxY(), true);
            m_image = (PlanarImage) gc.getRenderedImage();
            m_sName = gc.getName().toString();
            m_dNoDataValue =
                    -99999; // -> Default value in 'OutputFactory::getDefaultNoDataValue()'.
            initNoData(gc);
        }
    }

    public int getBandsCount() {

        if (m_BaseDataObject != null) {
            final GridCoverage2D gc = (GridCoverage2D) m_BaseDataObject;
            return gc.getNumSampleDimensions();
        } else {
            return 0;
        }
    }

    public double getCellValueInLayerCoords(final int x, final int y, final int band) {

        try {
            if (m_image != null) {
                return getTile(x, y).getSampleDouble(x, y, band);
            } else {
                return getNoDataValue();
            }
        } catch (final Exception e) {
            return getNoDataValue();
        }
    }

    public int getDataType() {

        if (m_image != null) {
            return m_image.getTile(0, 0).getDataBuffer().getDataType();
        } else {
            return DataBuffer.TYPE_DOUBLE;
        }
    }

    public double getLayerCellSize() {

        if (m_LayerExtent != null) {
            return m_LayerExtent.getCellSize();
        } else {
            return 0;
        }
    }

    public AnalysisExtent getLayerGridExtent() {

        return m_LayerExtent;
    }

    public double getNoDataValue() {

        return m_dNoDataValue;
    }

    public void setCellValue(final int x, final int y, final int band, final double value) {

        if (isInWindow(x, y)) {
            final Raster raster = getTile(x, y);
            if (raster instanceof WritableRaster) {
                ((WritableRaster) raster).setSample(x, y, band, value);
            }
        }
    }

    public void setNoDataValue(final double noDataValue) {

        m_dNoDataValue = noDataValue;
    }

    public Object getCRS() {

        return m_CRS;
    }

    public Rectangle2D getFullExtent() {

        if (m_BaseDataObject != null) {
            final GridCoverage2D gc = (GridCoverage2D) m_BaseDataObject;
            return new Envelope2D(gc.getEnvelope());
        } else {
            return null;
        }
    }

    public void open() {}

    public void close() {}

    public void postProcess() {}

    public IOutputChannel getOutputChannel() {

        return new FileOutputChannel(m_sFilename);
    }

    public String getName() {

        return m_sName;
    }

    public void setName(final String sName) {

        m_sName = sName;
    }

    private Raster getTile(final int x, final int y) {
        return m_image.getTile(m_image.XToTileX(x), m_image.YToTileY(y));
    }

    @Override
    public void free() {

        ((GridCoverage2D) m_BaseDataObject).dispose(true);
        m_BaseDataObject = null;
    }

    @Override
    public Object getBaseDataObject() {

        return m_BaseDataObject;
    }
}
