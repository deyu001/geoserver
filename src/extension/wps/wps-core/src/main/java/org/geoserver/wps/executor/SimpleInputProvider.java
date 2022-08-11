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

package org.geoserver.wps.executor;

import java.io.ByteArrayInputStream;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.ppio.RawDataPPIO;
import org.geoserver.wps.process.ByteArrayRawData;
import org.geoserver.wps.process.StringRawData;
import org.geotools.util.Base64;
import org.opengis.util.ProgressListener;

/**
 * Performs lazy parsing of a specific input
 *
 * @author Andrea Aime - GeoSolutions
 */
class SimpleInputProvider extends AbstractInputProvider {

    public SimpleInputProvider(InputType input, ProcessParameterIO ppio) {
        super(input, ppio);
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        // actual data, figure out which type
        DataType data = input.getData();
        Object result = null;

        if (data.getLiteralData() != null) {
            LiteralDataType literal = data.getLiteralData();
            result = ((LiteralPPIO) ppio).decode(literal.getValue());
        } else if (data.getComplexData() != null) {
            ComplexDataType complex = data.getComplexData();
            if (ppio instanceof RawDataPPIO) {
                Object inputData = complex.getData().get(0);
                String encoding = complex.getEncoding();
                byte[] decoded = null;
                if (encoding != null) {
                    if ("base64".equals(encoding)) {
                        String input = inputData.toString();
                        decoded = Base64.decode(input);
                    } else {
                        throw new WPSException("Unsupported encoding " + encoding);
                    }
                }

                if (decoded != null) {
                    return new ByteArrayRawData(decoded, complex.getMimeType());
                } else {
                    return new StringRawData(inputData.toString(), complex.getMimeType());
                }

            } else {
                Object inputData = complex.getData().get(0);
                String encoding = complex.getEncoding();
                byte[] decoded = null;
                if (encoding != null) {
                    if ("base64".equals(encoding)) {
                        String input = inputData.toString();
                        decoded = Base64.decode(input);
                    } else {
                        throw new WPSException("Unsupported encoding " + encoding);
                    }
                }

                if (decoded != null) {
                    result = ((ComplexPPIO) ppio).decode(new ByteArrayInputStream(decoded));
                } else {
                    result = ((ComplexPPIO) ppio).decode(inputData);
                }
            }
        } else if (data.getBoundingBoxData() != null) {
            result = ((BoundingBoxPPIO) ppio).decode(data.getBoundingBoxData());
        }

        return result;
    }

    @Override
    public int longStepCount() {
        return 0;
    }
}
