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

package org.geoserver.wms.staticRasterStore;

import java.util.HashMap;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;

/** Format class for the static raster reader. */
final class StaticRasterFormat extends AbstractGridFormat implements Format {

    // add filtering capabilities
    public static final ParameterDescriptor<Filter> FILTER =
            new DefaultParameterDescriptor<>("Filter", Filter.class, null, null);

    StaticRasterFormat() {
        setInfo();
        // reader capabilities
        readParameters =
                new ParameterGroup(
                        new DefaultParameterDescriptorGroup(
                                mInfo,
                                new GeneralParameterDescriptor[] {
                                    AbstractGridFormat.READ_GRIDGEOMETRY2D, FILTER
                                }));
    }

    private void setInfo() {
        HashMap<String, String> info = new HashMap<>();
        info.put("name", "StaticRaster");
        info.put("description", "Static raster store");
        info.put("vendor", "Geotools");
        info.put("docURL", "http://geotools.org/");
        info.put("version", "1.0");
        mInfo = info;
    }

    @Override
    public StaticRasterReader getReader(Object source) {
        // we just create the reader with no hints
        return getReader(source, null);
    }

    @Override
    public StaticRasterReader getReader(Object source, Hints hints) {
        return new StaticRasterReader(source);
    }

    @Override
    public boolean accepts(Object input, Hints hints) {
        // we don't need anything here
        return false;
    }

    @Override
    public ParameterValueGroup getReadParameters() {
        // this will return the read parameters we setup in the constructor
        return super.getReadParameters();
    }

    @Override
    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public GridCoverageWriter getWriter(Object destination, Hints hints) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public GridCoverageWriter getWriter(Object destination) {
        throw new UnsupportedOperationException("Operation not supported.");
    }
}
