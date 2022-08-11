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
import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.UnsupportedOutputChannelException;
import es.unex.sextante.gui.core.DefaultTaskMonitor;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import es.unex.sextante.outputs.StreamOutputChannel;
import java.io.File;
import javax.swing.JDialog;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class GTOutputFactory extends OutputFactory {

    @Override
    public IVectorLayer getNewVectorLayer(
            final String sName,
            final int iShapeType,
            final Class[] types,
            final String[] sFields,
            final IOutputChannel channel,
            final Object crs,
            final int[] fieldSize)
            throws UnsupportedOutputChannelException {

        return getNewVectorLayer(sName, iShapeType, types, sFields, channel, crs);
    }

    @Override
    public IVectorLayer getNewVectorLayer(
            final String sName,
            final int iShapeType,
            final Class[] types,
            final String[] sFields,
            final IOutputChannel channel,
            final Object crs)
            throws UnsupportedOutputChannelException {

        if (channel instanceof FileOutputChannel) {
            final String sFilename = ((FileOutputChannel) channel).getFilename();
            createBaseDir(sFilename);
            final GTVectorLayer vectorLayer = new GTVectorLayer();
            vectorLayer.create(sName, iShapeType, types, sFields, sFilename, crs);
            return vectorLayer;
        } else if (channel instanceof StreamOutputChannel) {
            return new StreamOutputLayer(((StreamOutputChannel) channel).getStream());
        } else {
            throw new UnsupportedOutputChannelException();
        }
    }

    @Override
    public IRasterLayer getNewRasterLayer(
            final String sName,
            final int iDataType,
            final AnalysisExtent extent,
            final int iBands,
            final IOutputChannel channel,
            final Object crs)
            throws UnsupportedOutputChannelException {

        if (channel instanceof FileOutputChannel) {
            final String sFilename = ((FileOutputChannel) channel).getFilename();
            createBaseDir(sFilename);
            final GTRasterLayer layer = new GTRasterLayer();
            layer.create(
                    sName, sFilename, extent, iDataType, iBands, crs, this.getDefaultNoDataValue());
            return layer;
        } else {
            throw new UnsupportedOutputChannelException();
        }
    }

    @Override
    public ITable getNewTable(
            final String sName,
            final Class types[],
            final String[] sFields,
            final IOutputChannel channel)
            throws UnsupportedOutputChannelException {

        if (channel instanceof FileOutputChannel) {
            final String sFilename = ((FileOutputChannel) channel).getFilename();
            createBaseDir(sFilename);
            final GTTable table = new GTTable();
            table.create(sName, sFilename, types, sFields);
            return table;
        } else {
            throw new UnsupportedOutputChannelException();
        }
    }

    protected void createBaseDir(final String fileName) {
        // creates the base dir if it does not exist
        final File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    @Override
    public String getTempFolder() {

        return System.getProperty("java.io.tmpdir");
    }

    @Override
    public String[] getRasterLayerOutputExtensions() {

        return new String[] {"tif", "asc"};
    }

    @Override
    public String[] getVectorLayerOutputExtensions() {

        return new String[] {"shp"};
    }

    @Override
    public String[] getTableOutputExtensions() {

        return new String[] {"dbf"};
    }

    @Override
    public DefaultTaskMonitor getTaskMonitor(
            final String sTitle, final boolean bDeterminate, final JDialog parent) {

        return new DefaultTaskMonitor(sTitle, bDeterminate, parent);
    }

    @Override
    public Object getDefaultCRS() {

        return DefaultGeographicCRS.WGS84;
    }
}
