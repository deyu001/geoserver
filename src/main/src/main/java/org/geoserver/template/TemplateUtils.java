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

package org.geoserver.template;

import static freemarker.template.Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import freemarker.template.utility.ClassUtil;
import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for Freemarker template configuration
 *
 * @author Kevin Smith, Boundless
 */
public class TemplateUtils {
    /** Reference feature version for Freemarker templates */
    public static Version FM_VERSION = Configuration.VERSION_2_3_0;

    /** Classes that should not be resolved in Freemarker templates */
    private static final Collection<String> ILLEGAL_FREEMARKER_CLASSES =
            Arrays.asList(
                    freemarker.template.utility.ObjectConstructor.class.getName(),
                    freemarker.template.utility.Execute.class.getName(),
                    "freemarker.template.utility.JythonRuntime");

    /**
     * Classes that should be resolved in Freemarker templates, even if they would not be by default
     */
    private static final Collection<String> LEGAL_FREEMARKER_CLASSES = Arrays.asList();

    /** Get a Freemarker configuration that is safe against malicious templates */
    public static Configuration getSafeConfiguration() {
        Configuration config = new Configuration(DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        config.setNewBuiltinClassResolver(
                (name, env, template) -> {
                    if (ILLEGAL_FREEMARKER_CLASSES.stream().anyMatch(name::equals)) {
                        throw new TemplateException(
                                String.format(
                                        "Class %s is not allowed in Freemarker templates", name),
                                env);
                    }
                    if (LEGAL_FREEMARKER_CLASSES.stream().anyMatch(name::equals)) {
                        try {
                            ClassUtil.forName(name);
                        } catch (ClassNotFoundException e) {
                            throw new TemplateException(e, env);
                        }
                    }

                    return TemplateClassResolver.SAFER_RESOLVER.resolve(name, env, template);
                });
        return config;
    }
}
