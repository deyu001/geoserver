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

package org.geoserver.ows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.URLs;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourceLoader;

/**
 * Controller which publishes files through a web interface.
 *
 * <p>To use this controller, it should be mapped to a particular url in the url mapping of the
 * spring dispatcher servlet. Example:
 *
 * <pre>
 * <code>
 *   &lt;bean id="filePublisher" class="org.geoserver.ows.FilePublisher"/&gt;
 *   &lt;bean id="dispatcherMappings"
 *      &lt;property name="alwaysUseFullPath" value="true"/&gt;
 *      class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"&gt;
 *      &lt;property name="mappings"&gt;
 *        &lt;prop key="/schemas/** /*.xsd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/schemas/** /*.dtd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/styles/*"&gt;filePublisher&lt;/prop&gt;
 *      &lt;/property&gt;
 *   &lt;/bean&gt;
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class FilePublisher extends AbstractURLPublisher {
    /** Resource loader */
    protected GeoServerResourceLoader loader;

    /** Servlet resource loader */
    protected ServletContextResourceLoader scloader;

    /**
     * Creates the new file publisher.
     *
     * @param loader The loader used to locate files.
     */
    public FilePublisher(GeoServerResourceLoader loader) {
        this.loader = loader;
    }

    @Override
    protected void initServletContext(ServletContext servletContext) {
        this.scloader = new ServletContextResourceLoader(servletContext);
    }

    @Override
    protected URL getUrl(HttpServletRequest request) throws IOException {
        String ctxPath = request.getContextPath();
        String reqPath = request.getRequestURI();
        reqPath = URLDecoder.decode(reqPath, "UTF-8");
        reqPath = reqPath.substring(ctxPath.length());

        if ((reqPath.length() > 1) && reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1);
        }

        // sigh, in order to serve the file we have to open it 2 times
        // 1) to determine its mime type
        // 2) to determine its encoding and really serve it
        // we can't coalish 1) because we don't have a way to give jmimemagic the bytes at the
        // beginning of the file without disabling extension quick matching

        // load the file
        File file = loader.find(reqPath);

        if (file == null && scloader != null) {
            // try loading as a servlet resource
            ServletContextResource resource =
                    (ServletContextResource) scloader.getResource(reqPath);
            if (resource != null && resource.exists()) {
                file = resource.getFile();
            }
        }

        if (file != null) {
            return URLs.fileToUrl(file);
        } else {
            return null;
        }
    }
}
