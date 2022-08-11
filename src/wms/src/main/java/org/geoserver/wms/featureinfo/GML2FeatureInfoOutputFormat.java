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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.xml.GML2OutputFormat;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.gml2.bindings.GML2EncodingUtils;

/**
 * A GetFeatureInfo response handler specialized in producing GML data for a GetFeatureInfo request.
 *
 * <p>This class does not deals directly with GML encoding. Instead, it works by taking the
 * FeatureResults produced in <code>execute()</code> and constructs a <code>GetFeaturesResult</code>
 * wich is passed to a <code>GML2FeatureResponseDelegate</code>, as if it where the result of a
 * GetFeature WFS request.
 *
 * @author Gabriel Roldan
 */
public class GML2FeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {
    /**
     * The MIME type of the format this response produces: <code>"application/vnd.ogc.gml"</code>
     */
    public static final String FORMAT = "application/vnd.ogc.gml";

    private WMS wms;

    /** Default constructor, sets up the supported output format string. */
    public GML2FeatureInfoOutputFormat(final WMS wms) {
        super(FORMAT);
        this.wms = wms;
    }

    protected GML2FeatureInfoOutputFormat(WMS wms, String format) {
        super(format);
        this.wms = wms;
    }

    /**
     * Takes the <code>FeatureResult</code>s generated by the <code>execute</code> method in the
     * superclass and constructs a <code>GetFeaturesResult</code> wich is passed to a <code>
     * GML2FeatureResponseDelegate</code>.
     *
     * @see AbstractFeatureInfoResponse#writeTo(OutputStream)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest fInfoReq, OutputStream out)
            throws ServiceException, IOException {

        // the 'response' object we'll pass to our OutputFormat
        FeatureCollectionType features = WfsFactory.eINSTANCE.createFeatureCollectionType();

        // the 'request' object we'll pass to our OutputFormat
        GetFeatureType gfreq = WfsFactory.eINSTANCE.createGetFeatureType();
        gfreq.setBaseUrl(fInfoReq.getBaseUrl());

        for (Object o : results.getFeature()) {
            FeatureCollection fc = (FeatureCollection) o;
            features.getFeature().add(fc);

            QueryType qt = WfsFactory.eINSTANCE.createQueryType();
            String crs = GML2EncodingUtils.epsgCode(fc.getSchema().getCoordinateReferenceSystem());
            if (crs != null) {
                final String srsName = "EPSG:" + crs;
                try {
                    qt.setSrsName(new URI(srsName));
                } catch (URISyntaxException e) {
                    throw new ServiceException(
                            "Unable to determite coordinate system for featureType "
                                    + fc.getSchema().getName()
                                    + ".  Schema told us '"
                                    + srsName
                                    + "'",
                            e);
                }
            }
            gfreq.getQuery().add(qt);
        }

        // this is a dummy wrapper around our 'request' object so that the new Dispatcher will
        // accept it.
        Service serviceDesc = new Service("wms", null, null, Collections.emptyList());
        Operation opDescriptor = new Operation("", serviceDesc, null, new Object[] {gfreq});

        final GeoServer gs = wms.getGeoServer();
        GML2OutputFormat format = new GML2OutputFormat(gs);
        format.write(features, out, opDescriptor);
    }
}
