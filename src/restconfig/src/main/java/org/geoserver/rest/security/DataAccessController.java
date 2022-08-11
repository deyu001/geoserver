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

package org.geoserver.rest.security;

import java.util.Map;
import java.util.Map.Entry;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/acl/layers")
public class DataAccessController extends AbstractAclController<DataAccessRule, DataAccessRuleDAO> {

    DataAccessController() {
        super(DataAccessRuleDAO.get());
    }

    @Override
    protected void addRuleToMap(DataAccessRule rule, Map<String, String> map) {
        map.put(rule.getKey(), rule.getValue());
    }

    @Override
    protected String keyFor(DataAccessRule rule) {
        return rule.getKey();
    }

    private String[] parseElements(String path) {
        // regexp: ignore extra spaces, split on dot
        return path.split("\\s*\\.\\s*");
    }

    @Override
    protected DataAccessRule convertEntryToRule(Entry entry) {
        String[] parts = parseElements(((String) entry.getKey()));

        AccessMode accessMode = AccessMode.getByAlias(parts[2]);

        return new DataAccessRule(
                parts[0], parts[1], accessMode, parseRoles((String) entry.getValue()));
    }

    @Override
    protected String validateRuleKey(String ruleKey) {
        String[] elements = parseElements(ruleKey);
        if (elements.length != 3) {
            return "Invalid rule "
                    + ruleKey
                    + ", the expected format is workspace.layer.mode=role1,role2,...";
        }

        String workspace = elements[0];
        String layerName = elements[1];
        String modeAlias = elements[2];

        AccessMode mode = AccessMode.getByAlias(modeAlias);
        if (mode == null) {
            return "Unknown access mode " + modeAlias + " in " + ruleKey;
        }

        if (ANY.equals(workspace)) {
            if (!ANY.equals(layerName)) {
                return "Invalid rule "
                        + ruleKey
                        + ", when namespace "
                        + "is * then also layer must be *.";
            }
        }
        if (mode == AccessMode.ADMIN && !ANY.equals(layerName)) {
            return "Invalid rule "
                    + ruleKey
                    + ", admin (a) privileges may only be applied "
                    + "globally to a workspace, layer must be *.";
        }

        return null;
    }

    @Override
    protected String getBasePath() {
        return "/security/acl/layers";
    }
}
