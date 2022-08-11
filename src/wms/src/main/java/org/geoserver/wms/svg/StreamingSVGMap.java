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

package org.geoserver.wms.svg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;

/**
 * Streaming SVG encoder (does not support styling)
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class StreamingSVGMap extends WebMap {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map");

    /** the XML and SVG header */
    private static final String SVG_HEADER =
            "<?xml version=\"1.0\" standalone=\"no\"?>\n\t"
                    + "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" \n\tstroke=\"green\" \n\tfill=\"none\" \n\tstroke-width=\"0.1%\"\n\tstroke-linecap=\"round\"\n\tstroke-linejoin=\"round\"\n\twidth=\"_width_\" \n\theight=\"_height_\" \n\tviewBox=\"_viewBox_\" \n\tpreserveAspectRatio=\"xMidYMid meet\">\n";

    /** the SVG closing element */
    private static final String SVG_FOOTER = "</svg>\n";

    private SVGWriter writer;

    /** Creates a new EncodeSVG object. */
    public StreamingSVGMap(WMSMapContent mapContent) {
        super(mapContent);
    }

    public void encode(final OutputStream out) throws IOException {
        Envelope env = this.mapContent.getRenderingArea();
        this.writer = new SVGWriter(out, mapContent.getRenderingArea());
        writer.setMinCoordDistance(env.getWidth() / 1000);

        long t = System.currentTimeMillis();

        writeHeader();

        writeLayers();

        writer.write(SVG_FOOTER);

        this.writer.flush();
        t = System.currentTimeMillis() - t;
        LOGGER.info("SVG generated in " + t + " ms");
    }

    public String createViewBox() {
        Envelope referenceSpace = mapContent.getRenderingArea();
        String viewBox =
                writer.getX(referenceSpace.getMinX())
                        + " "
                        + (writer.getY(referenceSpace.getMinY()) - referenceSpace.getHeight())
                        + " "
                        + referenceSpace.getWidth()
                        + " "
                        + referenceSpace.getHeight();

        return viewBox;
    }

    private void writeHeader() throws IOException {
        // TODO: this does not write out the doctype definition, there should be
        // a configuration option wether to include it or not.
        String viewBox = createViewBox();
        String header = SVG_HEADER.replaceAll("_viewBox_", viewBox);
        header = header.replaceAll("_width_", String.valueOf(mapContent.getMapWidth()));
        header = header.replaceAll("_height_", String.valueOf(mapContent.getMapHeight()));
        writer.write(header);
    }

    private void writeDefs(SimpleFeatureType layer) throws IOException {
        GeometryDescriptor gtype = layer.getGeometryDescriptor();
        Class geometryClass = gtype.getType().getBinding();

        if ((geometryClass == MultiPoint.class) || (geometryClass == Point.class)) {
            writePointDefs();
        }
    }

    private void writePointDefs() throws IOException {
        writer.write(
                "<defs>\n\t<circle id='point' cx='0' cy='0' r='0.25%' fill='blue'/>\n</defs>\n");
    }

    /** @task TODO: respect layer filtering given by their Styles */
    private void writeLayers() throws IOException {
        List<Layer> layers = mapContent.layers();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        for (Layer layer : layers) {
            SimpleFeatureIterator featureReader = null;
            SimpleFeatureSource fSource;
            fSource = (SimpleFeatureSource) layer.getFeatureSource();
            SimpleFeatureType schema = fSource.getSchema();

            try {
                String defaultGeometry = schema.getGeometryDescriptor().getName().getLocalPart();
                ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
                BBOX bboxFilter = ff.bbox(ff.property(defaultGeometry), renderingArea);

                Query bboxQuery = new Query(schema.getTypeName(), bboxFilter);
                Query definitionQuery = layer.getQuery();
                Query finalQuery =
                        new Query(
                                DataUtilities.mixQueries(definitionQuery, bboxQuery, "svgEncoder"));
                finalQuery.setHints(definitionQuery.getHints());
                finalQuery.setSortBy(definitionQuery.getSortBy());
                finalQuery.setStartIndex(definitionQuery.getStartIndex());

                LOGGER.fine("obtaining FeatureReader for " + schema.getTypeName());
                featureReader = fSource.getFeatures(finalQuery).features();
                LOGGER.fine("got FeatureReader, now writing");

                String groupId = null;
                String styleName = null;

                groupId = schema.getTypeName();

                styleName = layer.getStyle().getName();

                writer.write("<g id=\"" + groupId + "\"");

                if (!styleName.startsWith("#")) {
                    writer.write(" class=\"" + styleName + "\"");
                }

                writer.write(">\n");

                writeDefs(schema);

                writer.writeFeatures(fSource.getSchema(), featureReader, styleName);
                writer.write("</g>\n");
            } catch (IOException ex) {
                throw ex;
            } catch (Throwable t) {
                LOGGER.warning("UNCAUGHT exception: " + t.getMessage());

                IOException ioe = new IOException("UNCAUGHT exception: " + t.getMessage());
                ioe.setStackTrace(t.getStackTrace());
                throw ioe;
            } finally {
                if (featureReader != null) {
                    featureReader.close();
                }
            }
        }
    }
}
