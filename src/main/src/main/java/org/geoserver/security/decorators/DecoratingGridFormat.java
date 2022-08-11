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

package org.geoserver.security.decorators;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Delegates every method to the delegate grid format. Subclasses will override selected methods to
 * perform their "decoration" job
 *
 * @author Andrea Aime
 */
public abstract class DecoratingGridFormat implements Format {

    AbstractGridFormat delegate;

    public DecoratingGridFormat(AbstractGridFormat delegate) {
        this.delegate = delegate;
    }

    public boolean accepts(Object source, Hints hints) {
        return delegate.accepts(source, hints);
    }

    public boolean accepts(Object source) {
        return delegate.accepts(source);
    }

    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        return delegate.getDefaultImageIOWriteParameters();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public String getDocURL() {
        return delegate.getDocURL();
    }

    public String getName() {
        return delegate.getName();
    }

    public GridCoverage2DReader getReader(Object source, Hints hints) {
        return delegate.getReader(source, hints);
    }

    public GridCoverage2DReader getReader(Object source) {
        return delegate.getReader(source);
    }

    public ParameterValueGroup getReadParameters() {
        return delegate.getReadParameters();
    }

    public String getVendor() {
        return delegate.getVendor();
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public ParameterValueGroup getWriteParameters() {
        return delegate.getWriteParameters();
    }

    public GridCoverageWriter getWriter(Object destination, Hints hints) {
        return delegate.getWriter(destination, hints);
    }

    public GridCoverageWriter getWriter(Object destination) {
        return delegate.getWriter(destination);
    }
}
