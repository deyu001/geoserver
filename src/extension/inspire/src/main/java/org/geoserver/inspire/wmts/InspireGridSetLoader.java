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

package org.geoserver.inspire.wmts;

import org.geoserver.gwc.GWC;
import org.geoserver.platform.ContextLoadedEvent;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.springframework.context.ApplicationListener;

/** Loads the inspire grid set and mark it as non editable by the user. */
public class InspireGridSetLoader implements ApplicationListener<ContextLoadedEvent> {

    public static final String INSPIRE_GRID_SET_NAME = "InspireCRS84Quad";

    @Override
    public synchronized void onApplicationEvent(ContextLoadedEvent event) {
        GWC gwc = GWC.get();
        // this grid set should not be editable by the user
        gwc.addEmbeddedGridSet(INSPIRE_GRID_SET_NAME);
        GridSet gridSet = gwc.getGridSetBroker().get(INSPIRE_GRID_SET_NAME);
        if (gridSet != null) {
            // this grid set already exists
            return;
        }
        // the grid set resolutions
        double[] resolutions =
                new double[] {
                    0.703125,
                    0.3515625,
                    0.17578125,
                    0.087890625,
                    0.0439453125,
                    0.02197265625,
                    0.010986328125,
                    0.0054931640625,
                    0.00274658203125,
                    0.001373291015625,
                    6.866455078125E-4,
                    3.433227539062E-4,
                    1.716613769531E-4,
                    8.58306884766E-5,
                    4.29153442383E-5,
                    2.14576721191E-5,
                    1.07288360596E-5,
                    5.3644180298E-6
                };
        // the grid sets scale names
        String[] scaleNames =
                new String[] {
                    "InspireCRS84Quad:0", "InspireCRS84Quad:1", "InspireCRS84Quad:2",
                            "InspireCRS84Quad:3", "InspireCRS84Quad:4",
                    "InspireCRS84Quad:5", "InspireCRS84Quad:6", "InspireCRS84Quad:7",
                            "InspireCRS84Quad:8", "InspireCRS84Quad:9",
                    "InspireCRS84Quad:10", "InspireCRS84Quad:11", "InspireCRS84Quad:12",
                            "InspireCRS84Quad:13", "InspireCRS84Quad:14",
                    "InspireCRS84Quad:15", "InspireCRS84Quad:16", "InspireCRS84Quad:17"
                };
        // creating thee grid set
        gridSet =
                GridSetFactory.createGridSet(
                        INSPIRE_GRID_SET_NAME,
                        SRS.getEPSG4326(),
                        BoundingBox.WORLD4326,
                        false,
                        resolutions,
                        null,
                        111319.49079327358,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        scaleNames,
                        256,
                        256,
                        false);
        // set a proper description
        gridSet.setDescription(
                "Every layer offered by a INSPIRE WMTS should use the InspireCRS84Quad Matrix Set");
        try {
            // add the grid set
            gwc.addGridSet(gridSet);
        } catch (Exception exception) {
            throw new RuntimeException("Error adding grid set InspireCRS84Quad.", exception);
        }
    }
}
