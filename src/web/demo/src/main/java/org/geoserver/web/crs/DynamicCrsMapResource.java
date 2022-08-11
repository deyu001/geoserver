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

package org.geoserver.web.crs;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A wicket resource that acts as a mini WMS to generate a map for a {@link
 * CoordinateReferenceSystem CRS}'s area of validity.
 *
 * <p>This resource expects the following parameters in order to generate the area of validity map:
 *
 * <ul>
 *   <li>WIDTH
 *   <li>HEIGHT
 *   <li>BBOX
 * </ul>
 *
 * @author Gabriel Roldan
 */
public class DynamicCrsMapResource extends AbstractResource {

    private static final long serialVersionUID = 1L;

    private final CoordinateReferenceSystem crs;

    public DynamicCrsMapResource(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    @Override
    protected ResourceResponse newResourceResponse(Attributes attributes) {
        ResourceResponse rsp = new ResourceResponse();
        rsp.setWriteCallback(
                new WriteCallback() {
                    @Override
                    public void writeData(Attributes attributes) throws IOException {
                        IRequestParameters params = attributes.getRequest().getQueryParameters();
                        int width = params.getParameterValue("WIDTH").toInt(400);
                        int height = params.getParameterValue("HEIGHT").toInt(200);
                        String bboxStr = params.getParameterValue("BBOX").toOptionalString();

                        ByteArrayOutputStream output = null;
                        if (bboxStr != null) {

                            try {
                                CRSAreaOfValidityMapBuilder builder =
                                        new CRSAreaOfValidityMapBuilder(width, height);
                                Envelope envelope = parseEnvelope(bboxStr);
                                RenderedImage image = builder.createMapFor(crs, envelope);
                                output = new ByteArrayOutputStream();
                                ImageIO.write(image, "PNG", output);
                            } catch (Exception e) {
                                output = null;
                                e.printStackTrace();
                            }
                        }

                        final byte[] byteArray = output == null ? null : output.toByteArray();
                        if (byteArray != null) {
                            attributes.getResponse().write(byteArray);
                        }
                    }
                });
        return rsp;
    }

    private Envelope parseEnvelope(String bboxStr) {
        String[] split = bboxStr.split(",");
        double minx = Double.valueOf(split[0]);
        double miny = Double.valueOf(split[1]);
        double maxx = Double.valueOf(split[2]);
        double maxy = Double.valueOf(split[3]);
        return new Envelope(minx, maxx, miny, maxy);
    }

    private static class ByteArrayResourceStream extends AbstractResourceStream {

        private static final long serialVersionUID = 1L;

        private final byte[] content;

        public ByteArrayResourceStream(final byte[] content) {
            this.content = content;
        }

        public void setLocale(Locale arg0) {}

        public Bytes length() {
            return Bytes.bytes(content == null ? 0 : content.length);
        }

        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            if (content == null) {
                throw new ResourceStreamNotFoundException();
            }
            return new ByteArrayInputStream(content);
        }

        public String getContentType() {
            return "image/png";
        }

        public void close() throws IOException {}
    }
}
