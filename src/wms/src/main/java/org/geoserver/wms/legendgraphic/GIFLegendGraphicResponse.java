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

package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.image.ImageWorker;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes a {@link BufferedImageLegendGraphic} to the image/gif MIME Type
 *
 * @author groldan
 */
public class GIFLegendGraphicResponse extends AbstractGetLegendGraphicResponse {

    public GIFLegendGraphicResponse() {
        super(BufferedImageLegendGraphic.class, GIFLegendOutputFormat.MIME_TYPE);
    }

    /**
     * @return {@code image/gif}
     * @see Response#getMimeType(Object, Operation)
     */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, value);
        return GIFLegendOutputFormat.MIME_TYPE;
    }

    /**
     * @param legend a {@link BufferedImageLegendGraphic}
     * @param output image destination
     * @param operation Operation descriptor the {@code legend} was produced for
     * @see Response#write(Object, OutputStream, Operation)
     */
    @Override
    public void write(Object legend, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        Assert.isInstanceOf(BufferedImageLegendGraphic.class, legend);

        BufferedImage legendGraphic = (BufferedImage) ((LegendGraphic) legend).getLegend();

        RenderedImage forcedIndexed8Bitmask = ImageUtils.forceIndexed8Bitmask(legendGraphic, null);
        ImageWorker imageWorker = new ImageWorker(forcedIndexed8Bitmask);
        imageWorker.writeGIF(output, "LZW", 0.75f);
    }
}
