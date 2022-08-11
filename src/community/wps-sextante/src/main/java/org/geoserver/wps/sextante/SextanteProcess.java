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

import static org.geoserver.wps.sextante.SextanteProcessFactory.*;

import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.ITaskMonitor;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.dataObjects.IDataObject;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.outputs.Output;
import es.unex.sextante.outputs.OutputRasterLayer;
import es.unex.sextante.parameters.Parameter;
import java.util.HashMap;
import java.util.Map;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.Process;
import org.locationtech.jts.geom.Envelope;
import org.opengis.util.ProgressListener;

public class SextanteProcess implements Process {

    private GeoAlgorithm m_Algorithm;

    /**
     * Constructs a new process based on a SEXTANTE geoalgorithm
     *
     * @param algorithm the SEXTANTE geoalgorithm
     */
    public SextanteProcess(GeoAlgorithm algorithm) {

        m_Algorithm = algorithm;
    }

    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor) {

        ITaskMonitor taskMonitor = new ProgressListenerTaskMonitor(monitor);

        try {
            setAlgorithmInputs(input);

            /*
             * Execute the algorithm
             * The output factory tells the algorithm how to create
             * new data objects (layers and tables)
             * Since we are working with GeoTools , we use the
             * GTOutputFactory, which creates objects based on geotools
             * data objects (DataStore and GridCoverage)
             */

            if (m_Algorithm.execute(taskMonitor, new GTOutputFactory())) {
                return createReturnMapFromOutputObjects();
            } else {
                // if the execution was canceled, we return null
                return null;
            }

        } catch (GeoAlgorithmExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a suitable return map for this process from the outputs generated by the SEXTANTE
     * algorithm
     *
     * @return a map with algorithm results
     */
    private Map<String, Object> createReturnMapFromOutputObjects() {
        try {
            Map<String, Object> results = new HashMap<String, Object>();

            OutputObjectsSet outputs = m_Algorithm.getOutputObjects();
            for (int i = 0; i < outputs.getOutputObjectsCount(); i++) {
                Output output = outputs.getOutput(i);
                Object outputObject = output.getOutputObject();
                // if the output object is a layer or a table, we return
                // the inner GeoTools object
                if (outputObject instanceof IDataObject) {
                    IDataObject dataObject = (IDataObject) outputObject;
                    Object wrapped = dataObject.getBaseDataObject();
                    if (wrapped instanceof FeatureSource) {
                        results.put(output.getName(), ((FeatureSource) wrapped).getFeatures());
                    } else if (wrapped instanceof GridCoverage2D) {
                        results.put(output.getName(), wrapped);
                    } else {
                        results.put(output.getName(), wrapped);
                    }

                } else {
                    results.put(output.getName(), outputObject);
                }
            }
            return results;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Sets the input of the SEXTANTE algorithm from the input map of this process
     *
     * @param input the input map of this process
     */
    private void setAlgorithmInputs(Map<String, Object> input)
            throws GeoAlgorithmExecutionException {

        // set the normal parameters
        ParametersSet paramSet = (ParametersSet) m_Algorithm.getParameters();
        boolean gridExtendRequired = false;
        for (String sKey : input.keySet()) {
            if (SEXTANTE_GRID_CELL_SIZE.equals(sKey) || SEXTANTE_GRID_ENVELOPE.equals(sKey)) {
                // these two parameters we made up to expose the GridExtent, we'll deal with them
                // later
                continue;
            }
            Object paramValue = input.get(sKey);
            Parameter param = paramSet.getParameter(sKey);
            if (paramValue instanceof FeatureCollection) {
                GTVectorLayer layer = new GTVectorLayer();
                layer.create(DataUtilities.source((FeatureCollection) paramValue));
                param.setParameterValue(layer);
            } else if (paramValue instanceof GridCoverage2D) {
                GTRasterLayer layer = new GTRasterLayer();
                gridExtendRequired = true;
                layer.create(paramValue);
                param.setParameterValue(layer);
            } else {
                param.setParameterValue(paramValue);
            }
        }

        // check the outputs as well for raster data
        OutputObjectsSet outputs = m_Algorithm.getOutputObjects();
        for (int i = 0; i < outputs.getOutputObjectsCount(); i++) {
            Output output = outputs.getOutput(i);
            if (output instanceof OutputRasterLayer) {
                gridExtendRequired = true;
            }
        }

        // handle the grid extent if necessary
        if (gridExtendRequired) {
            // get the cell size
            double cellSize = Double.NaN;
            if (input.get(SEXTANTE_GRID_CELL_SIZE) != null) {
                cellSize = (Double) input.get(SEXTANTE_GRID_CELL_SIZE);
            } else {
                for (String sKey : input.keySet()) {
                    Object value = paramSet.getParameter(sKey).getParameterValueAsObject();
                    if (value instanceof GTRasterLayer) {
                        cellSize = ((GTRasterLayer) value).getLayerCellSize();
                        return;
                    }
                }
            }
            if (Double.isNaN(cellSize)) {
                throw new GeoAlgorithmExecutionException(
                        SEXTANTE_GRID_CELL_SIZE
                                + " parameter could not be derived from inputs, and is not available among ");
            }

            // get the extents
            Envelope envelope = null;
            if (input.get(SEXTANTE_GRID_ENVELOPE) != null) {
                envelope = (Envelope) input.get(SEXTANTE_GRID_ENVELOPE);
            } else {
                for (String sKey : input.keySet()) {
                    Object value = paramSet.getParameter(sKey).getParameterValueAsObject();
                    if (value instanceof GTRasterLayer) {
                        AnalysisExtent ge = ((GTRasterLayer) value).getLayerGridExtent();
                        Envelope genv =
                                new Envelope(
                                        ge.getXMin(), ge.getXMax(), ge.getYMin(), ge.getYMax());
                        if (envelope == null) {
                            envelope = genv;
                        } else {
                            envelope.expandToInclude(genv);
                        }
                        return;
                    }
                }
            }
            if (envelope == null) {
                if (Double.isNaN(cellSize)) {
                    throw new GeoAlgorithmExecutionException(
                            SEXTANTE_GRID_ENVELOPE
                                    + " parameter could not be derived from inputs, and is not available among ");
                }
            }

            // build and set the grid extends
            AnalysisExtent extent = new AnalysisExtent();
            extent.setXRange(envelope.getMinX(), envelope.getMaxX(), false);
            extent.setYRange(envelope.getMinY(), envelope.getMaxY(), false);
            extent.setCellSize(cellSize);
            m_Algorithm.setAnalysisExtent(extent);
        }
    }
}
