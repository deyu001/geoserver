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

package org.geoserver.rest.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

/** Base controller for catalog info requests */
public abstract class AbstractCatalogController extends RestBaseController {

    protected final Catalog catalog;
    protected final GeoServerDataDirectory dataDir;
    protected final List<String> validImageFileExtensions;

    public AbstractCatalogController(Catalog catalog) {
        super();
        this.catalog = catalog;
        this.dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        this.validImageFileExtensions = Arrays.asList("svg", "png", "jpg");
    }

    /**
     * Uses messages as a template to update resource.
     *
     * @param message Possibly incomplete ResourceInfo used to update resource
     * @param resource Original resource (to be saved in catalog after modification)
     */
    protected void calculateOptionalFields(
            ResourceInfo message, ResourceInfo resource, String calculate) {
        List<String> fieldsToCalculate;
        if (calculate == null || calculate.isEmpty()) {
            boolean changedProjection =
                    message.getSRS() == null || !message.getSRS().equals(resource.getSRS());
            boolean changedProjectionPolicy =
                    message.getProjectionPolicy() == null
                            || !message.getProjectionPolicy()
                                    .equals(resource.getProjectionPolicy());
            boolean changedNativeBounds =
                    message.getNativeBoundingBox() == null
                            || !message.getNativeBoundingBox()
                                    .equals(resource.getNativeBoundingBox());
            boolean changedLatLonBounds =
                    message.getLatLonBoundingBox() == null
                            || !message.getLatLonBoundingBox()
                                    .equals(resource.getLatLonBoundingBox());
            boolean changedNativeInterpretation = changedProjectionPolicy || changedProjection;
            fieldsToCalculate = new ArrayList<>();
            if (changedNativeInterpretation && !changedNativeBounds) {
                fieldsToCalculate.add("nativebbox");
            }
            if ((changedNativeInterpretation || changedNativeBounds) && !changedLatLonBounds) {
                fieldsToCalculate.add("latlonbbox");
            }
        } else {
            fieldsToCalculate = Arrays.asList(calculate.toLowerCase().split(","));
        }

        if (fieldsToCalculate.contains("nativebbox")) {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            try {
                message.setNativeBoundingBox(builder.getNativeBounds(message));
            } catch (IOException e) {
                String errorMessage = "Error while calculating native bounds for layer: " + message;
                throw new RestException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }
        if (fieldsToCalculate.contains("latlonbbox")) {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            try {
                message.setLatLonBoundingBox(
                        builder.getLatLonBounds(
                                message.getNativeBoundingBox(), resolveCRS(message.getSRS())));
            } catch (IOException e) {
                String errorMessage =
                        "Error while calculating lat/lon bounds for featuretype: " + message;
                throw new RestException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    private CoordinateReferenceSystem resolveCRS(String srs) {
        if (srs == null) {
            return null;
        }
        try {
            return CRS.decode(srs);
        } catch (Exception e) {
            throw new RuntimeException(
                    "This is unexpected, the layer seems to be mis-configured", e);
        }
    }

    /** Determines if the current user is authenticated as full administrator. */
    protected boolean isAuthenticatedAsAdmin() {
        return SecurityContextHolder.getContext() != null
                && GeoServerExtensions.bean(GeoServerSecurityManager.class)
                        .checkAuthenticationForAdminRole();
    }

    /**
     * Validates the current user can edit the resource (full admin required if workspaceName is
     * null)
     */
    protected void checkFullAdminRequired(String workspaceName) {
        // global workspaces/styles can only be edited by a full admin
        if (workspaceName == null && !isAuthenticatedAsAdmin()) {
            throw new RestException(
                    "Cannot edit global resource , full admin credentials required",
                    HttpStatus.METHOD_NOT_ALLOWED);
        }
    }
}
