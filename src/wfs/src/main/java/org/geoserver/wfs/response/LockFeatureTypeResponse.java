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

package org.geoserver.wfs.response;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.LockFeatureType;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.util.Version;
import org.geotools.xsd.Encoder;
import org.opengis.filter.identity.FeatureId;

public class LockFeatureTypeResponse extends WFSResponse {

    Catalog catalog;
    WFSConfiguration configuration;

    public LockFeatureTypeResponse(GeoServer gs, WFSConfiguration configuration) {
        super(gs, LockFeatureResponseType.class);

        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        WFSInfo wfs = getInfo();

        LockFeatureResponseType lockResponse = (LockFeatureResponseType) value;

        if (new Version("1.1.0").equals(operation.getService().getVersion())) {
            write1_1(lockResponse, output, operation);

            return;
        }

        String indent = wfs.isVerbose() ? "   " : "";
        Charset charset = Charset.forName(wfs.getGeoServer().getSettings().getCharset());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, charset));

        LockFeatureType lft = (LockFeatureType) operation.getParameters()[0];

        // TODO: get rid of this hardcoding, and make a common utility to get all
        // these namespace imports, as everyone is using them, and changes should
        // go through to all the operations.
        writer.write("<?xml version=\"1.0\" encoding=\"" + charset.name() + "\"?>");
        writer.write("<WFS_LockFeatureResponse " + "\n");
        writer.write(indent + "xmlns=\"http://www.opengis.net/wfs\" " + "\n");
        writer.write(indent + "xmlns:ogc=\"http://www.opengis.net/ogc\" " + "\n");

        writer.write(indent + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + "\n");
        writer.write(indent + "xsi:schemaLocation=\"http://www.opengis.net/wfs ");
        writer.write(buildSchemaURL(lft.getBaseUrl(), "wfs/1.0.0/WFS-transaction.xsd"));
        writer.write("\">" + "\n");

        writer.write(indent + "<LockId>" + lockResponse.getLockId() + "</LockId>" + "\n");

        List featuresLocked = null;

        if (lockResponse.getFeaturesLocked() != null) {
            featuresLocked = lockResponse.getFeaturesLocked().getFeatureId();
        }

        List featuresNotLocked = null;

        if (lockResponse.getFeaturesNotLocked() != null) {
            featuresNotLocked = lockResponse.getFeaturesNotLocked().getFeatureId();
        }

        if ((featuresLocked != null) && !featuresLocked.isEmpty()) {
            writer.write(indent + "<FeaturesLocked>" + "\n");

            for (Object o : featuresLocked) {
                writer.write(indent + indent);

                FeatureId featureId = (FeatureId) o;
                writer.write("<ogc:FeatureId fid=\"" + featureId + "\"/>" + "\n");
            }

            writer.write(indent + "</FeaturesLocked>" + "\n");
        }

        if ((featuresNotLocked != null) && !featuresNotLocked.isEmpty()) {
            writer.write("<FeaturesNotLocked>" + "\n");

            for (Object o : featuresNotLocked) {
                writer.write(indent + indent);

                FeatureId featureId = (FeatureId) o;
                writer.write("<ogc:FeatureId fid=\"" + featureId + "\"/>" + "\n");
            }

            writer.write("</FeaturesNotLocked>" + "\n");
        }

        writer.write("</WFS_LockFeatureResponse>");
        writer.flush();
    }

    void write1_1(LockFeatureResponseType lockResponse, OutputStream output, Operation operation)
            throws IOException {
        XSDSchema result;
        try {
            result = configuration.getXSD().getSchema();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Encoder encoder = new Encoder(configuration, result);
        encoder.setEncoding(Charset.forName(getInfo().getGeoServer().getSettings().getCharset()));

        LockFeatureType req = (LockFeatureType) operation.getParameters()[0];

        encoder.setSchemaLocation(
                org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                buildSchemaURL(req.getBaseUrl(), "schemas/wfs/1.1.0/wfs.xsd"));

        encoder.encode(lockResponse, org.geoserver.wfs.xml.v1_1_0.WFS.LOCKFEATURERESPONSE, output);
        output.flush();
    }
}
