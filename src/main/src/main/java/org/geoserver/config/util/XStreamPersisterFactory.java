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

package org.geoserver.config.util;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Factory for {@link XStreamPersister} instances.
 *
 * <p>This class is a singleton registered in a spring context. Any code that needs to create an
 * XStreamPersister instance should do one of the following:
 *
 * <ol>
 *   <li>Use dependency injection via spring. Example:
 *       <pre>
 *       <bean id="myBean" class="com.xyz.MyBean">
 *         <constructor-arg ref="xstreamPersisterFactory"/>
 *       </bean>
 *     </pre>
 *   <li>Lookup via {@link GeoServerExtensions#bean(Class)}:
 *       <pre>
 *       XStreamPersisterFactory xpf = GeoServerExtension.bean(XStreamPeristerFactory.class);
 *     </pre>
 * </ol>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XStreamPersisterFactory implements ApplicationContextAware {

    private List<XStreamPersisterInitializer> initializers;

    /** Creates an instance configured to persist XML. */
    public XStreamPersister createXMLPersister() {
        return buildPersister(null);
    }

    /** Creates an instance configured to persist JSON. */
    public XStreamPersister createJSONPersister() {
        return buildPersister(new JettisonMappedXmlDriver());
    }

    /** Builds a persister and runs the initializers against it */
    private XStreamPersister buildPersister(HierarchicalStreamDriver driver) {
        XStreamPersister persister = new XStreamPersister(driver);

        // give the initializers a chance to register their own converters, aliases and so on
        for (XStreamPersisterInitializer initializer : getInitializers()) {
            initializer.init(persister);
        }

        return persister;
    }

    private List<XStreamPersisterInitializer> getInitializers() {
        if (initializers == null) {
            initializers =
                    new ArrayList<>(
                            GeoServerExtensions.extensions(XStreamPersisterInitializer.class));
        }

        return initializers;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        initializers =
                new ArrayList<>(
                        GeoServerExtensions.extensions(
                                XStreamPersisterInitializer.class, applicationContext));
    }

    /**
     * Programmatically adds a {@link XStreamPersisterInitializer} to the factory (initializers are
     * also automatically looked up from the Spring context, use this method only if you cannot
     * Declare your initializer as a spring bean)
     */
    public void addInitializer(XStreamPersisterInitializer initializer) {
        getInitializers().add(initializer);
    }

    /**
     * Removes an initializer
     *
     * @return True if the initializer was found and removed, false otherwise
     */
    public boolean removeInitializer(XStreamPersisterInitializer initializer) {
        return getInitializers().remove(initializer);
    }
}
