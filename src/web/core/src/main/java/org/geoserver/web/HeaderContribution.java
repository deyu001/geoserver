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

package org.geoserver.web;

import java.util.logging.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geotools.util.logging.Logging;

/**
 * Extension point for contributing additional css and/or javascript to a page.
 *
 * <p>Use of this extension point is more suited toward bulk updating of multiple existing pages in
 * the GeoServer application. For contributing css or javascript to a single page one should use the
 * existing Wicket approach.
 *
 * <p>Instances of this class are registered in the spring context. Example:
 *
 * <pre>
 * &lt;bean id="myHeaderContribution" class="org.geoserver.web.HeaderContribution">
 *   &lt;property name="scope" value="com.acme.MyClass"/>
 *   &lt;property name="cssFilename=" value="mycss.css"/>
 *   &lt;property name="javaScriptFilename=" value="myjavascript.js"/>
 * &lt;/bean>
 * </pre>
 *
 * Would correspond to a package structure like:
 *
 * <pre>
 * src/
 *   com.acme/
 *      MyClass.java
 *      mycss.css
 *      myjavascript.css
 *
 * </pre>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class HeaderContribution {

    static Logger LOGGER = Logging.getLogger("org.geoserver.web");

    Class<?> scope;
    String cssFilename, javaScriptFilename, faviconFilename;

    public Class<?> getScope() {
        return scope;
    }

    public void setScope(Class<?> scope) {
        this.scope = scope;
    }

    public String getCSSFilename() {
        return cssFilename;
    }

    public void setCSSFilename(String filename) {
        this.cssFilename = filename;
    }

    public String getJavaScriptFilename() {
        return javaScriptFilename;
    }

    public void setJavaScriptFilename(String javaScriptFilename) {
        this.javaScriptFilename = javaScriptFilename;
    }

    public String getFaviconFilename() {
        return faviconFilename;
    }

    public void setFaviconFilename(String faviconName) {
        this.faviconFilename = faviconName;
    }

    /**
     * Determines if the header contribution should apply to a particular page or not.
     *
     * <p>This implementation always returns true, if clients need a more flexible mechanism for
     * determining which pages apply they should subclass and override this method.
     */
    public boolean appliesTo(WebPage page) {
        return true;
    }

    /**
     * Returns the resource reference to the css for the header contribution, or null if there is no
     * css contribution.
     */
    public PackageResourceReference getCSS() {
        if (scope != null && cssFilename != null) {
            return new PackageResourceReference(scope, cssFilename);
        }

        return null;
    }

    /**
     * Returns the resource reference to the javascript for the header contribution, or null if
     * there is no javascript contribution.
     */
    public PackageResourceReference getJavaScript() {
        if (scope != null && javaScriptFilename != null) {
            return new PackageResourceReference(scope, javaScriptFilename);
        }

        return null;
    }

    /**
     * Returns the resource reference to a replacement favicon for the header contribution, or null
     * if there is no favicon replacement
     */
    public PackageResourceReference getFavicon() {
        if (scope != null && faviconFilename != null) {
            return new PackageResourceReference(scope, faviconFilename);
        }

        return null;
    }
}
