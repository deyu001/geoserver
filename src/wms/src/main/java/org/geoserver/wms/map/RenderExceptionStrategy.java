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

package org.geoserver.wms.map;

import java.awt.geom.NoninvertibleTransformException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.RenderListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 * A {@link RenderListener} that knows which rendering exceptions are ignorable and stops rendering
 * if not.
 *
 * <p>This map producer should register an instance of this listener to the renderer in order to get
 * notified of an unexpected rendering exception through the {@link #getException()} method.
 *
 * <p>The following exception causes are going to be ignored, any other one will stop the rendering:
 *
 * <ul>
 *   <li>{@link IllegalAttributeException}: known to be thrown when a Feature attribute does not
 *       validate against it's schema.
 *   <li>{@link TransformException}: a geometry can't be transformed to the target CRS, usually
 *       because of being outside the target CRS area of validity.
 *   <li>{@link FactoryException}: the transform from source CRS to destination CRS and from there
 *       to the display failed.
 *   <li>{@link NoninvertibleTransformException}: a transformation error for geometry decimation
 * </ul>
 */
public class RenderExceptionStrategy implements RenderListener {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.wms");
    private final GTRenderer renderer;

    private Exception renderException;

    /**
     * Creates a render listener to stop the given {@code renderer} when a non ignorable exception
     * is notified
     *
     * @param renderer the renderer to {@link GTRenderer#stopRendering() stop} if a non ignorable
     *     exception occurs
     */
    public RenderExceptionStrategy(final GTRenderer renderer) {
        this.renderer = renderer;
        this.renderException = null;
    }

    /**
     * Tells whether a non ignorable exception occurred and hence the rendering process was aborted
     *
     * @return {@code true} if rendering aborted due to an exception, {@code false} otherwise
     */
    public boolean exceptionOccurred() {
        return renderException != null;
    }

    /**
     * @return the non ignorable exception occurred on the rendering loop, or {@code null} if the
     *     renderer finished successfully.
     */
    public Exception getException() {
        return renderException;
    }

    /**
     * Upon a render exception check if its cause is one that we actually want to ignore, and if not
     * abort the rendering process so the map producer can fail.
     */
    public void errorOccurred(final Exception renderException) {

        Throwable cause = renderException;

        while (cause != null) {
            if (cause instanceof TransformException
                    || cause instanceof IllegalAttributeException
                    || cause instanceof FactoryException
                    || cause instanceof NoninvertibleTransformException) {

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Ignoring renderer error", renderException);
                }

                return;
            }
            cause = cause.getCause();
        }

        // not an ignorable cause... stop rendering
        LOGGER.log(Level.FINE, "Got an unexpected render exception.", renderException);
        this.renderException = renderException;
        renderer.stopRendering();
    }

    /**
     * Not used, we're only checking exceptions here
     *
     * @see RenderListener#featureRenderer(SimpleFeature)
     */
    public void featureRenderer(SimpleFeature feature) {
        // intentionally left blank
    }
}
