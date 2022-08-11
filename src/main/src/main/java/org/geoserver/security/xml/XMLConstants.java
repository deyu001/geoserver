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

package org.geoserver.security.xml;

/**
 * Holding XML Constants for Element and AttributeNames
 *
 * @author christian
 */
public class XMLConstants {

    public static final String NS_XMLSCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String NSP_XMLSCHEMA = "xsi";

    public static final String FILE_UR = "users.xml";
    public static final String FILE_RR = "roles.xml";
    public static final String FILE_UR_SCHEMA = "users.xsd";
    public static final String FILE_RR_SCHEMA = "roles.xsd";

    /** Namespace Prefix for User Registry */
    public static final String NS_UR = "http://www.geoserver.org/security/users";

    public static final String NSP_UR = "gsu";
    public static final String SCHEMA_UR = NS_UR + " " + FILE_UR_SCHEMA;
    public static final String VERSION_UR_1_0 = "1.0";
    public static final String E_PROPERTY_UR = "property";
    public static final String A_PROPERTY_NAME_UR = "name";

    public static final String E_USERREGISTRY_UR = "userRegistry";
    public static final String A_VERSION_UR = "version";

    public static final String E_USERS_UR = "users";
    public static final String E_GROUPS_UR = "groups";
    public static final String E_USER_UR = "user";
    public static final String A_USER_NAME_UR = "name";
    public static final String A_USER_PASSWORD_UR = "password";
    public static final String A_USER_ENABLED_UR = "enabled";

    public static final String E_GROUP_UR = "group";
    public static final String A_GROUP_NAME_UR = "name";
    public static final String A_GROUP_ENABLED_UR = "enabled";
    public static final String E_MEMBER_UR = "member";
    public static final String A_MEMBER_NAME_UR = "username";

    /** Namespace Prefix for Role Registry */
    public static final String NS_RR = "http://www.geoserver.org/security/roles";

    public static final String NSP_RR = "gsr";
    public static final String SCHEMA_RR = NS_RR + " " + FILE_RR_SCHEMA;
    public static final String E_ROLEREGISTRY_RR = "roleRegistry";
    public static final String VERSION_RR_1_0 = "1.0";
    public static final String A_VERSION_RR = "version";
    public static final String E_PROPERTY_RR = "property";
    public static final String A_PROPERTY_NAME_RR = "name";

    public static final String E_ROLELIST_RR = "roleList";
    public static final String E_ROLE_RR = "role";
    public static final String A_ROLEID_RR = "id";
    public static final String A_PARENTID_RR = "parentID";

    public static final String E_USERLIST_RR = "userList";
    public static final String E_USERROLES_RR = "userRoles";
    public static final String A_USERNAME_RR = "username";
    public static final String E_ROLEREF_RR = "roleRef";
    public static final String A_ROLEREFID_RR = "roleID";

    public static final String E_GROUPLIST_RR = "groupList";
    public static final String E_GROUPROLES_RR = "groupRoles";
    public static final String A_GROUPNAME_RR = "groupname";
}
