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

package org.geoserver.wfs.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs.response.dxf.DXFWriter;
import org.geoserver.wfs.response.dxf.DXFWriterFinder;
import org.geoserver.wfs.response.dxf.LineType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * This class returns a dxf encoded results of the users's query (optionally zipped). Several format
 * options are available to control output generation. - version: (number) creates a DXF in the
 * specified version format (a DXFWriter implementation supporting the requested version needs to be
 * available or an exception will be thrown); the default implementation creates a version 14 DXF. -
 * asblock: (true/false) if true, all geometries are written as blocks and then inserted as
 * entities. If false, simple geometries are directly written as entities. - colors: (comma
 * delimited list of numbers): colors to be used for the DXF layers, in sequence. If layers are more
 * than the specified colors, they will be reused many times. A set of default colors is used if the
 * option is not used. Colors are AutoCad color numbers (7=white, etc.). - ltypes: (comma delimited
 * list of line type descriptors): line types to be used for the DXF layers, in sequence. If layers
 * are more than the specified line types, they will be reused many times. If not specified, all
 * layers will be given a solid, continuous line type. A descriptor has the following format:
 * <name>!<repeatable pattern>[!<base length>], where <name> is the name assigned to the line type,
 * <base length> (optional) is a real number that tells how long is each part of the line pattern
 * (defaults to 0.125), and <repeatable pattern> is a visual description of the repeatable part of
 * the line pattern, as a sequence of - (solid line), * (dot) and _ (empty space). - layers: (comma
 * delimited list of strings) names to be assigned to the DXF layers. If specified, must contain a
 * name for each requested query. By default a standard name will be assigned to layers.
 *
 * <p>A different layer will be generated for each requested query. Layer names can be chosen using
 * the layers format option, or in POST mode, using the handle attribute of the Query tag. The name
 * of the resulting file can be chosen using the handle attribute of the GetFeature tag. By default,
 * the names of layers concatenated with _ will be used.
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public class DXFOutputFormat extends WFSGetFeatureOutputFormat {

    private static final Logger LOGGER = Logging.getLogger(DXFOutputFormat.class);

    public static final Set<String> formats = new HashSet<>();

    static {
        // list of supported output formats
        formats.add("DXF");
        formats.add("DXF-ZIP");
    }

    public DXFOutputFormat(GeoServer gs) {
        super(gs, formats);
    }

    /** Sets the right extension for the response */
    protected String getExtension(FeatureCollectionResponse response) {
        String outputFormat = response.getOutputFormat().toUpperCase();
        // DXF
        if (outputFormat.equals("DXF")) return "dxf";
        // DXF-ZIP
        return "zip";
    }

    /** Gets current request extension (dxf or zip). */
    public String getDxfExtension(Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);

        String outputFormat = request.getOutputFormat().toUpperCase();
        // DXF
        if (outputFormat.equals("DXF")) return "dxf";
        // DXF-ZIP
        return "zip";
    }

    /** Mime type: application/dxf or application/zip */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/" + getDxfExtension(operation);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    /**
     * Gets output filename. If the handle attribute is defined on the GetFeature tag it will be
     * used, else the name is obtained concatenating layer names with underscore as a separator (up
     * to a maximum name length).
     */
    private String getFileName(Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);

        if (request.getHandle() != null) {
            LOGGER.log(Level.FINE, "Using handle for file name: " + request.getHandle());
            return request.getHandle();
        }

        List<String> names = new ArrayList<>();
        for (Query query : request.getQueries()) {
            addLayerNames(names, query, false);
        }
        String layerNames = StringUtils.join(names.toArray(), '_');
        LOGGER.log(Level.FINE, "Using layer names for file name: " + layerNames);
        if (layerNames.length() > 20) {
            LOGGER.log(
                    Level.WARNING,
                    "Calculated filename too long. Returing a shorter one: "
                            + layerNames.substring(0, 20));
            return layerNames.substring(0, 20);
        }
        return layerNames;
    }

    private void addLayerNames(List<String> names, Query query, boolean toUpper) {
        for (QName name : query.getTypeNames()) {
            String localName = name.getLocalPart();
            if (toUpper) {
                localName = localName.toUpperCase();
            }
            names.add(localName);
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        if (request.getFormatOptions() != null
                && request.getFormatOptions().containsKey("FILENAME")) {
            String fileName = (String) request.getFormatOptions().get("FILENAME");
            if (fileName.contains(".")) {
                return fileName; // includes extension
            } else {
                return fileName + "." + getDxfExtension(operation);
            }
        }
        return getFileName(operation) + '.' + getDxfExtension(operation);
    }

    /**
     * Actually write the given featurecollection as a dxf file to the output stream.
     *
     * @see org.geoserver.wfs.WFSGetFeatureOutputFormat#write(net.opengis.wfs.FeatureCollectionType,
     *     java.io.OutputStream, org.geoserver.platform.Operation)
     */
    @Override
    @SuppressWarnings("PMD.CloseResource") // only wrappers created, out is managed by the servlet
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        // output format (zipped or not)
        String format = getDxfExtension(operation);
        BufferedWriter w = null;
        ZipOutputStream zipStream = null;
        // DXF: use a simple buffered writer
        if (format.equals("dxf")) {
            LOGGER.log(Level.FINE, "Plain DXF output");
            w = new BufferedWriter(new OutputStreamWriter(output));
        } else {
            LOGGER.log(Level.FINE, "Zipped DXF output");
            // DXF-ZIP: use a zip stream wrapped with the buffered writer
            zipStream = new ZipOutputStream(output);
            ZipEntry entry = new ZipEntry(getFileName(operation) + ".dxf");
            zipStream.putNextEntry(entry);
            w = new BufferedWriter(new OutputStreamWriter(zipStream));
        }
        // extract format_options (GET mode)
        GetFeatureRequest gft = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String version = (String) gft.getFormatOptions().get("VERSION");
        String blocks = (String) gft.getFormatOptions().get("ASBLOCKS");
        String colors = (String) gft.getFormatOptions().get("COLORS");
        String ltypes = (String) gft.getFormatOptions().get("LTYPES");
        String[] layers = getLayers(gft);
        if (layers != null) {
            for (int count = 0; count < layers.length; count++) {
                layers[count] = layers[count].toUpperCase();
            }
        }
        String writeAttributes = (String) gft.getFormatOptions().get("WITHATTRIBUTES");
        LOGGER.log(
                Level.FINE,
                "Format options: "
                        + version
                        + "; "
                        + blocks
                        + "; "
                        + colors
                        + "; "
                        + ltypes
                        + "; "
                        + StringUtils.join(layers, ",")
                        + "; "
                        + writeAttributes);
        // get a suitable DXFWriter, for the requested version (null -> get any writer)
        DXFWriter dxfWriter = DXFWriterFinder.getWriter(version, w);

        if (dxfWriter != null) {
            LOGGER.log(Level.INFO, "DXFWriter: " + dxfWriter.getDescription());

            if (layers == null) {
                layers = getLayerNames(gft.getQueries());
            }
            LOGGER.log(Level.FINE, "Layers names: " + StringUtils.join(layers, ","));
            dxfWriter.setOption("layers", layers);
            if (writeAttributes != null) {
                dxfWriter.setOption(
                        "writeattributes", writeAttributes.toLowerCase().equals("true"));
            }
            if (blocks != null && blocks.toLowerCase().equals("true"))
                dxfWriter.setOption("geometryasblock", true);
            // set optional colors
            if (colors != null) {
                try {
                    String[] sColors = colors.split(",");
                    int[] icolors = new int[sColors.length];
                    for (int count = 0; count < sColors.length; count++)
                        icolors[count] = Integer.parseInt(sColors[count]);
                    dxfWriter.setOption("colors", icolors);
                } catch (Throwable t) {
                    LOGGER.log(
                            Level.WARNING,
                            "format option colors ignored by DXFOutputFormat due to a wrong format: "
                                    + t.getMessage());
                }
            }
            // set optional line types
            if (ltypes != null) {
                try {
                    String[] sLTypes = ltypes.split(",");
                    LineType[] ltypesArr = new LineType[sLTypes.length];
                    for (int count = 0; count < sLTypes.length; count++)
                        ltypesArr[count] = LineType.parse(sLTypes[count]);
                    dxfWriter.setOption("linetypes", ltypesArr);

                } catch (Throwable t) {
                    LOGGER.warning(
                            "format option ltypes ignored by DXFOutputFormat due to a wrong format: "
                                    + t.getMessage());
                }
            }

            // do the real job, please
            @SuppressWarnings("unchecked") // only actually works with simple features
            List<SimpleFeatureCollection> feature = (List) featureCollection.getFeature();
            dxfWriter.write(feature, version);

            w.flush();
            if (zipStream != null) {
                zipStream.closeEntry();
                zipStream.close();
            }
            dxfWriter = null;
            zipStream = null;
            w = null;

        } else
            throw new UnsupportedOperationException(
                    "Version " + version + " not supported by dxf output format");
    }

    @SuppressWarnings("unchecked")
    private String[] getLayers(GetFeatureRequest gft) {
        String[] layers = null;
        if (gft.getFormatOptions().get("LAYERS") instanceof String) {
            layers = ((String) gft.getFormatOptions().get("LAYERS")).split(",");
        } else if (gft.getFormatOptions().get("LAYERS") instanceof List) {
            layers =
                    (String[]) ((List) gft.getFormatOptions().get("LAYERS")).toArray(new String[0]);
        }
        return layers;
    }

    /** Gets a list of names for layers, one for each query. */
    private String[] getLayerNames(List<Query> queries) {
        List<String> names = new ArrayList<>();
        for (Query query : queries) {
            addLayerNames(names, query, true);
        }

        return names.toArray(new String[] {});
    }
}
