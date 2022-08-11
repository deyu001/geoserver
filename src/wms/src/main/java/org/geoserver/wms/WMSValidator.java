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

package org.geoserver.wms;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.AbstractCatalogValidator;
import org.geotools.util.factory.GeoTools;

/**
 * Configuration validator for Web Map Service.
 *
 * @author David Winslow, OpenGeo
 */
public class WMSValidator extends AbstractCatalogValidator {

    public void validate(LayerInfo lyr, boolean isNew) {
        if (lyr.isEnabled() == false) {
            // short-circuit - for disabled layers we don't need to validate
            // anything because it won't cause service exceptions for anyone
            return;
        }

        if (lyr.getResource() == null
                || ((lyr.getResource().getSRS() == null
                                || lyr.getResource().getLatLonBoundingBox() == null)
                        && WMS.isWmsExposable(lyr))) {
            throw new RuntimeException("Layer's resource is not fully configured");
        }

        // Resource-dependent checks
        if (lyr.getType() == PublishedType.RASTER) {
            if (!(lyr.getResource() instanceof CoverageInfo))
                throw new RuntimeException(
                        "Layer with type RASTER doesn't have a coverage associated");
            CoverageInfo cvinfo = (CoverageInfo) lyr.getResource();
            try {
                cvinfo.getCatalog()
                        .getResourcePool()
                        .getGridCoverageReader(cvinfo, GeoTools.getDefaultHints());
            } catch (Throwable t) {
                throw new RuntimeException("Couldn't connect to raster layer's resource");
            }
        } else if (lyr.getType() == PublishedType.VECTOR) {
            if (!(lyr.getResource() instanceof FeatureTypeInfo))
                throw new RuntimeException(
                        "Layer with type VECTOR doesn't have a featuretype associated");
        } else if (lyr.getType()
                == PublishedType.WMTS) { // this is mostly to avoid throwing a not RASTER nor VECTOR
            // exception
            if (!(lyr.getResource() instanceof WMTSLayerInfo)) {
                throw new RuntimeException("WMTS Layer doesn't have the correct resource");
            }
        } else if (lyr.getType()
                == PublishedType.WMS) { // this is mostly to avoid throwing a not RASTER nor VECTOR
            // exception
            if (!(lyr.getResource() instanceof WMSLayerInfo)) {
                throw new RuntimeException("WMS Layer doesn't have the correct resource");
            }
        } else throw new RuntimeException("Layer is neither RASTER nor VECTOR type");

        // Style-dependent checks
        if ((lyr.getDefaultStyle() == null || lyr.getStyles().contains(null))
                && WMS.isWmsExposable(lyr)) {
            throw new RuntimeException("Layer has null styles!");
        }
    }
}
