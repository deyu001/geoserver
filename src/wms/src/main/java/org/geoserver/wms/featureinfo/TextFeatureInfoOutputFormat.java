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

package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.data.util.TemporalUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

/**
 * Generates a FeatureInfoResponse of type text. This simply reports the attributes of the feature
 * requested as a text string. This class just performs the writeTo, the GetFeatureInfoDelegate and
 * abstract feature info class handle the rest.
 *
 * @author James Macgill, PSU
 * @version $Id$
 */
public class TextFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private WMS wms;

    public TextFeatureInfoOutputFormat(final WMS wms) {
        super("text/plain");
        this.wms = wms;
    }

    /**
     * Writes the feature information to the client in text/plain format.
     *
     * @see GetFeatureInfoOutputFormat#write
     */
    @SuppressWarnings("PMD.CloseResource") // just a wrapper, actual output managed by servlet
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        Charset charSet = wms.getCharSet();
        OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        // getRequest().getGeoServer().getCharSet());
        PrintWriter writer = new PrintWriter(osw);

        // DJB: this is to limit the number of features read - as per the spec
        // 7.3.3.7 FEATURE_COUNT
        int featuresPrinted = 0; // how many features we've actually printed
        // so far!

        int maxfeatures = request.getFeatureCount(); // will default to 1
        // if not specified
        // in the request

        FeatureIterator reader = null;

        try {
            final List collections = results.getFeature();
            FeatureCollection fr;
            SimpleFeature f;

            SimpleFeatureType schema;
            List<AttributeDescriptor> types;

            // for each layer queried
            for (Object collection : collections) {
                fr = (FeatureCollection) collection;
                reader = fr.features();

                boolean startFeat = true;
                while (reader.hasNext()) {
                    Feature feature = reader.next();

                    if (startFeat) {
                        writer.println(
                                "Results for FeatureType '" + fr.getSchema().getName() + "':");
                        startFeat = false;
                    }

                    if (featuresPrinted < maxfeatures) {
                        writer.println("--------------------------------------------");

                        if (feature instanceof SimpleFeature) {
                            f = (SimpleFeature) feature;
                            schema = f.getType();
                            types = schema.getAttributeDescriptors();

                            for (AttributeDescriptor descriptor : types) {
                                final Name name = descriptor.getName();
                                final Class<?> binding = descriptor.getType().getBinding();
                                if (Geometry.class.isAssignableFrom(binding)) {
                                    // writer.println(types[j].getName() + " =
                                    // [GEOMETRY]");

                                    // DJB: changed this to print out WKT - its very
                                    // nice for users
                                    // Geometry g = (Geometry)
                                    // f.getAttribute(types[j].getName());
                                    // writer.println(types[j].getName() + " =
                                    // [GEOMETRY] = "+g.toText() );

                                    // DJB: decided that all the geometry info was
                                    // too much - they should use GML version if
                                    // they want those details
                                    Geometry g = (Geometry) f.getAttribute(name);
                                    if (g != null) {
                                        writer.println(
                                                name
                                                        + " = [GEOMETRY ("
                                                        + g.getGeometryType()
                                                        + ") with "
                                                        + g.getNumPoints()
                                                        + " points]");
                                    } else {
                                        // GEOS-6829
                                        writer.println(name + " = null");
                                    }
                                } else if (Date.class.isAssignableFrom(binding)
                                        && TemporalUtils.isDateTimeFormatEnabled()) {
                                    // Temporal types print handling
                                    String printValue =
                                            TemporalUtils.printDate((Date) f.getAttribute(name));
                                    writer.println(name + " = " + printValue);
                                } else {
                                    writer.println(name + " = " + f.getAttribute(name));
                                }
                            }

                        } else {
                            writer.println(feature.toString());
                        }
                    }

                    writer.println("--------------------------------------------");
                    featuresPrinted++;
                }
            }
        } catch (Exception ife) {
            LOGGER.log(Level.WARNING, "Error generating getFeaturInfo, HTML format", ife);
            writer.println("Unable to generate information " + ife);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        if (featuresPrinted == 0) {
            writer.println("no features were found");
        }

        writer.flush();
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
