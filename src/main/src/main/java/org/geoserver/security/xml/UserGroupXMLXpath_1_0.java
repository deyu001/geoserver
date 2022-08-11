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

import static org.geoserver.security.xml.XMLConstants.A_GROUP_ENABLED_UR;
import static org.geoserver.security.xml.XMLConstants.A_GROUP_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_MEMBER_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_PROPERTY_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_USER_ENABLED_UR;
import static org.geoserver.security.xml.XMLConstants.A_USER_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_USER_PASSWORD_UR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPS_UR;
import static org.geoserver.security.xml.XMLConstants.E_GROUP_UR;
import static org.geoserver.security.xml.XMLConstants.E_MEMBER_UR;
import static org.geoserver.security.xml.XMLConstants.E_PROPERTY_UR;
import static org.geoserver.security.xml.XMLConstants.E_USERREGISTRY_UR;
import static org.geoserver.security.xml.XMLConstants.E_USERS_UR;
import static org.geoserver.security.xml.XMLConstants.E_USER_UR;
import static org.geoserver.security.xml.XMLConstants.NSP_UR;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * This class provides precompiled XPath Expressions
 *
 * @author christian
 */
public class UserGroupXMLXpath_1_0 extends UserGroupXMLXpath {

    /** Singleton, the implementation is stateless */
    public static final UserGroupXMLXpath_1_0 Singleton = new UserGroupXMLXpath_1_0();

    /** XML name space context for user/group store */
    protected XPathExpression userListExpression;

    protected XPathExpression userEnabledExpression;
    protected XPathExpression userNameExpression;
    protected XPathExpression userPasswordExpression;
    protected XPathExpression userPropertiesExpression;
    protected XPathExpression propertyNameExpression;
    protected XPathExpression propertyValueExpression;
    protected XPathExpression groupListExpression;
    protected XPathExpression groupNameExpression;
    protected XPathExpression groupEnabledExpression;
    protected XPathExpression groupMemberListExpression;
    protected XPathExpression groupMemberNameExpression;

    /** Constructor is protected, use the static Singleton instance */
    protected UserGroupXMLXpath_1_0() {

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(this.urContext);

        // compile(xpath,"/"+E_USERREGISTRY+"["+A_VERSION_UR + "]");

        userListExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_UR
                                + ":"
                                + E_USERREGISTRY_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_USERS_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_USER_UR);
        userEnabledExpression = compileRelativeAttribute(xpath, A_USER_ENABLED_UR, NSP_UR);
        userNameExpression = compileRelativeAttribute(xpath, A_USER_NAME_UR, NSP_UR);
        userPasswordExpression = compileRelativeAttribute(xpath, A_USER_PASSWORD_UR, NSP_UR);

        userPropertiesExpression = compile(xpath, NSP_UR + ":" + E_PROPERTY_UR);
        propertyNameExpression = compileRelativeAttribute(xpath, A_PROPERTY_NAME_UR, NSP_UR);
        propertyValueExpression = compile(xpath, "text()");

        groupListExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_UR
                                + ":"
                                + E_USERREGISTRY_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_GROUPS_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_GROUP_UR);

        groupNameExpression = compileRelativeAttribute(xpath, A_GROUP_NAME_UR, NSP_UR);
        groupEnabledExpression = compileRelativeAttribute(xpath, A_GROUP_ENABLED_UR, NSP_UR);

        groupMemberListExpression = compile(xpath, NSP_UR + ":" + E_MEMBER_UR);
        groupMemberNameExpression = compileRelativeAttribute(xpath, A_MEMBER_NAME_UR, NSP_UR);
    }

    @Override
    public XPathExpression getUserListExpression() {
        return userListExpression;
    }

    @Override
    public XPathExpression getUserEnabledExpression() {
        return userEnabledExpression;
    }

    @Override
    public XPathExpression getUserNameExpression() {
        return userNameExpression;
    }

    @Override
    public XPathExpression getUserPasswordExpression() {
        return userPasswordExpression;
    }

    @Override
    public XPathExpression getUserPropertiesExpression() {
        return userPropertiesExpression;
    }

    @Override
    public XPathExpression getPropertyNameExpression() {
        return propertyNameExpression;
    }

    @Override
    public XPathExpression getPropertyValueExpression() {
        return propertyValueExpression;
    }

    @Override
    public XPathExpression getGroupListExpression() {
        return groupListExpression;
    }

    @Override
    public XPathExpression getGroupNameExpression() {
        return groupNameExpression;
    }

    @Override
    public XPathExpression getGroupEnabledExpression() {
        return groupEnabledExpression;
    }

    @Override
    public XPathExpression getGroupMemberListExpression() {
        return groupMemberListExpression;
    }

    @Override
    public XPathExpression getGroupMemberNameExpression() {
        return groupMemberNameExpression;
    }
}
