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


package org.geoserver.netcdfout;

import java.lang.reflect.Method;
import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import ucar.nc2.jni.netcdf.Nc4prototypes;

public class NetCDFOutStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-netcdf-out";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("GridCoverage2DWriter");
    }

    @Override
    public String getName() {
        return "WCS NetCDF output Module";
    }

    @Override
    public Optional<String> getVersion() {
        Version v = GeoTools.getVersion(NetCDFUtilities.class);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(v.toString());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String Nc4Version() {
        try {
            // This reflection code is to deal with JNA being an optional jar.
            // Its the same as;
            // Nc4prototypes nc4 = (Nc4prototypes) Native.loadLibrary("netcdf",
            // Nc4prototypes.class);
            // return nc4.nc_inq_libvers();
            Class<?> jnaNativeClass = Class.forName("com.sun.jna.Native");
            Method loadLibraryMethod =
                    jnaNativeClass.getMethod("loadLibrary", String.class, Class.class);
            Object nc4 = loadLibraryMethod.invoke(null, "netcdf", Nc4prototypes.class);

            Method nc_inq_libversMethod = Nc4prototypes.class.getMethod("nc_inq_libvers");
            String version = (String) nc_inq_libversMethod.invoke(nc4);

            return version;
        } catch (Exception e) {
            return "unavailable (" + e.getClass() + ":" + e.getMessage() + ")";
        }
    }

    @Override
    public Optional<String> getMessage() {
        String message = "NETCDF-4 Binary Available: " + NetCDFUtilities.isNC4CAvailable();
        message += "\nNc4prototypes Version: " + GeoTools.getVersion(Nc4prototypes.class);
        if (NetCDFUtilities.isNC4CAvailable()) {
            message += "\nc_inq_libvers: " + Nc4Version();
        }
        return Optional.ofNullable(message);
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable("");
    }
}
