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

package org.geoserver.importer.web;

import static org.geotools.jdbc.JDBCDataStoreFactory.DATABASE;
import static org.geotools.jdbc.JDBCDataStoreFactory.HOST;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PK_METADATA_TABLE;
import static org.geotools.jdbc.JDBCDataStoreFactory.PORT;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.wicket.Component;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.sqlserver.SQLServerDataStoreFactory;
import org.geotools.data.sqlserver.SQLServerJNDIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

public class SQLServerPanel extends AbstractDbPanel {

    JNDIDbParamPanel jndiParamPanel;
    BasicDbParamPanel basicParamPanel;

    public SQLServerPanel(String id) {
        super(id);
    }

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels() {
        LinkedHashMap<String, Component> result = new LinkedHashMap<>();

        // basic panel
        basicParamPanel = new BasicDbParamPanel("01", "localhost", 4866, true);
        result.put(CONNECTION_DEFAULT, basicParamPanel);

        // jndi param panels
        jndiParamPanel = new JNDIDbParamPanel("03", "java:comp/env/jdbc/mydatabase");
        result.put(CONNECTION_JNDI, jndiParamPanel);

        return result;
    }

    @Override
    protected DataStoreFactorySpi fillStoreParams(Map<String, Serializable> params) {
        DataStoreFactorySpi factory;
        params.put(JDBCDataStoreFactory.DBTYPE.key, "sqlserver");
        if (CONNECTION_JNDI.equals(connectionType)) {
            factory = new SQLServerJNDIDataStoreFactory();

            fillInJndiParams(params, jndiParamPanel);
        } else {
            factory = new SQLServerDataStoreFactory();

            // basic params
            params.put(HOST.key, basicParamPanel.host);
            params.put(PORT.key, basicParamPanel.port);
            params.put(USER.key, basicParamPanel.username);
            params.put(PASSWD.key, basicParamPanel.password);
            params.put(DATABASE.key, basicParamPanel.database);
        }
        if (!CONNECTION_JNDI.equals(connectionType)) {
            // connection pool params common to OCI and default connections
            fillInConnPoolParams(params, basicParamPanel);
        }
        params.put(SQLServerDataStoreFactory.INTSEC.key, false);
        /*
        OtherDbmsParamPanel otherParamsPanel = (OtherDbmsParamPanel) this.otherParamsPanel;
        if(otherParamsPanel.userSchema) {
            params.put(JDBCDataStoreFactory.SCHEMA.key, ((String) params.get(USER.key)).toUpperCase());
        } else {
            params.put(JDBCDataStoreFactory.SCHEMA.key, otherParamsPanel.schema);
        }
        */
        if (basicParamPanel.schema != null) {
            params.put(JDBCDataStoreFactory.SCHEMA.key, basicParamPanel.schema);
        }
        // params.put(NAMESPACE.key, new URI(namespace.getURI()).toString());
        // params.put(SQLServerDataStoreFactory.LOOSEBBOX.key, advancedParamPanel.looseBBox);
        params.put(PK_METADATA_TABLE.key, advancedParamPanel.pkMetadata);
        return factory;
    }
}
