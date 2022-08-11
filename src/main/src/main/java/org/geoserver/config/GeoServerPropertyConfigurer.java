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

package org.geoserver.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;

/**
 * A spring placeholder configurer that loads properties from the data directory.
 *
 * <p>This class is used by declaring an instance in a spring context:
 *
 * <pre>
 *  &lt;bean id="myPropertyConfigurer" class="org.geoserver.config.GeoServerPropertyConfigurer">
 *    &lt;constructor-arg ref="dataDirectory"/>
 *    &lt;property name="location" value="file:myDirectory/myFile.properties"/>
 *    &lt;property name="properties">
 *      &lt;props>
 *        &lt;prop key="prop1">value1&lt;/prop>
 *        &lt;prop key="prop2">value2&lt;/prop>
 *      &lt;/props>
 *    &lt;/property>
 *  &lt;/bean>
 * </pre>
 *
 * The location <tt>myDirectory/myFile.properties</tt> will be resolved relative to the data
 * directory.
 *
 * <p>In the same spring context the configurer is used as follows:
 *
 * <pre>
 *  &lt;bean id="myBean" class="com.xyz.MyClass">
 *    &lt;property name="someProperty" value="${prop1}"/>
 *    &lt;property name="someOtherProperty" value="${prop2}"/>
 *  &lt;/bean>
 * </pre>
 *
 * If the file <tt>myDirectory/myFile.properties</tt> exists then the property values will be loaded
 * from it, otherwise the defaults declared on the property configurer will be used. By default when
 * the resource is not found it will be copied out into the data directory. However {@link
 * #setCopyOutTemplate(boolean)} can be used to control this.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerPropertyConfigurer extends PropertyPlaceholderConfigurer {

    static Logger LOGGER = Logging.getLogger("org.geoserver.config");

    org.geoserver.platform.resource.Resource configFile;
    protected GeoServerDataDirectory data;
    boolean copyOutTemplate = true;
    String comments;

    public GeoServerPropertyConfigurer(GeoServerDataDirectory data) {
        this.data = data;
    }

    public void setCopyOutTemplate(boolean copyOutTemplate) {
        this.copyOutTemplate = copyOutTemplate;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    /** @return the configLocation */
    public org.geoserver.platform.resource.Resource getConfigFile() {
        return configFile;
    }

    @Override
    public void setLocation(Resource location) {
        try {
            location = SpringResourceAdaptor.relative(location, data.getResourceStore());
            if (location instanceof SpringResourceAdaptor) {
                configFile = ((SpringResourceAdaptor) location).getResource();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading resource " + location, e);
        }

        super.setLocation(location);
    }

    @Override
    public void setLocations(Resource[] locations) {
        throw new UnsupportedOperationException("Only a single location is supported");
    }

    @Override
    protected void loadProperties(Properties props) throws IOException {
        try {
            super.loadProperties(props);
        } catch (FileNotFoundException e) {
            // location was not found, create
            if (configFile != null && copyOutTemplate) {
                try (OutputStream fout = configFile.out()) {
                    props.store(fout, comments);
                    fout.flush();
                }
            }
        }
    }

    /**
     * Force reloading the properties which may have been updated in the meanwhile; after a restore
     * as an instance.
     */
    public void reload() throws IOException {
        if (localProperties != null) {
            for (Properties props : localProperties) {
                loadProperties(props);
            }
        }
    }

    @Override
    protected String convertPropertyValue(String property) {
        return property.replace("${GEOSERVER_DATA_DIR}", data.root().getPath());
    }
}
