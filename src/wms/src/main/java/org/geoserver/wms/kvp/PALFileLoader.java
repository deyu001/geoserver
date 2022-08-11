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

package org.geoserver.wms.kvp;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Loads a JASC Pal files into an {@link IndexColorModel}.
 *
 * <p>I made a real minor extension to the usual form of a JASC pal file which allows us to provide
 * values in the #ffffff or 0Xffffff hex form.
 *
 * <p>Note that this kind of file does not support explicitly setting transparent pixel. However I
 * have implemented this workaround, if you use less than 256 colors in your paletteInverter I will
 * accordingly set the transparent pixel to the first available position in the paletteInverter,
 * which is palette_size. If you use 256 colors no transparency will be used for the image we
 * generate.
 *
 * <p><strong>Be aware</strong> that IrfanView does not always report correctly the size of the
 * palette it exports. Be ready to manually correct the number of colors reported.
 *
 * <p>Here is an explanation of what a JASC pal file should look like:
 *
 * <p><a href="http://www.cryer.co.uk/filetypes/p/pal.htm">JASC PAL file</a>
 *
 * <p>and here is a list of other possible formats we could parse (in the future if we have time or
 * someone pays for it :-) )
 *
 * <p><a href="http://www.pl32.com/forum/viewtopic.php?t=873">alternative PAL file formats</a>
 *
 * @author Simone Giannecchini
 */
class PALFileLoader {
    protected static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    "it.geosolutions.inversecolormap.PALFileLoader");

    /** Size of the color map we'll use. */
    protected int mapsize;

    /** Final index color model. */
    protected IndexColorModel indexColorModel;

    /**
     * {@link PALFileLoader} constructor that accept a resource.
     *
     * <p>Note that the transparentIndex pixel should not exceed the last zero-based index available
     * for the colormap we area going to create. If this happens we might get very bad behaviour.
     * Note also that if we set this parameter to -1 we'll get an opaque {@link IndexColorModel}.
     *
     * @param file the palette file.
     */
    public PALFileLoader(Resource file) {
        if (file.getType() != Type.RESOURCE)
            throw new IllegalArgumentException("The provided file does not exist.");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(file.in()));
            // header
            boolean loadNext = false;
            String temp = trimNextLine(reader);
            if (temp.equalsIgnoreCase("JASC-PAL")) {
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Found header in palette file");
                loadNext = true;
            }

            // version
            if (loadNext) {
                temp = trimNextLine(reader);
                if (temp.equalsIgnoreCase("0100")) {
                    if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Found version in palette file");
                    loadNext = true;
                }
            }

            // num colors
            if (loadNext) temp = trimNextLine(reader);

            this.mapsize = Integer.parseInt(temp);
            if (mapsize > 256 || mapsize <= 0)
                throw new IllegalArgumentException("The provided number of colors is invalid");

            // load various colors
            final byte colorMap[][] = new byte[3][mapsize < 256 ? mapsize + 1 : mapsize];
            for (int i = 0; i < mapsize; i++) {
                // get the line
                temp = trimNextLine(reader);

                if (temp.startsWith("#")) temp = "0x" + temp.substring(1);

                if (temp.startsWith("0x") || temp.startsWith("0X")) {
                    final Color color = Color.decode(temp);
                    colorMap[0][i] = (byte) color.getRed();
                    colorMap[1][i] = (byte) color.getGreen();
                    colorMap[2][i] = (byte) color.getBlue();
                } else {
                    // tokenize it
                    final StringTokenizer tokenizer = new StringTokenizer(temp, " ", false);
                    int numComponents = 0;
                    while (tokenizer.hasMoreTokens()) {
                        if (numComponents >= 3)
                            throw new IllegalArgumentException(
                                    "The number of components in one the color is greater than 3!");
                        colorMap[numComponents++][i] =
                                (byte) Integer.parseInt(tokenizer.nextToken());
                    }
                    if (numComponents != 3)
                        throw new IllegalArgumentException(
                                "The number of components in one the color is invalid!");
                }
            }

            // //
            //
            // create the index color model reserving space for the transparent
            // pixel is room exists.
            //
            ////
            if (mapsize < 256)
                this.indexColorModel =
                        new IndexColorModel(
                                8, mapsize + 1, colorMap[0], colorMap[1], colorMap[2], mapsize);
            else
                this.indexColorModel =
                        new IndexColorModel(8, mapsize, colorMap[0], colorMap[1], colorMap[2]);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);

        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
                }
        }
    }

    public String trimNextLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException(
                    "Was expecting to get another line, but the end of file was reached, while reading a PAL file");
        }
        return line.trim();
    }

    public IndexColorModel getIndexColorModel() {
        return indexColorModel;
    }

    public int getMapsize() {
        return mapsize;
    }
}
