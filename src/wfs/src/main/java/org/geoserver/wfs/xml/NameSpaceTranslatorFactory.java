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

package org.geoserver.wfs.xml;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * NameSpaceTranslatorFactory purpose.
 *
 * <p>Follows the factory pattern. Creates and stores a list of name space translators.
 *
 * @see NameSpaceTranslator
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class NameSpaceTranslatorFactory {
    /** map of namespace names as Strings -> Class representations of NameSpaceTranslators */
    private Map<String, Class<? extends NameSpaceTranslator>> namespaceTranslators;

    /** map of prefixs as String -> Instances of NameSpaceTranslators */
    private Map<String, NameSpaceTranslator> namespaceTranslatorInstances;

    /** the only instance */
    private static final NameSpaceTranslatorFactory instance = new NameSpaceTranslatorFactory();

    /**
     * NameSpaceTranslatorFactory constructor.
     *
     * <p>Loads some default prefixes into memory when the class is first loaded.
     */
    private NameSpaceTranslatorFactory() {
        namespaceTranslators = new HashMap<>();
        namespaceTranslatorInstances = new HashMap<>();

        // TODO replace null for these default namespaces.
        namespaceTranslators.put("http://www.w3.org/2001/XMLSchema", XMLSchemaTranslator.class);
        namespaceTranslators.put("http://www.opengis.net/gml", GMLSchemaTranslator.class);

        addNameSpaceTranslator("xs", "http://www.w3.org/2001/XMLSchema");
        addNameSpaceTranslator("xsd", "http://www.w3.org/2001/XMLSchema");
        addNameSpaceTranslator("gml", "http://www.opengis.net/gml");
    }

    /**
     * getInstance purpose.
     *
     * <p>Completes the singleton pattern of this factory class.
     *
     * @return NameSpaceTranslatorFactory The instance.
     */
    public static NameSpaceTranslatorFactory getInstance() {
        return instance;
    }

    /**
     * addNameSpaceTranslator purpose.
     *
     * <p>Adds a new translator for the namespace specified if a NameSpaceTranslator was registered
     * for that namespace.
     *
     * <p>Some the magic for creating instances using the classloader occurs here (ie. the
     * translators are not loaded lazily)
     *
     * @param prefix The desired namespace prefix
     * @param namespace The desired namespace.
     */
    public void addNameSpaceTranslator(String prefix, String namespace) {
        if ((prefix == null) || (namespace == null)) {
            throw new NullPointerException();
        }

        try {
            Class<?> nstClass = namespaceTranslators.get(namespace);

            if (nstClass == null) {
                return;
            }

            Constructor<?> nstConstructor =
                    nstClass.getConstructor(
                            new Class[] {
                                String.class,
                            });
            NameSpaceTranslator nst =
                    (NameSpaceTranslator)
                            nstConstructor.newInstance(
                                    new Object[] {
                                        prefix,
                                    });
            namespaceTranslatorInstances.put(prefix, nst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * getNameSpaceTranslator purpose.
     *
     * <p>Description ...
     *
     * @param prefix the prefix of the translator to get.
     * @return the translator, or null if it was not found
     */
    public NameSpaceTranslator getNameSpaceTranslator(String prefix) {
        return namespaceTranslatorInstances.get(prefix);
    }

    /**
     * registerNameSpaceTranslator purpose.
     *
     * <p>Registers a namespace and it's translator with the factory. good for adding additional
     * namespaces :)
     *
     * @param namespace The namespace.
     * @param nameSpaceTranslator The translator class for this namespace.
     */
    public void registerNameSpaceTranslator(
            String namespace, Class<? extends NameSpaceTranslator> nameSpaceTranslator) {
        if ((nameSpaceTranslator != null)
                && NameSpaceTranslator.class.isAssignableFrom(nameSpaceTranslator)) {
            namespaceTranslators.put(namespace, nameSpaceTranslator);
        }
    }
}
