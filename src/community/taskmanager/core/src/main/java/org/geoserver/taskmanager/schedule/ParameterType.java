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

package org.geoserver.taskmanager.schedule;

import com.google.common.collect.Lists;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * A Parameter Type For a Task
 *
 * @author Niels Charlier
 */
public interface ParameterType {

    /** STRING type */
    public ParameterType STRING =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public String parse(String value, List<String> dependsOnRawValues) {
                    return value;
                }
            };

    /** INTEGER type */
    public ParameterType INTEGER =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public Integer parse(String value, List<String> dependsOnRawValues) {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            };

    /** BOOLEAN type */
    public ParameterType BOOLEAN =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return Lists.newArrayList("true", "false");
                }

                @Override
                public Boolean parse(String value, List<String> dependsOnRawValues) {
                    return Boolean.parseBoolean(value);
                }
            };

    /** URI type */
    public ParameterType URI =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public java.net.URI parse(String value, List<String> dependsOnRawValues) {
                    try {
                        if (!value.contains(":")) {
                            value = "file:" + value;
                        }
                        return new java.net.URI(value);
                    } catch (URISyntaxException e) {
                        return null;
                    }
                }
            };

    /** SQL Type */
    public ParameterType SQL =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public String parse(String value, List<String> dependsOnRawValues) {
                    // protection against sneaking in extra statement
                    if (value.contains(";")) {
                        return null;
                    }
                    return value;
                }
            };

    /**
     * List possible values for this parameter (when applicable). Include an empty string if custom
     * value is also allowed.
     *
     * @param dependsOnRawValues raw values of depending parameters.
     * @return list of possible values, null if not applicable.
     */
    public List<String> getDomain(List<String> dependsOnRawValues);

    /**
     * Validate and parse a parameter value for this parameter (at run time).
     *
     * @param value the raw value.
     * @param dependsOnRawValues raw values of depending parameters.
     * @return the parsed value, NULL if the value is invalid.
     */
    public Object parse(String value, List<String> dependsOnRawValues);

    /**
     * Validate a parameter value (at configuration time).
     *
     * @param value the raw value.
     * @param dependsOnRawValues raw values of depending parameters.
     * @return true if the value is considered valid at configuration time (may still be considered
     *     invalid at parse time)
     */
    public default boolean validate(String value, List<String> dependsOnRawValues) {
        return parse(value, dependsOnRawValues) != null;
    }

    /**
     * Returns a list of web actions related to this type
     *
     * @return list of web actions
     */
    public default List<String> getActions() {
        return Collections.emptyList();
    }
}
