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

package org.geoserver.security.impl;

import static org.geoserver.security.impl.DataAccessRule.ANY;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * Allows one to manage the rules used by the service layer security subsystem TODO: consider
 * splitting the persistence of properties into two strategies, and in memory one, and a file system
 * one (this class is so marginal that I did not do so right away, in memory access is mostly handy
 * for testing)
 */
public class ServiceAccessRuleDAO extends AbstractAccessRuleDAO<ServiceAccessRule> {
    private static final Logger LOGGER = Logging.getLogger(ServiceAccessRuleDAO.class);

    /** property file name */
    static final String SERVICES = "services.properties";

    /** the catalog */
    Catalog rawCatalog;

    /** Returns the instanced contained in the Spring context for the UI to use */
    public static ServiceAccessRuleDAO get() {
        return GeoServerExtensions.bean(ServiceAccessRuleDAO.class);
    }

    public ServiceAccessRuleDAO(GeoServerDataDirectory dd, Catalog rawCatalog) throws IOException {
        super(dd, SERVICES);
        this.rawCatalog = rawCatalog;
    }

    /** Builds a new dao */
    public ServiceAccessRuleDAO() throws IOException {
        super(GeoServerExtensions.bean(GeoServerDataDirectory.class), SERVICES);
    }

    /** Builds a new dao with a custom security dir. Used mostly for testing purposes */
    ServiceAccessRuleDAO(Catalog rawCatalog, Resource securityDir) {
        super(securityDir, SERVICES);
    }

    /** Parses the rules contained in the property file */
    protected void loadRules(Properties props) {
        TreeSet<ServiceAccessRule> result = new TreeSet<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String ruleKey = (String) entry.getKey();
            String ruleValue = (String) entry.getValue();

            ServiceAccessRule rule = parseServiceAccessRule(ruleKey, ruleValue);
            if (rule != null) {
                if (result.contains(rule))
                    LOGGER.warning(
                            "Rule "
                                    + ruleKey
                                    + "."
                                    + ruleValue
                                    + " overwrites another rule on the same path");
                result.add(rule);
            }
        }

        // make sure to add the "all access alloed" rule if the set if empty
        if (result.isEmpty()) {
            result.add(new ServiceAccessRule(new ServiceAccessRule()));
        }

        rules = result;
    }

    /**
     * Parses a single layer.properties line into a {@link DataAccessRule}, returns false if the
     * rule is not valid
     */
    ServiceAccessRule parseServiceAccessRule(String ruleKey, String ruleValue) {
        final String rule = ruleKey + "=" + ruleValue;

        // parse
        String[] elements = parseElements(ruleKey);
        if (elements.length != 2) {
            LOGGER.warning(
                    "Invalid rule "
                            + rule
                            + ", the expected format is service.method=role1,role2,...");
            return null;
        }
        String service = elements[0];
        String method = elements[1];
        Set<String> roles = parseRoles(ruleValue);

        // check ANY usage sanity
        if (ANY.equals(service)) {
            if (!ANY.equals(method)) {
                LOGGER.warning(
                        "Invalid rule "
                                + rule
                                + ", when namespace "
                                + "is * then also layer must be *. Skipping rule "
                                + rule);
                return null;
            }
        }

        // build the rule
        return new ServiceAccessRule(service, method, roles);
    }

    /** Turns the rules list into a property bag */
    protected Properties toProperties() {
        Properties props = new Properties();
        for (ServiceAccessRule rule : rules) {
            props.put(rule.getKey(), rule.getValue());
        }
        return props;
    }

    /** Parses workspace.layer.mode into an array of strings */
    private String[] parseElements(String path) {
        // regexp: ignore extra spaces, split on dot
        return path.split("\\s*\\.\\s*");
    }

    /** Returns a sorted set of rules associated to the role */
    public SortedSet<ServiceAccessRule> getRulesAssociatedWithRole(String role) {
        SortedSet<ServiceAccessRule> result = new TreeSet<>();
        for (ServiceAccessRule rule : getRules())
            if (rule.getRoles().contains(role)) result.add(rule);
        return result;
    }
}
