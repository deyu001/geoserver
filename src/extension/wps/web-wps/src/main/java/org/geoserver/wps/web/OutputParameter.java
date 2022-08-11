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

package org.geoserver.wps.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

/**
 * A single output parameter, along with the chosen output mime type and the output inclusion flag
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class OutputParameter implements Serializable {
    Name processName;

    String paramName;

    String mimeType;

    boolean include = true;

    public OutputParameter(Name processName, String paramName) {
        this.processName = processName;
        this.paramName = paramName;
        this.mimeType = getDefaultMime();
    }

    String getDefaultMime() {
        if (!isComplex()) {
            return null;
        } else {
            return ((ComplexPPIO) getProcessParameterIO().get(0)).getMimeType();
        }
    }

    public List<String> getSupportedMime() {
        List<String> results = new ArrayList<>();
        for (ProcessParameterIO ppio : getProcessParameterIO()) {
            ComplexPPIO cp = (ComplexPPIO) ppio;
            results.add(cp.getMimeType());
        }
        return results;
    }

    public boolean isComplex() {
        List<ProcessParameterIO> ppios = getProcessParameterIO();
        return !ppios.isEmpty() && ppios.get(0) instanceof ComplexPPIO;
    }

    List<ProcessParameterIO> getProcessParameterIO() {
        return ProcessParameterIO.findAll(
                getParameter(), GeoServerApplication.get().getApplicationContext());
    }

    ProcessFactory getProcessFactory() {
        return GeoServerProcessors.createProcessFactory(processName, false);
    }

    Parameter<?> getParameter() {
        return getProcessFactory().getResultInfo(processName, null).get(paramName);
    }
}
