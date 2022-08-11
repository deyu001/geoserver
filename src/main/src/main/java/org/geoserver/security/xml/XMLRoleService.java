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
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.FileBasedSecurityServiceConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.util.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLRoleService extends AbstractRoleService {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    protected DocumentBuilder builder;
    protected Resource roleResource;

    /** Validate against schema on load/store, default = true; */
    private boolean validatingXMLSchema = true;

    public XMLRoleService() throws IOException {
        super();
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

        super.initializeFromConfig(config);
        validatingXMLSchema = false;

        if (config instanceof XMLSecurityServiceConfig) {
            validatingXMLSchema = ((XMLSecurityServiceConfig) config).isValidating();

            // copy schema file
            Resource xsdFile = getConfigRoot().get(XMLConstants.FILE_RR_SCHEMA);
            if (xsdFile.getType() == Type.UNDEFINED) {
                IOUtils.copy(
                        getClass().getResourceAsStream(XMLConstants.FILE_RR_SCHEMA), xsdFile.out());
            }
        }

        if (config instanceof FileBasedSecurityServiceConfig) {
            String fileName = ((FileBasedSecurityServiceConfig) config).getFileName();

            File roleFile = new File(fileName);
            if (roleFile.isAbsolute()) {
                roleResource = Files.asResource(roleFile);
            } else {
                roleResource = getConfigRoot().get(fileName);
            }

            if (roleResource.getType() == Type.UNDEFINED) {
                IOUtils.copy(
                        getClass().getResourceAsStream("rolesTemplate.xml"), roleResource.out());
            }
        } else {
            throw new IOException("Cannot initialize from " + config.getClass().getName());
        }
        // load the data
        deserialize();
    }

    @Override
    public boolean canCreateStore() {
        return true;
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        XMLRoleStore store = new XMLRoleStore();
        store.initializeFromService(this);
        return store;
    }

    public boolean isValidatingXMLSchema() {
        return validatingXMLSchema;
    }

    public void setValidatingXMLSchema(boolean validatingXMLSchema) {
        this.validatingXMLSchema = validatingXMLSchema;
    }

    @Override
    protected void deserialize() throws IOException {

        try {
            Document doc = null;
            try (InputStream is = roleResource.in()) {
                doc = builder.parse(is);
            } catch (SAXException e) {
                throw new IOException(e);
            }
            if (isValidatingXMLSchema()) {
                XMLValidator.Singleton.validateRoleRegistry(doc);
            }

            XPathExpression expr = XMLXpathFactory.Singleton.getVersionExpressionRR();
            String versioNummer = expr.evaluate(doc);
            RoleXMLXpath xmlXPath = XMLXpathFactory.Singleton.getRoleXMLXpath(versioNummer);

            clearMaps();

            NodeList roleNodes =
                    (NodeList)
                            xmlXPath.getRoleListExpression().evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < roleNodes.getLength(); i++) {
                Node roleNode = roleNodes.item(i);

                String roleName = xmlXPath.getRoleNameExpression().evaluate(roleNode);
                NodeList propertyNodes =
                        (NodeList)
                                xmlXPath.getRolePropertiesExpression()
                                        .evaluate(roleNode, XPathConstants.NODESET);
                Properties roleProps = new Properties();
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Node propertyNode = propertyNodes.item(j);
                    String propertyName =
                            xmlXPath.getPropertyNameExpression().evaluate(propertyNode);
                    String propertyValue =
                            xmlXPath.getPropertyValueExpression().evaluate(propertyNode);
                    roleProps.put(propertyName, propertyValue);
                }
                GeoServerRole role = createRoleObject(roleName);

                role.getProperties().clear(); // set properties
                for (Object key : roleProps.keySet()) {
                    role.getProperties().put(key, roleProps.get(key));
                }
                helper.roleMap.put(roleName, role);
            }
            // second pass for hierarchy
            for (int i = 0; i < roleNodes.getLength(); i++) {
                Node roleNode = roleNodes.item(i);
                String roleName = xmlXPath.getRoleNameExpression().evaluate(roleNode);
                String parentName = xmlXPath.getParentExpression().evaluate(roleNode);
                if (parentName != null && parentName.length() > 0) {
                    helper.role_parentMap.put(
                            helper.roleMap.get(roleName), helper.roleMap.get(parentName));
                }
            }

            // user roles
            NodeList userRolesNodes =
                    (NodeList)
                            xmlXPath.getUserRolesExpression().evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < userRolesNodes.getLength(); i++) {
                Node userRolesNode = userRolesNodes.item(i);
                String userName = xmlXPath.getUserNameExpression().evaluate(userRolesNode);
                SortedSet<GeoServerRole> roleSet = new TreeSet<>();
                helper.user_roleMap.put(userName, roleSet);
                NodeList userRolesRefNodes =
                        (NodeList)
                                xmlXPath.getUserRolRefsExpression()
                                        .evaluate(userRolesNode, XPathConstants.NODESET);
                for (int j = 0; j < userRolesRefNodes.getLength(); j++) {
                    Node userRolesRefNode = userRolesRefNodes.item(j);
                    String roleRef =
                            xmlXPath.getUserRolRefNameExpression().evaluate(userRolesRefNode);
                    roleSet.add(helper.roleMap.get(roleRef));
                }
            }

            // group roles
            NodeList groupRolesNodes =
                    (NodeList)
                            xmlXPath.getGroupRolesExpression()
                                    .evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < groupRolesNodes.getLength(); i++) {
                Node groupRolesNode = groupRolesNodes.item(i);
                String groupName = xmlXPath.getGroupNameExpression().evaluate(groupRolesNode);
                SortedSet<GeoServerRole> roleSet = new TreeSet<>();
                helper.group_roleMap.put(groupName, roleSet);
                NodeList groupRolesRefNodes =
                        (NodeList)
                                xmlXPath.getGroupRolRefsExpression()
                                        .evaluate(groupRolesNode, XPathConstants.NODESET);
                for (int j = 0; j < groupRolesRefNodes.getLength(); j++) {
                    Node groupRolesRefNode = groupRolesRefNodes.item(j);
                    String roleRef =
                            xmlXPath.getGroupRolRefNameExpression().evaluate(groupRolesRefNode);
                    roleSet.add(helper.roleMap.get(roleRef));
                }
            }
        } catch (XPathExpressionException ex) {
            throw new IOException(ex);
        }
    }
}
