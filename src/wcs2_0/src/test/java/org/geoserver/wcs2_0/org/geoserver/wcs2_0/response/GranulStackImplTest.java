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

package org.geoserver.wcs2_0.org.geoserver.wcs2_0.response;

import static org.junit.Assert.assertTrue;

import com.sun.media.jai.operator.ImageReadDescriptor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.jai.RenderedOp;
import org.geoserver.wcs2_0.response.GranuleStackImpl;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class GranulStackImplTest {

    @Test
    public void testImageDispose() throws Exception {
        // build a stream and a reader on top of a one pixel GIF image,
        // with a check on whether they get disposed of
        AtomicBoolean readerDisposed = new AtomicBoolean(false);
        AtomicBoolean streamDisposed = new AtomicBoolean(false);
        byte[] bytes =
                Base64.getDecoder().decode("R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==");
        MemoryCacheImageInputStream is =
                new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes)) {
                    @Override
                    public void close() {
                        streamDisposed.set(true);
                    }
                };
        final ImageReader nativeReader = ImageIO.getImageReadersByFormatName("GIF").next();
        nativeReader.setInput(is);
        ImageReader reader =
                new ImageReader(null) {

                    @Override
                    public int getNumImages(boolean allowSearch) throws IOException {
                        return nativeReader.getNumImages(allowSearch);
                    }

                    @Override
                    public int getWidth(int imageIndex) throws IOException {
                        return nativeReader.getWidth(imageIndex);
                    }

                    @Override
                    public int getHeight(int imageIndex) throws IOException {
                        return nativeReader.getHeight(imageIndex);
                    }

                    @Override
                    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
                            throws IOException {
                        return nativeReader.getImageTypes(imageIndex);
                    }

                    @Override
                    public IIOMetadata getStreamMetadata() throws IOException {
                        return nativeReader.getStreamMetadata();
                    }

                    @Override
                    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
                        return nativeReader.getImageMetadata(imageIndex);
                    }

                    @Override
                    public BufferedImage read(int imageIndex, ImageReadParam param)
                            throws IOException {
                        return nativeReader.read(imageIndex, param);
                    }

                    @Override
                    public void dispose() {
                        nativeReader.dispose();
                        readerDisposed.set(true);
                    }
                };
        // wrap it in a image read
        RenderedOp image =
                ImageReadDescriptor.create(
                        is, 0, false, false, false, null, null, null, reader, null);

        // build a coverage and a granule stack around it
        GridCoverageFactory coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(null);
        GridCoverage2D coverage =
                coverageFactory.create(
                        "foo",
                        image,
                        new ReferencedEnvelope(0, 1, 0, 1, DefaultGeographicCRS.WGS84));
        GranuleStackImpl stack = new GranuleStackImpl("fooBar", DefaultGeographicCRS.WGS84, null);
        stack.addCoverage(coverage);

        // check stream and reader have been properly disposed of on stack dispose
        stack.dispose(true);
        assertTrue(streamDisposed.get());
        assertTrue(readerDisposed.get());
    }
}
