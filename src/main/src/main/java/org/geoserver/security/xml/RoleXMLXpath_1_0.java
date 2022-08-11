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

import static org.geoserver.security.xml.XMLConstants.A_GROUPNAME_RR;
import static org.geoserver.security.xml.XMLConstants.A_PARENTID_RR;
import static org.geoserver.security.xml.XMLConstants.A_PROPERTY_NAME_RR;
import static org.geoserver.security.xml.XMLConstants.A_ROLEID_RR;
import static org.geoserver.security.xml.XMLConstants.A_ROLEREFID_RR;
import static org.geoserver.security.xml.XMLConstants.A_USERNAME_RR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPLIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPROLES_RR;
import static org.geoserver.security.xml.XMLConstants.E_PROPERTY_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLELIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLEREF_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLEREGISTRY_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLE_RR;
import static org.geoserver.security.xml.XMLConstants.E_USERLIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_USERROLES_RR;
import static org.geoserver.security.xml.XMLConstants.NSP_RR;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * This class provides precompiled XPath Expressions
 *
 * @author christian
 */
public class RoleXMLXpath_1_0 extends RoleXMLXpath {

    /** Singleton, the implementation is stateless */
    public static final RoleXMLXpath_1_0 Singleton = new RoleXMLXpath_1_0();

    /** XML name space context for user/group store */
    protected XPathExpression roleListExpression;

    protected XPathExpression parentExpression;
    protected XPathExpression roleNameExpression;
    protected XPathExpression rolePropertiesExpression;
    protected XPathExpression propertyNameExpression;
    protected XPathExpression propertyValueExpression;
    protected XPathExpression userRolesExpression;
    protected XPathExpression userNameExpression;
    protected XPathExpression userRolRefsExpression;
    protected XPathExpression userRolRefNameExpression;
    protected XPathExpression groupRolesExpression;
    protected XPathExpression groupNameExpression;
    protected XPathExpression groupRolRefsExpression;
    protected XPathExpression groupRolRefNameExpression;

    /** Constructor is protected, use the static Singleton instance */
    protected RoleXMLXpath_1_0() {

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(this.rrContext);
        // versionExpression = compile(xpath,"/"+E_USERREGISTRY+"["+A_VERSION_UR + "]");

        roleListExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_RR
                                + ":"
                                + E_ROLEREGISTRY_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_ROLELIST_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_ROLE_RR);
        parentExpression = compileRelativeAttribute(xpath, A_PARENTID_RR, NSP_RR);
        roleNameExpression = compileRelativeAttribute(xpath, A_ROLEID_RR, NSP_RR);

        rolePropertiesExpression = compile(xpath, NSP_RR + ":" + E_PROPERTY_RR);
        propertyNameExpression = compileRelativeAttribute(xpath, A_PROPERTY_NAME_RR, NSP_RR);
        propertyValueExpression = compile(xpath, "text()");

        userRolesExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_RR
                                + ":"
                                + E_ROLEREGISTRY_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_USERLIST_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_USERROLES_RR);
        userNameExpression = compileRelativeAttribute(xpath, A_USERNAME_RR, NSP_RR);
        userRolRefsExpression = compile(xpath, NSP_RR + ":" + E_ROLEREF_RR);
        userRolRefNameExpression = compileRelativeAttribute(xpath, A_ROLEREFID_RR, NSP_RR);

        groupRolesExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_RR
                                + ":"
                                + E_ROLEREGISTRY_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_GROUPLIST_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_GROUPROLES_RR);
        groupNameExpression = compileRelativeAttribute(xpath, A_GROUPNAME_RR, NSP_RR);
        groupRolRefsExpression = compile(xpath, NSP_RR + ":" + E_ROLEREF_RR);
        groupRolRefNameExpression = compileRelativeAttribute(xpath, A_ROLEREFID_RR, NSP_RR);
    }

    public XPathExpression getRoleListExpression() {
        return roleListExpression;
    }

    public XPathExpression getParentExpression() {
        return parentExpression;
    }

    public XPathExpression getRoleNameExpression() {
        return roleNameExpression;
    }

    public XPathExpression getRolePropertiesExpression() {
        return rolePropertiesExpression;
    }

    public XPathExpression getPropertyNameExpression() {
        return propertyNameExpression;
    }

    public XPathExpression getPropertyValueExpression() {
        return propertyValueExpression;
    }

    public XPathExpression getUserRolesExpression() {
        return userRolesExpression;
    }

    public XPathExpression getUserNameExpression() {
        return userNameExpression;
    }

    public XPathExpression getUserRolRefsExpression() {
        return userRolRefsExpression;
    }

    public XPathExpression getUserRolRefNameExpression() {
        return userRolRefNameExpression;
    }

    public XPathExpression getGroupRolesExpression() {
        return groupRolesExpression;
    }

    public XPathExpression getGroupNameExpression() {
        return groupNameExpression;
    }

    public XPathExpression getGroupRolRefsExpression() {
        return groupRolRefsExpression;
    }

    public XPathExpression getGroupRolRefNameExpression() {
        return groupRolRefNameExpression;
    }
}
