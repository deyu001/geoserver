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

package org.geoserver.security.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SortedSet;
import java.util.TreeMap;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;

/**
 * Implementation for testing uses serialization into a byte array
 *
 * @author christian
 */
public class MemoryUserGroupService extends AbstractUserGroupService {

    byte[] byteArray;
    protected String toBeEncrypted;

    public String getToBeEncrypted() {
        return toBeEncrypted;
    }

    public MemoryUserGroupService() {}

    @Override
    public boolean canCreateStore() {
        return true;
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        MemoryUserGroupStore store = new MemoryUserGroupStore();
        store.initializeFromService(this);
        return store;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void deserialize() throws IOException {
        clearMaps();
        if (byteArray == null) return;
        ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
        ObjectInputStream oin = new ObjectInputStream(in);
        try {
            helper.userMap = (TreeMap<String, GeoServerUser>) oin.readObject();
            helper.groupMap = (TreeMap<String, GeoServerUserGroup>) oin.readObject();
            helper.user_groupMap =
                    (TreeMap<GeoServerUser, SortedSet<GeoServerUserGroup>>) oin.readObject();
            helper.group_userMap =
                    (TreeMap<GeoServerUserGroup, SortedSet<GeoServerUser>>) oin.readObject();
            helper.propertyMap = (TreeMap<String, SortedSet<GeoServerUser>>) oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        GeoServerUser user = new MemoryGeoserverUser(username, this);
        user.setEnabled(isEnabled);
        user.setPassword(password);
        return user;
    }

    @Override
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        GeoServerUserGroup group = new MemoryGeoserverUserGroup(groupname);
        group.setEnabled(isEnabled);
        return group;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        this.name = config.getName();
        SecurityUserGroupServiceConfig ugConfig = (SecurityUserGroupServiceConfig) config;
        passwordEncoderName = ugConfig.getPasswordEncoderName();
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
        passwordValidatorName = ugConfig.getPasswordPolicyName();
        toBeEncrypted = (((MemoryUserGroupServiceConfigImpl) config).getToBeEncrypted());
    }
}
