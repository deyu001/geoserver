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

package org.geoserver.importer;

import java.sql.Statement;
import org.junit.Ignore;

@Ignore
public class OracleImportTest extends ImporterDbTestBase {

    @Override
    protected String getFixtureId() {
        return "oracle";
    }

    @Override
    protected String tableName(String name) {
        return name.toUpperCase();
    }

    @Override
    protected void dropTable(String tableName, Statement st) throws Exception {
        runSafe("DROP TABLE " + tableName(tableName) + " PURGE", st);
        runSafe("DROP SEQUENCE " + tableName(tableName) + "_PKEY_SEQ", st);
        run(
                "DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = '"
                        + tableName(tableName)
                        + "'",
                st);
    }

    protected void createWidgetsTable(Statement st) throws Exception {
        String sql =
                "CREATE TABLE WIDGETS ("
                        + "ID INT, GEOMETRY MDSYS.SDO_GEOMETRY, "
                        + "PRICE FLOAT, DESCRIPTION VARCHAR(255), "
                        + "PRIMARY KEY(id))";
        run(sql, st);

        sql = "CREATE SEQUENCE WIDGETS_PKEY_SEQ";
        run(sql, st);

        sql =
                "INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID ) "
                        + "VALUES ('WIDGETS','GEOMETRY', MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',-180,180,0.5), "
                        + "MDSYS.SDO_DIM_ELEMENT('Y',-90,90,0.5)), 4326)";
        run(sql, st);

        sql =
                "CREATE INDEX WIDGETS_GEOMETRY_IDX ON WIDGETS(GEOMETRY) "
                        + "INDEXTYPE IS MDSYS.SPATIAL_INDEX PARAMETERS ('SDO_INDX_DIMS=2 LAYER_GTYPE=\"POINT\"')";
        run(sql, st);

        sql =
                "INSERT INTO WIDGETS VALUES (0,"
                        + "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(0.0,0.0,NULL),NULL,NULL), 1.99, 'anvil')";
        run(sql, st);

        sql =
                "INSERT INTO WIDGETS VALUES (1,"
                        + "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(1.0,1.0,NULL),NULL,NULL), 2.99, 'bomb')";
        run(sql, st);

        sql =
                "INSERT INTO WIDGETS VALUES (2,"
                        + "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(2.0,2.0,NULL),NULL,NULL), 3.99, 'dynamite')";
        run(sql, st);
    };
}
