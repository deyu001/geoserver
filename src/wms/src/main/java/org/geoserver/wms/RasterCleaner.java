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

package org.geoserver.wms;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageAdapter;
import javax.media.jai.RenderedImageList;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.wms.map.RenderedImageTimeDecorator;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.util.ImageUtilities;

public class RasterCleaner extends AbstractDispatcherCallback {
    static final ThreadLocal<List<RenderedImage>> images = new ThreadLocal<>();

    static final ThreadLocal<List<GridCoverage2D>> coverages = new ThreadLocal<>();

    /** Schedules a RenderedImage for cleanup at the end of the request */
    public static void addImage(RenderedImage image) {
        if (image == null) {
            return;
        }

        List<RenderedImage> list = images.get();
        if (list == null) {
            list = new ArrayList<>();
            images.set(list);
        }
        list.add(image);
    }

    /** Schedules a RenderedImage for cleanup at the end of the request */
    public static void addCoverage(GridCoverage2D coverage) {
        if (coverage == null) {
            return;
        }

        List<GridCoverage2D> list = coverages.get();
        if (list == null) {
            list = new ArrayList<>();
            coverages.set(list);
        }
        list.add(coverage);
    }

    @Override
    public void finished(Request request) {
        disposeCoverages();
        disposeImages();
    }

    private void disposeImages() {
        List<RenderedImage> list = images.get();
        if (list != null) {
            images.remove();
            for (RenderedImage image : list) {
                if (image instanceof RenderedImageAdapter) {
                    image = ((RenderedImageAdapter) image).getWrappedImage();
                }

                if (image instanceof RenderedImageTimeDecorator)
                    image = ((RenderedImageTimeDecorator) image).getDelegate();

                if (image instanceof RenderedImageList) {
                    RenderedImageList ril = (RenderedImageList) image;
                    for (int i = 0; i < ril.size(); i++) {
                        disposeImage((RenderedImage) ril.get(i));
                    }
                } else {
                    disposeImage(image);
                }
            }
            list.clear();
        }
    }

    private void disposeImage(RenderedImage image) {
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        } else if (image instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) image;
            bi.flush();
        }
    }

    private void disposeCoverages() {
        List<GridCoverage2D> list = coverages.get();
        if (list != null) {
            coverages.remove();
            for (GridCoverage2D coverage : list) {
                coverage.dispose(true);
            }
            list.clear();
        }
    }

    public List<RenderedImage> getImages() {
        return images.get();
    }

    public List<GridCoverage2D> getCoverages() {
        return coverages.get();
    }
}
