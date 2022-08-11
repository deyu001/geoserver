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

package org.geoserver.wms.featureinfo;

import freemarker.template.Template;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;

/**
 * Produces a FeatureInfo response in HTML. Relies on {@link AbstractFeatureInfoResponse} and the
 * feature delegate to do most of the work, just implements an HTML based writeTo method.
 *
 * @author James Macgill, PSU
 * @author Andrea Aime, TOPP
 * @version $Id$
 */
public class HTMLFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private static final String FORMAT = "text/html";

    private FreeMarkerTemplateManager templateManager;

    private WMS wms;

    public HTMLFeatureInfoOutputFormat(final WMS wms, GeoServerResourceLoader resourceLoader) {
        super(FORMAT);
        this.wms = wms;
        this.templateManager =
                new HTMLTemplateManager(
                        FreeMarkerTemplateManager.OutputFormat.HTML, wms, resourceLoader);
    }

    /**
     * Writes the image to the client.
     *
     * @param out The output stream to write to.
     * @throws ServiceException For problems with geoserver
     * @throws java.io.IOException For problems writing the output.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        templateManager.write(results, request, out);
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }

    public FreeMarkerTemplateManager getTemplateManager() {
        return templateManager;
    }

    /** */
    private final class HTMLTemplateManager extends FreeMarkerTemplateManager {

        public HTMLTemplateManager(
                OutputFormat format, WMS wms, GeoServerResourceLoader resourceLoader) {
            super(format, wms, resourceLoader);
        }

        @Override
        protected boolean templatesExist(
                Template header, Template footer, List<FeatureCollection> collections)
                throws IOException {
            return true;
        }

        @Override
        protected void handleContent(
                List<FeatureCollection> collections,
                OutputStreamWriter osw,
                GetFeatureInfoRequest request)
                throws IOException {
            for (int i = 0; i < collections.size(); i++) {
                FeatureCollection fc = collections.get(i);
                Template content = getContentTemplate(fc, wms.getCharSet());
                String typeName = request.getQueryLayers().get(i).getName();
                processTemplate(typeName, fc, content, osw);
            }
        }

        @Override
        protected String getTemplateFileName(String filename) {
            return filename + ".ftl";
        }
    }
}
