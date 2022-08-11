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

package org.geoserver.wms.describelayer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.WMS;
import org.geotools.ows.wms.LayerDescription;
import org.geotools.util.logging.Logging;

/**
 * A DescribeLayer response specialized in producing Json or JsonP data for a DescribeLayer request.
 *
 * @author carlo cancellieri - GeoSolutions
 */
public class JSONDescribeLayerResponse extends DescribeLayerResponse {

    /** A logger for this class. */
    protected static final Logger LOGGER = Logging.getLogger(JSONDescribeLayerResponse.class);

    /**
     * The MIME type of the format this response produces, supported formats see {@link JSONType}
     */
    private final JSONType type;

    protected final WMS wms;

    /** Constructor for subclasses */
    public JSONDescribeLayerResponse(final WMS wms, final String outputFormat) {
        super(outputFormat);
        this.wms = wms;
        this.type = JSONType.getJSONType(outputFormat);
        if (type == null)
            throw new IllegalArgumentException("Not supported mime type for:" + outputFormat);
    }

    /** Actually write the passed DescribeLayerModel on the OutputStream */
    @SuppressWarnings("PMD.CloseResource") // just creates wrappers, actual out managed by servlet
    public void write(DescribeLayerModel layers, DescribeLayerRequest request, OutputStream output)
            throws ServiceException, IOException {

        switch (type) {
            case JSON:
                OutputStreamWriter osw =
                        new OutputStreamWriter(
                                output, wms.getGeoServer().getSettings().getCharset());
                Writer outWriter = new BufferedWriter(osw);

                writeJSON(outWriter, layers);
                outWriter.flush();
                break;
            case JSONP:
                writeJSONP(output, layers);
        }
    }

    @SuppressWarnings("PMD.CloseResource") // just a wrapper, actual output managed by servlet
    private void writeJSONP(OutputStream out, DescribeLayerModel layers) throws IOException {
        // prepare to write out
        OutputStreamWriter osw =
                new OutputStreamWriter(out, wms.getGeoServer().getSettings().getCharset());
        Writer outWriter = new BufferedWriter(osw);

        outWriter.write(getCallbackFunction() + "(");

        writeJSON(outWriter, layers);

        outWriter.write(")");
        outWriter.flush();
    }

    private void writeJSON(Writer outWriter, DescribeLayerModel description) throws IOException {

        try {
            JSONBuilder json = new JSONBuilder(outWriter);
            final List<LayerDescription> layers = description.getLayerDescriptions();
            json.object();
            json.key("version").value(description.getVersion());
            json.key("layerDescriptions");
            json.array();
            for (LayerDescription layer : layers) {
                json.object();
                json.key("layerName").value(layer.getName());
                URL url = layer.getOwsURL();
                json.key("owsURL").value(url != null ? url.toString() : "");
                json.key("owsType").value(layer.getOwsType());
                json.key("typeName").value(layer.getName());
                json.endObject();
            }
            json.endArray();
            json.endObject();
        } catch (JSONException jsonException) {
            ServiceException serviceException =
                    new ServiceException("Error: " + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }
    }

    private static String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        } else {
            return JSONType.getCallbackFunction(request.getKvp());
        }
    }

    @Override
    public String getCharset(Operation operation) {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
