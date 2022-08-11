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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.config.FileBasedSecurityServiceConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.Util;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;
import org.geoserver.util.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** @author christian */
public class XMLUserGroupService extends AbstractUserGroupService {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    protected DocumentBuilder builder;
    protected Resource userResource;
    /** Validate against schema on load/store, default = true; */
    private boolean validatingXMLSchema = true;

    public XMLUserGroupService() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {

        this.name = config.getName();
        validatingXMLSchema = false;
        passwordEncoderName = ((SecurityUserGroupServiceConfig) config).getPasswordEncoderName();
        passwordValidatorName = ((SecurityUserGroupServiceConfig) config).getPasswordPolicyName();

        GeoServerPasswordEncoder enc =
                getSecurityManager().loadPasswordEncoder(passwordEncoderName);
        if (enc.getEncodingType() == PasswordEncodingType.ENCRYPT) {
            KeyStoreProvider prov = getSecurityManager().getKeyStoreProvider();
            String alias = prov.aliasForGroupService(name);
            if (prov.containsAlias(alias) == false) {
                prov.setUserGroupKey(
                        name,
                        getSecurityManager()
                                .getRandomPassworddProvider()
                                .getRandomPasswordWithDefaultLength());
                prov.storeKeyStore();
            }
        }
        enc.initializeFor(this);

        if (config instanceof XMLSecurityServiceConfig) {
            validatingXMLSchema = ((XMLSecurityServiceConfig) config).isValidating();
            // copy schema file
            Resource xsdFile = getConfigRoot().get(XMLConstants.FILE_UR_SCHEMA);
            if (xsdFile.getType() == Type.UNDEFINED) {
                IOUtils.copy(
                        getClass().getResourceAsStream(XMLConstants.FILE_UR_SCHEMA), xsdFile.out());
            }
        }

        if (config instanceof FileBasedSecurityServiceConfig) {
            String fileName = ((FileBasedSecurityServiceConfig) config).getFileName();
            File userFile = new File(fileName);
            if (userFile.isAbsolute()) {
                userResource = Files.asResource(userFile);
            } else {
                userResource = getConfigRoot().get(fileName);
            }

            if (userResource.getType() == Type.UNDEFINED) {
                IOUtils.copy(
                        getClass().getResourceAsStream("usersTemplate.xml"), userResource.out());
            }
        } else {
            throw new IOException("Cannot initialize from " + config.getClass().getName());
        }
        deserialize();
    }

    @Override
    public boolean canCreateStore() {
        return true;
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        XMLUserGroupStore store = new XMLUserGroupStore();
        store.initializeFromService(this);
        return store;
    }

    public boolean isValidatingXMLSchema() {
        return validatingXMLSchema;
    }

    public void setValidatingXMLSchema(boolean validatingXMLSchema) {
        this.validatingXMLSchema = validatingXMLSchema;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.AbstractUserGroupService#deserialize()
     */
    @Override
    protected void deserialize() throws IOException {

        try {

            Document doc = null;
            try (InputStream is = userResource.in()) {
                doc = builder.parse(is);
            } catch (SAXException e) {
                throw new IOException(e);
            }

            if (isValidatingXMLSchema()) {
                XMLValidator.Singleton.validateUserGroupRegistry(doc);
            }

            XPathExpression expr = XMLXpathFactory.Singleton.getVersionExpressionUR();
            String versionNummer = expr.evaluate(doc);
            UserGroupXMLXpath xmlXPath =
                    XMLXpathFactory.Singleton.getUserGroupXMLXpath(versionNummer);

            clearMaps();

            NodeList userNodes =
                    (NodeList)
                            xmlXPath.getUserListExpression().evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < userNodes.getLength(); i++) {
                Node userNode = userNodes.item(i);
                boolean userEnabled =
                        Util.convertToBoolean(
                                xmlXPath.getUserEnabledExpression().evaluate(userNode), true);
                String userPassword = null;

                // there doesn't seem to be a way to check for existence of an attribute vs an
                // attribute being empty, so we check the attribute manually
                if (userNode.getAttributes().getNamedItem(XMLConstants.A_USER_PASSWORD_UR)
                        != null) {
                    userPassword = xmlXPath.getUserPasswordExpression().evaluate(userNode);
                }

                String userName = xmlXPath.getUserNameExpression().evaluate(userNode);
                NodeList propertyNodes =
                        (NodeList)
                                xmlXPath.getUserPropertiesExpression()
                                        .evaluate(userNode, XPathConstants.NODESET);
                Properties userProps = new Properties();
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Node propertyNode = propertyNodes.item(j);
                    String propertyName =
                            xmlXPath.getPropertyNameExpression().evaluate(propertyNode);
                    String propertyValue =
                            xmlXPath.getPropertyValueExpression().evaluate(propertyNode);
                    userProps.put(propertyName, propertyValue);
                }
                GeoServerUser user = createUserObject(userName, userPassword, userEnabled);

                helper.userMap.put(user.getUsername(), user);
                user.getProperties().clear(); // set properties
                for (Object key : userProps.keySet()) {
                    user.getProperties().put(key, userProps.get(key));
                    SortedSet<GeoServerUser> propUsers = helper.propertyMap.get(key);
                    if (propUsers == null) {
                        propUsers = new TreeSet<>();
                        helper.propertyMap.put((String) key, propUsers);
                    }
                    propUsers.add(user);
                }
            }

            NodeList groupNodes =
                    (NodeList)
                            xmlXPath.getGroupListExpression().evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < groupNodes.getLength(); i++) {
                Node groupNode = groupNodes.item(i);
                String groupName = xmlXPath.getGroupNameExpression().evaluate(groupNode);
                boolean groupEnabled =
                        Util.convertToBoolean(
                                xmlXPath.getGroupEnabledExpression().evaluate(groupNode), true);
                GeoServerUserGroup group = createGroupObject(groupName, groupEnabled);
                helper.groupMap.put(groupName, group);
                NodeList memberNodes =
                        (NodeList)
                                xmlXPath.getGroupMemberListExpression()
                                        .evaluate(groupNode, XPathConstants.NODESET);
                for (int j = 0; j < memberNodes.getLength(); j++) {
                    Node memberNode = memberNodes.item(j);
                    String memberName =
                            xmlXPath.getGroupMemberNameExpression().evaluate(memberNode);
                    GeoServerUser member = helper.userMap.get(memberName);

                    SortedSet<GeoServerUser> members = helper.group_userMap.get(group);
                    if (members == null) {
                        members = new TreeSet<>();
                        helper.group_userMap.put(group, members);
                    }
                    members.add(member);

                    SortedSet<GeoServerUserGroup> userGroups = helper.user_groupMap.get(member);
                    if (userGroups == null) {
                        userGroups = new TreeSet<>();
                        helper.user_groupMap.put(member, userGroups);
                    }
                    userGroups.add(group);
                }
            }
        } catch (XPathExpressionException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        XMLGeoserverUser user = new XMLGeoserverUser(username);
        user.setEnabled(isEnabled);
        user.setPassword(password);
        return user;
    }
}
