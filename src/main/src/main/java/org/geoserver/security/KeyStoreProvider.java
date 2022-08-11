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

package org.geoserver.security;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import org.geoserver.platform.resource.Resource;

public interface KeyStoreProvider {

    /** Sets the security manager facade. */
    void setSecurityManager(GeoServerSecurityManager securityManager);

    /** @return the default key store {@link Resource} object */
    Resource getResource();

    /** Forces a reload of the key store */
    void reloadKeyStore() throws IOException;

    /** Gets the {@link Key} object for this alias <code>null</code> if the alias does not exist */
    Key getKey(String alias) throws IOException;

    /**
     * Gets the key for encrypting passwords stored in configuration files, may be <code>null</code>
     */
    byte[] getConfigPasswordKey() throws IOException;

    /** Checks if a such a key is available without presenting the key itself */
    boolean hasConfigPasswordKey() throws IOException;

    /** Test it the key store contains a alias */
    boolean containsAlias(String alias) throws IOException;

    /**
     * Returns the key for a {@link org.geoserver.security.GeoServerUserGroupService} service Name.
     * Needed if the service uses symmetric password encryption
     *
     * <p>may be <code>null</code>
     */
    byte[] getUserGroupKey(String serviceName) throws IOException;

    /** Checks if a such a key is available without presenting the key itself */
    boolean hasUserGroupKey(String serviceName) throws IOException;

    /**
     * Gets the {@link SecretKey} object for this alias <code>null</code> if the alias does not
     * exist
     *
     * @throws IOException if the key exists but has the wrong type
     */
    SecretKey getSecretKey(String name) throws IOException;

    /**
     * Gets the {@link SecretKey} object for this alias <code>null</code> if the alias does not
     * exist
     *
     * @throws IOException if the key exists but has the wrong type
     */
    PublicKey getPublicKey(String name) throws IOException;

    /**
     * Gets the {@link PrivateKey} object for this alias <code>null</code> if the alias does not
     * exist
     *
     * @throws IOException if the key exists but has the wrong type
     */
    PrivateKey getPrivateKey(String name) throws IOException;

    /**
     * @param serviceName for a {@link org.geoserver.security.GeoServerUserGroupService}
     * @return the following String {@link #USERGROUP_PREFIX}+serviceName+{@value
     *     #USERGROUP_POSTFIX}
     */
    String aliasForGroupService(String serviceName);

    /** Tests if the password is the key store password */
    boolean isKeyStorePassword(char[] password) throws IOException;

    /** Adds/replaces a {@link SecretKey} with its alias */
    void setSecretKey(String alias, char[] key) throws IOException;

    /** Sets a secret for the name of a {@link org.geoserver.security.GeoServerUserGroupService} */
    void setUserGroupKey(String serviceName, char[] password) throws IOException;

    /** Remove a key belonging to the alias */
    void removeKey(String alias) throws IOException;

    /** Stores the key store to {@link #ks} */
    void storeKeyStore() throws IOException;

    /**
     * Prepares a master password change. The new password is used to encrypt the {@link KeyStore}
     * and each {@link Entry};
     *
     * <p>The new password is assumed to be already validated by the {@link PasswordValidator} named
     * {@link PasswordValidator#MASTERPASSWORD_NAME}
     *
     * <p>A new key store named {@link #PREPARED_FILE_NAME} is created. All keys a re-encrypted with
     * the new password and stored in the new key store.
     */
    void prepareForMasterPasswordChange(char[] oldPassword, char[] newPassword) throws IOException;

    /** Aborts the master password change by removing the file named {@link #PREPARED_FILE_NAME} */
    void abortMasterPasswordChange();

    /**
     * if {@link #DEFAULT_FILE_NAME} and {@link #PREPARED_FILE_NAME} exist, this method checks if
     * {@link #PREPARED_FILE_NAME} can be used with new {@link
     * MasterPasswordProvider#getMasterPassword()} method.
     *
     * <p>YES: replace the old keystore with the new one
     *
     * <p>NO: Do nothing, log the problem and use the old configuration A reason may be that the new
     * master password is not properly injected at startup
     */
    void commitMasterPasswordChange() throws IOException;
}
