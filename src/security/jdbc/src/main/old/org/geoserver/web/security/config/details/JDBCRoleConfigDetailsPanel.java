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

package org.geoserver.web.security.config.details;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.web.security.JDBCConnectFormComponent;
import org.geoserver.web.security.JDBCConnectFormComponent.JDBCConnectConfig;
import org.geoserver.web.security.JDBCConnectFormComponent.Mode;
import org.geoserver.web.security.config.SecurityNamedConfigModelHelper;

/**
 * A form component that can be used for xml configurations
 */
public class JDBCRoleConfigDetailsPanel extends AbstractRoleDetailsPanel{
    private static final long serialVersionUID = 1L;
    JDBCConnectFormComponent comp;
    TextField<String> propertyFileNameDDLComponent;
    TextField<String> propertyFileNameDMLComponent;
    CheckBox creatingTablesComponent;

    
    public JDBCRoleConfigDetailsPanel(String id, CompoundPropertyModel<SecurityNamedConfigModelHelper> model) {
        super(id,model);
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        if (configHelper.isNew()) {
            comp = new JDBCConnectFormComponent("jdbcConnectFormComponent",Mode.DYNAMIC);
        } else {
            JDBCSecurityServiceConfig config = (JDBCSecurityServiceConfig) configHelper.getConfig(); 
           if (config.isJndi()) {
               comp = new JDBCConnectFormComponent("jdbcConnectFormComponent",Mode.DYNAMIC,config.getJndiName());
           } else {
               comp = new JDBCConnectFormComponent("jdbcConnectFormComponent",Mode.DYNAMIC,
                       config.getDriverClassName(),config.getConnectURL(),
                       config.getUserName(),config.getPassword()
                       );
           }
        }        
        addOrReplace(comp);        
        
        add(creatingTablesComponent=new CheckBox("config.creatingTables"));
        
        propertyFileNameDDLComponent = new TextField<String>("config.propertyFileNameDDL");
        add(propertyFileNameDDLComponent);
        propertyFileNameDMLComponent = new TextField<String>("config.propertyFileNameDML");
        add(propertyFileNameDMLComponent);
        
        
    };
        
    
    @Override
    protected SecurityNamedServiceConfig createNewConfigObject() {
        return new JDBCRoleServiceConfig();
    }
          
    @Override
    public void updateModel() {
        super.updateModel();
        comp.updateModel();
        creatingTablesComponent.updateModel();
        propertyFileNameDDLComponent.updateModel();
        propertyFileNameDMLComponent.updateModel();

        JDBCSecurityServiceConfig config = (JDBCSecurityServiceConfig) configHelper.getConfig();
        JDBCConnectConfig c = comp.getModelObject();
        config.setJndiName(null);
        config.setDriverClassName(null);
        config.setConnectURL(null);
        config.setUserName(null);
        config.setPassword(null);
        
        config.setJndi(c.getType().equals(JDBCConnectConfig.TYPEJNDI));
        if (config.isJndi()) {
            config.setJndiName(c.getJndiName());
        } else {
            config.setDriverClassName(c.getDriverName());
            config.setConnectURL(c.getConnectURL());
            config.setUserName(c.getUsername());
            config.setPassword(c.getPassword());
        }
    }
}
