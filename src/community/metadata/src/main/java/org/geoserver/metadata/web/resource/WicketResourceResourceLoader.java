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

package org.geoserver.metadata.web.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

public class WicketResourceResourceLoader implements IStringResourceLoader {

    private static final Logger LOGGER = Logging.getLogger(WicketResourceResourceLoader.class);

    private Resource folder;

    private String resourceBundleName;

    private static String EXTENSION = ".properties";

    private boolean shouldThrowException = true;

    public WicketResourceResourceLoader(Resource folder, String resourceBundleName) {
        this.folder = folder;
        this.resourceBundleName = resourceBundleName;
        if (resourceBundleName.endsWith(EXTENSION)) {
            this.resourceBundleName = this.resourceBundleName.replace(EXTENSION, "");
        }
    }

    public String loadStringResource(Component component, String key) {
        return findResource(component.getLocale(), key);
    }

    public String loadStringResource(Class<?> clazz, String key, Locale locale, String style) {
        return findResource(locale, key);
    }

    private String findResource(Locale locale, String key) {
        String string = null;

        ResourceBundle resourceBundle = null;
        if (locale != null && key != null) {
            try {
                Resource res =
                        folder.get(resourceBundleName + "_" + locale.getLanguage() + EXTENSION);
                // Try the specific resource
                if (Resources.exists(res)) {
                    try (InputStream fis = res.in()) {
                        resourceBundle = new PropertyResourceBundle(fis);
                        try {
                            string = findString(key, string, resourceBundle);
                        } catch (Exception ignored) {
                            // ignore, try the generic resource
                        }
                    }
                }
                // Fallback to the main resource
                if (string == null) {
                    res = folder.get(resourceBundleName + EXTENSION);
                    try (InputStream fis = res.in()) {
                        resourceBundle = new PropertyResourceBundle(fis);
                        string = findString(key, string, resourceBundle);
                    }
                }
            } catch (IOException e) {
                if (shouldThrowExceptionForMissingResource()) {
                    throw new WicketRuntimeException(
                            String.format(
                                    "Unable able to locate resource bundle for the specifed base name: %s",
                                    resourceBundleName));
                }
                LOGGER.fine(
                        "Unable able to locate resource bundle for the specifed base name:"
                                + resourceBundleName);
            }
        }
        return string;
    }

    private boolean shouldThrowExceptionForMissingResource() {
        return Application.get().getResourceSettings().getThrowExceptionOnMissingResource()
                && shouldThrowException;
    }

    @Override
    public String loadStringResource(
            Class<?> clazz, String key, Locale locale, String style, String variation) {
        return findResource(locale, key);
    }

    @Override
    public String loadStringResource(
            Component component, String key, Locale locale, String style, String variation) {
        if (component != null) {
            return findResource(component.getLocale(), key);
        }
        return null;
    }

    public void setShouldThrowException(boolean shouldThrowException) {
        this.shouldThrowException = shouldThrowException;
    }

    private String findString(String key, String string, ResourceBundle resourceBundle) {
        boolean caught = false;
        try {
            string = resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            caught = true;
        }

        if (caught || string == null) {
            if (shouldThrowExceptionForMissingResource()) {
                throw new WicketRuntimeException(
                        String.format(
                                "Unable able to locate resource bundle for the specifed base name: %s",
                                resourceBundleName));
            }

            LOGGER.fine("No value found key " + key + " in resource bundle " + resourceBundleName);
        }
        return string;
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }
}
