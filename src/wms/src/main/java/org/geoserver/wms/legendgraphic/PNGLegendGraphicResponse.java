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
import java.awt.image.DirectColorModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.media.jai.PlanarImage;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.image.ImageWorker;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes a {@link BufferedImageLegendGraphic} to the image/png MIME Type
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class PNGLegendGraphicResponse extends AbstractGetLegendGraphicResponse {

    public PNGLegendGraphicResponse() {
        super(BufferedImageLegendGraphic.class, PNGLegendOutputFormat.MIME_TYPE);
    }

    /**
     * @param legend a {@link BufferedImageLegendGraphic}
     * @param output png image destination
     * @see GetLegendGraphicProducer#writeTo(java.io.OutputStream)
     */
    @Override
    public void write(Object legend, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, legend);

        BufferedImage image = (BufferedImage) ((LegendGraphic) legend).getLegend();
        // /////////////////////////////////////////////////////////////////
        //
        // Reformatting this image for png
        //
        // /////////////////////////////////////////////////////////////////
        final MemoryCacheImageOutputStream memOutStream = new MemoryCacheImageOutputStream(output);
        final ImageWorker worker = new ImageWorker(image);
        final PlanarImage finalImage =
                (image.getColorModel() instanceof DirectColorModel)
                        ? worker.forceComponentColorModel().getPlanarImage()
                        : worker.getPlanarImage();

        // /////////////////////////////////////////////////////////////////
        //
        // Getting a writer
        //
        // /////////////////////////////////////////////////////////////////
        final Iterator<ImageWriter> it;
        it = ImageIO.getImageWritersByMIMEType(PNGLegendOutputFormat.MIME_TYPE);
        ImageWriter writer = null;

        if (!it.hasNext()) {
            throw new IllegalStateException("No PNG ImageWriter found");
        } else {
            writer = it.next();
        }

        // /////////////////////////////////////////////////////////////////
        //
        // Compression is available only on native lib
        //
        // /////////////////////////////////////////////////////////////////
        final ImageWriteParam iwp = writer.getDefaultWriteParam();

        if (writer.getClass()
                .getName()
                .equals("com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriter")) {
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            iwp.setCompressionQuality(0.75f); // we can control quality here
        }

        writer.setOutput(memOutStream);
        try {
            writer.write(null, new IIOImage(finalImage, null, null), iwp);
            memOutStream.flush();
            // this doesn't close the destination output stream
            memOutStream.close();
        } finally {
            writer.dispose();
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, value);
        return PNGLegendOutputFormat.MIME_TYPE;
    }
}
