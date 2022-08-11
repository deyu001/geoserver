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

package org.geoserver.wcs2_0.xml;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import java.io.File;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

public class GeoTiffGetCoverageTest extends WCSTestSupport {

    @Test
    public void testGeotiffExtensionCompressionJPEGWrongQuality1() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/geotiff/geotiffExtensionCompressionJPEGWrongQuality1.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "105");
    }

    @Test
    public void testGeotiffExtensionCompressionJPEGWrongQuality2() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/geotiff/geotiffExtensionCompressionJPEGWrongQuality2.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "0");
    }

    @Test
    public void testGeotiffExtensionCompressionLZW() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionLZW.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "LZW",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "5", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionCompressionDeflate() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionDeflate.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "Deflate",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "32946",
                field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    @Ignore // TODO
    public void testGeotiffExtensionCompressionHuffman() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionHuffman.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "Deflate",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "32946",
                field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    @Ignore
    public void testGeotiffExtensionCompressionPackBits() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionPackBits.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "PackBits",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "32773",
                field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionCompressionWrongCompression() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/geotiff/geotiffExtensionCompressionWrongCompression.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkOws20Exception(response, 404, WcsExceptionCode.CompressionInvalid.toString(), "OUCH");
    }

    @Test
    public void testGeotiffExtensionCompressionJPEG() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionJPEG.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "JPEG",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "7", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionTilingDefault() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTilingDefault.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_gtiff.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // tiling
        assertTrue(reader.isImageTiled(0));
        assertEquals(368, reader.getTileHeight(0));
        assertEquals(368, reader.getTileWidth(0));

        IIOMetadataNode node =
                ((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionTilingWrong1() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTilingWrong1.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "13");
    }

    @Test
    public void testGeotiffExtensionTilingWrong2() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTilingWrong2.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "25");
    }

    @Test
    public void testGeotiffExtensionTiling() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTiling.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_gtiff.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // tiling
        assertTrue(reader.isImageTiled(0));
        assertEquals(256, reader.getTileHeight(0));
        assertEquals(256, reader.getTileWidth(0));

        IIOMetadataNode node =
                ((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionBanded() throws Exception {

        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wcs:GetCoverage\n"
                        + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                        + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                        + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                        + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd\"\n"
                        + "  service=\"WCS\"\n"
                        + "  version=\"2.0.1\">\n"
                        + "  <wcs:Extension>\n"
                        + "    <wcsgeotiff:compression>None</wcsgeotiff:compression>\n"
                        + "    <wcsgeotiff:interleave>band</wcsgeotiff:interleave>\n"
                        + "  </wcs:Extension>\n"
                        + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                        + "  <wcs:format>image/tiff</wcs:format>\n"
                        + "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        // TODO Fix this test
        //        byte[] tiffContents = getBinary(response);
        //        File file = File.createTempFile("exception", "xml", new File("./target"));
        //        FileUtils.writeByteArrayToFile(file, tiffContents);
        //
        //        String ex=FileUtils.readFileToString(file);
    }
}
