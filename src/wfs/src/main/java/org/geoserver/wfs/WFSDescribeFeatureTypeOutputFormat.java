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

package org.geoserver.wfs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.SOAPAwareResponse;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.response.WFSResponse;

/**
 * Base class for a response to a WFS DescribeFeatureType operation.
 *
 * <p>The result of a DescribeFeatureType operation is an array of {@link FeatureTypeInfo}.
 * Subclasses are responsible for serializing these instances. See {@link
 * #write(FeatureCollectionType, OutputStream, Operation)}.
 *
 * <p>Subclasses also need declare the mime-type in which the format is encoded.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class WFSDescribeFeatureTypeOutputFormat extends WFSResponse
        implements SOAPAwareResponse {
    /**
     * Constructor which sets the outputFormat.
     *
     * @param outputFormat The well-known name of the format, not <code>null</code>
     */
    public WFSDescribeFeatureTypeOutputFormat(GeoServer gs, String outputFormat) {
        super(gs, FeatureTypeInfo[].class, outputFormat);
    }

    /**
     * Constructor which sets multiple outputFormats.
     *
     * @param outputFormats The well-known name of the format, not <code>null</code>
     */
    public WFSDescribeFeatureTypeOutputFormat(GeoServer gs, Set<String> outputFormats) {
        super(gs, FeatureTypeInfo[].class, outputFormats);
    }

    /**
     * Ensures that the operation being executed is a DescribeFeatureType operation.
     *
     * <p>This method may be extended to add additional checks, it should not be overriden.
     */
    public boolean canHandle(Operation operation) {
        if ("DescribeFeatureType".equalsIgnoreCase(operation.getId())) {
            return true;
        }

        return false;
    }

    @Override
    public String getBodyType() {
        return "xsd:base64";
    }

    /** Calls through to {@link #write(FeatureTypeInfo[], OutputStream, Operation)}. */
    public final void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        write((FeatureTypeInfo[]) value, output, operation);
    }

    /**
     * Serializes the collection of feature type metadata objects in the format declared.
     *
     * @param featureTypeInfos The feature type metadata objects to serialize
     * @param output The output stream to serialize to.
     * @param describeFeatureType The DescribeFeatureType operation descriptor.
     */
    protected abstract void write(
            FeatureTypeInfo[] featureTypeInfos, OutputStream output, Operation describeFeatureType)
            throws IOException;

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        FeatureTypeInfo[] infos = (FeatureTypeInfo[]) value;
        if (infos.length == 1) {
            return infos[0].prefixedName().replace(":", "-") + ".xsd";
        } else {
            return "schema.xsd";
        }
    }
}
