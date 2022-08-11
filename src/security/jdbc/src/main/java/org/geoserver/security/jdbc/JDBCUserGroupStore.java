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
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.PasswordValidatorImpl;

/**
 * JDBC Implementation of {@link GeoServerUserGroupStore}
 *
 * @author christian
 */
public class JDBCUserGroupStore extends JDBCUserGroupService implements GeoServerUserGroupStore {

    public JDBCUserGroupStore() throws IOException {
        super();
    }

    protected boolean modified;
    protected Connection connection;
    protected JDBCUserGroupService jdbcService;

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
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#initializeFromServer(org.geoserver.security.GeoServerUserGroupService)
     */
    public void initializeFromService(GeoServerUserGroupService service) throws IOException {
        jdbcService = (JDBCUserGroupService) service;
        setSecurityManager(service.getSecurityManager());
        this.name = jdbcService.getName();
        this.passwordEncoderName = service.getPasswordEncoderName();
        this.passwordValidatorName = service.getPasswordValidatorName();
        this.datasource = jdbcService.datasource;
        this.ddlProps = jdbcService.ddlProps;
        this.dmlProps = jdbcService.dmlProps;
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new IOException();
        }
    }

    /**
     * Executes {@link Connection#rollback() }and frees the connection object
     *
     * @see org.geoserver.security.jdbc.JDBCUserGroupService#load()
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
        // fireUserGroupChangedEvent();
    }

    /** Helper method for inserting user properties */
    protected void addUserProperties(GeoServerUser user, Connection con)
            throws SQLException, IOException {
        if (user.getProperties().size() == 0) return; // nothing to do

        PreparedStatement ps = getDMLStatement("userprops.insert", con);
        try {
            for (Object key : user.getProperties().keySet()) {
                Object propertyVal = user.getProperties().get(key);
                ps.setString(1, user.getUsername());
                ps.setString(2, key.toString());
                ps.setObject(3, propertyVal);
                ps.execute();
            }
        } finally {
            closeFinally(null, ps, null);
        }
    }

    /**
     * validates and encodes the password. Do nothing for a not changed password of an existing user
     */
    protected void preparePassword(GeoServerUser user) throws IOException, PasswordPolicyException {

        char[] passwordArray = user.getPassword() != null ? user.getPassword().toCharArray() : null;

        if (PasswordValidatorImpl.passwordStartsWithEncoderPrefix(passwordArray) != null)
            return; // do nothing, password already encoded

        // we have a plain text password
        // validate it
        getSecurityManager()
                .loadPasswordValidator(getPasswordValidatorName())
                .validatePassword(passwordArray);

        // validation ok, initializer encoder and set encoded password
        GeoServerPasswordEncoder enc =
                getSecurityManager().loadPasswordEncoder(getPasswordEncoderName());

        enc.initializeFor(this);
        user.setPassword(enc.encodePassword(user.getPassword(), null));
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#addUser(org.geoserver.security.impl.GeoserverUser)
     */
    public void addUser(GeoServerUser user) throws IOException, PasswordPolicyException {

        preparePassword(user);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("users.insert", con);
            ps.setString(1, user.getUsername());
            if (user.getPassword() != null) {
                ps.setString(2, user.getPassword());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }

            ps.setString(3, convertToString(user.isEnabled()));
            ps.execute();

            addUserProperties(user, con);

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#updateUser(org.geoserver.security.impl.GeoserverUser)
     */
    public void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException {

        preparePassword(user);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("users.update", con);
            ps.setString(1, user.getPassword());
            ps.setString(2, convertToString(user.isEnabled()));
            ps.setString(3, user.getUsername());
            ps.execute();

            ps.close();
            ps = getDMLStatement("userprops.deleteForUser", con);
            ps.setString(1, user.getUsername());
            ps.execute();

            addUserProperties(user, con);

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#removeUser(org.geoserver.security.impl.GeoserverUser)
     */
    public boolean removeUser(GeoServerUser user) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        boolean retval = false;
        try {
            con = getConnection();
            ps = getDMLStatement("users.delete", con);
            ps.setString(1, user.getUsername());
            ps.execute();
            retval = ps.getUpdateCount() > 0;

            ps.close();
            ps = getDMLStatement("userprops.deleteForUser", con);
            ps.setString(1, user.getUsername());
            ps.execute();

            ps.close();
            ps = getDMLStatement("groupmembers.deleteUser", con);
            ps.setString(1, user.getUsername());
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
        return retval;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#addGroup(org.geoserver.security.impl.GeoserverUserGroup)
     */
    public void addGroup(GeoServerUserGroup group) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("groups.insert", con);
            ps.setString(1, group.getGroupname());
            ps.setString(2, convertToString(group.isEnabled()));
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#updateGroup(org.geoserver.security.impl.GeoserverUserGroup)
     */
    public void updateGroup(GeoServerUserGroup group) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("groups.update", con);
            ps.setString(1, convertToString(group.isEnabled()));
            ps.setString(2, group.getGroupname());
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#removeGroup(org.geoserver.security.impl.GeoserverUserGroup)
     */
    public boolean removeGroup(GeoServerUserGroup group) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        boolean retval = false;
        try {
            con = getConnection();
            ps = getDMLStatement("groups.delete", con);
            ps.setString(1, group.getGroupname());
            ps.execute();
            retval = ps.getUpdateCount() > 0;

            ps.close();
            ps = getDMLStatement("groupmembers.deleteGroup", con);
            ps.setString(1, group.getGroupname());
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
     * @see org.geoserver.security.GeoServerUserGroupStore#store()
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
        // fireUserGroupChangedEvent();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#associateUserToGroup(org.geoserver.security.impl.GeoserverUser, org.geoserver.security.impl.GeoserverUserGroup)
     */
    public void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("groupmembers.insert", con);
            ps.setString(1, group.getGroupname());
            ps.setString(2, user.getUsername());
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#disAssociateUserFromGroup(org.geoserver.security.impl.GeoserverUser, org.geoserver.security.impl.GeoserverUserGroup)
     */
    public void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("groupmembers.delete", con);
            ps.setString(1, group.getGroupname());
            ps.setString(2, user.getUsername());
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
     * @see org.geoserver.security.GeoserverUserGroupStore#clear()
     */
    public void clear() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("groupmembers.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("groups.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("userprops.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("users.deleteAll", con);
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }
    /** Delegates to the {@link GeoServerUserGroupService} backend */
    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        return jdbcService.createUserObject(username, password, isEnabled);
    }
}
