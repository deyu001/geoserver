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

package org.geoserver.wms.animate;

import java.util.Map;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;

/**
 * The Frame Catalog initializes the list of frames to be produced.<br>
 * The catalog takes care of splitting the "avalues" parameter and assigning values to each frame.
 * <br>
 * Notice that the catalog is not delegated to the frame production, it just handles the frames
 * metadata.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S., alessio.fabiani@geo-solutions.it
 * @author Andrea Aime, GeoSolutions S.A.S., andrea.aime@geo-solutions.it
 */
public class FrameCatalog {

    private String parameter;

    private String[] values;

    private GetMapRequest getMapRequest;

    private WebMapService wms;

    private WMS wmsConfiguration;

    /** Default Constructor. */
    private FrameCatalog() {}

    /** Frame Catalog Constructor. */
    public FrameCatalog(GetMapRequest request, WebMapService wms, WMS wmsConfiguration) {
        this();

        this.getMapRequest = request;
        this.wms = wms;
        this.wmsConfiguration = wmsConfiguration;

        Map<String, String> rawKvp = request.getRawKvp();
        String aparam = KvpUtils.caseInsensitiveParam(rawKvp, "aparam", null);
        String avalues = KvpUtils.caseInsensitiveParam(rawKvp, "avalues", null);

        if (aparam != null && !aparam.isEmpty() && avalues != null && !avalues.isEmpty()) {
            this.parameter = aparam;
            this.values = avalues.split("(?<!\\\\)(,)");
        } else {
            dispose();
            throw new RuntimeException(
                    "Missing \"animator\" mandatory params \"aparam\" and \"avalues\".");
        }

        if (this.values.length > this.getWmsConfiguration().getMaxAllowedFrames()) {
            dispose();
            throw new RuntimeException(
                    "Request too long; reached the maximum allowed number of frames.");
        }
    }

    /** @return the parameter */
    public String getParameter() {
        return parameter;
    }

    /** @return the values */
    public String[] getValues() {
        return values;
    }

    /** @return the getMapRequest */
    public GetMapRequest getGetMapRequest() {
        return getMapRequest;
    }

    /** @return the wms */
    public WebMapService getWms() {
        return wms;
    }

    /** @return the wmsConfiguration */
    public WMS getWmsConfiguration() {
        return wmsConfiguration;
    }

    /** Creates Frames visitors. Still not producing any image here. */
    void getFrames(FrameCatalogVisitor visitor) {
        for (String value : values) {
            visitor.visit(
                    this.getMapRequest, this.wms, this.wmsConfiguration, this.parameter, value);
        }
    }

    /** Dispose the catalog, removing all stored informations. */
    void dispose() {
        this.parameter = null;
        this.values = null;
        this.getMapRequest = null;
    }
}
