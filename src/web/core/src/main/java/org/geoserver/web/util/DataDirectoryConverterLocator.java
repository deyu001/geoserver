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

package org.geoserver.web.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

/**
 * Special converter locator which can resolve relative urls relative to the GeoServer data
 * directory.
 *
 * <p>This converter locator will turn URL's of the form "file:data/..." into full path URL's such
 * as "file://var/lib/geoserver/data/...".
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
@SuppressWarnings("serial")
public class DataDirectoryConverterLocator implements IConverterLocator {

    static final Logger LOGGER = Logging.getLogger(DataDirectoryConverterLocator.class);
    GeoServerResourceLoader resourceLoader;

    public DataDirectoryConverterLocator(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @SuppressWarnings("unchecked")
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (File.class.isAssignableFrom(type)) {
            return (IConverter<C>) new FileLocator();
        }
        if (URL.class.isAssignableFrom(type)) {
            return (IConverter<C>) new URLLocator();
        }
        if (URI.class.isAssignableFrom(type)) {
            return (IConverter<C>) new URILocator();
        }

        return null;
    }

    File toFile(String value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        try {
            // first try as a url to strip off url protocol prefix
            try {
                URL url = new URL(value);
                if ("file".equals(url.getProtocol())) {
                    value = url.getFile();
                }
            } catch (MalformedURLException e) {
            }

            File file = new File(value);
            if (file.isAbsolute()) {
                return file;
            }

            return resourceLoader.find(value);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error converting \"" + value + "\" to File", e);
        }

        return null;
    }

    String fromFile(File file) {
        File data = resourceLoader.getBaseDirectory();

        // figure out if the file is a child of the base data directory
        List<String> path = new ArrayList<>();
        boolean isChild = false;
        while (file != null) {
            if (file.equals(data)) {
                isChild = true;
                break;
            }

            path.add(file.getName());
            file = file.getParentFile();
        }

        if (isChild) {
            StringBuffer b = new StringBuffer("file:");
            for (int i = path.size() - 1; i > -1; i--) {
                b.append(path.get(i)).append(File.separatorChar);
            }
            b.setLength(b.length() - 1);
            return b.toString();
        }

        return null;
    }

    class FileLocator implements IConverter<File> {

        public File convertToObject(String value, Locale locale) {
            return toFile(value);
        }

        public String convertToString(File value, Locale locale) {
            return fromFile(value);
        }
    }

    class URLLocator implements IConverter<URL> {

        public URL convertToObject(String value, Locale locale) {
            File file = toFile(value);
            if (file != null) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException e) {
                    LOGGER.log(Level.WARNING, "Error converting \"" + value + "\" to URL", e);
                }
            }

            return null;
        }

        public String convertToString(URL value, Locale locale) {
            String file = value.getFile();
            if (file != null && !"".equals(file)) {
                return fromFile(new File(value.getFile()));
            }
            return null;
        }
    }

    class URILocator implements IConverter<URI> {

        public URI convertToObject(String value, Locale locale) {
            File file = toFile(value);
            if (file != null) {
                return file.toURI();
            }
            return null;
        }

        public String convertToString(URI value, Locale locale) {
            try {
                return new URLLocator().convertToString(value.toURL(), locale);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error converting \"" + value + "\" to URI", e);
                return null;
            }
        }
    }
}
