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

package org.geoserver.security.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleHierarchyHelper;

/**
 * JDBC Implementation of {@link GeoServerRoleStore}
 *
 * @author christian
 */
public class JDBCRoleStore extends JDBCRoleService implements GeoServerRoleStore {

    protected boolean modified;
    protected Connection connection;

    /**
     * The identical connection is used until {@link #store()} or {@link #load()} is called. Within
     * a transaction it is not possible to use different connections.
     *
     * @see org.geoserver.security.jdbc.AbstractJDBCService#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null) connection = super.getConnection();
        return connection;
    }

    @Override
    protected void closeConnection(Connection con) throws SQLException {
        // do nothing
    }

    /** To be called at the the end of a transaction, frees the current {@link Connection} */
    protected void releaseConnection() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    /**
     * Executes {@link Connection#rollback()} and frees the connection object
     *
     * @see org.geoserver.security.jdbc.JDBCRoleService#load()
     */
    public void load() throws IOException {
        // Simply roll back the transaction
        try {
            getConnection().rollback();
            releaseConnection();

        } catch (SQLException ex) {
            throw new IOException(ex);
        }
        setModified(false);
    }

    protected void addRoleProperties(GeoServerRole role, Connection con)
            throws SQLException, IOException {
        if (role.getProperties().size() == 0) return; // nothing to do

        PreparedStatement ps = getDMLStatement("roleprops.insert", con);
        try {
            for (Object key : role.getProperties().keySet()) {
                Object propertyVal = role.getProperties().get(key);
                ps.setString(1, role.getAuthority());
                ps.setString(2, key.toString());
                ps.setObject(3, propertyVal);
                ps.execute();
            }
        } finally {
            closeFinally(null, ps, null);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#addRole(org.geoserver.security.impl.GeoserverRole)
     */
    public void addRole(GeoServerRole role) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("roles.insert", con);
            ps.setString(1, role.getAuthority());
            // ps.setNull(2, Types.VARCHAR);
            ps.execute();

            addRoleProperties(role, con);

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#updateRole(org.geoserver.security.impl.GeoserverRole)
     */
    public void updateRole(GeoServerRole role) throws IOException {

        // No attributes for update
        Connection con = null;
        PreparedStatement ps = null;
        try {

            con = getConnection();
            ps = getDMLStatement("roles.update", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("roleprops.deleteForRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            addRoleProperties(role, con);

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true); // we do as if there was an update
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#removeRole(org.geoserver.security.impl.GeoserverRole)
     */
    public boolean removeRole(GeoServerRole role) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        boolean retval = false;
        try {
            con = getConnection();
            ps = getDMLStatement("roles.delete", con);
            ps.setString(1, role.getAuthority());
            ps.execute();
            retval = ps.getUpdateCount() > 0;

            ps.close();
            ps = getDMLStatement("userroles.deleteRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("grouproles.deleteRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("roleprops.deleteForRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("roles.deleteParent", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
        return retval;
    }

    /**
     * Executes {@link Connection#commit()} and frees the connection
     *
     * @see org.geoserver.security.GeoServerRoleStore#store()
     */
    public void store() throws IOException {
        // Simply commit the transaction
        try {
            getConnection().commit();
            releaseConnection();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
        setModified(false);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#associateRoleToUser(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("userroles.insert", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, username);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#disAssociateRoleFromUser(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("userroles.delete", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, username);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#associateRoleToGroup(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("grouproles.insert", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, groupname);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#disAssociateRoleFromGroup(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("grouproles.delete", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, groupname);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#setParentRole(org.geoserver.security.impl.GeoserverRole, org.geoserver.security.impl.GeoserverRole)
     */
    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {

        RoleHierarchyHelper helper = new RoleHierarchyHelper(getParentMappings());
        if (helper.isValidParent(
                        role.getAuthority(), parentRole == null ? null : parentRole.getAuthority())
                == false)
            throw new IOException(
                    parentRole.getAuthority()
                            + " is not a valid parent for "
                            + role.getAuthority());

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("roles.parentUpdate", con);
            if (parentRole == null) ps.setNull(1, Types.VARCHAR);
            else ps.setString(1, parentRole.getAuthority());
            ps.setString(2, role.getAuthority());
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#clear()
     */
    public void clear() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("grouproles.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("userroles.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("roleprops.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("roles.deleteAll", con);
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#initializeFromService(org.geoserver.security.GeoserverRoleService)
     */
    public void initializeFromService(GeoServerRoleService service) throws IOException {
        JDBCRoleService jdbcService = (JDBCRoleService) service;
        this.name = service.getName();
        this.adminRoleName = jdbcService.adminRoleName;
        this.groupAdminRoleName = jdbcService.groupAdminRoleName;
        this.datasource = jdbcService.datasource;
        this.ddlProps = jdbcService.ddlProps;
        this.dmlProps = jdbcService.dmlProps;
        this.securityManager = service.getSecurityManager();
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
