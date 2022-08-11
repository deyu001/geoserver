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

package org.geoserver.security.concurrent;

import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.SecretKey;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.KeyStoreProvider;

/**
 * Locking wrapper for {@link KeyStoreProviderImpl}
 *
 * @author christian
 */
public class LockingKeyStoreProvider implements KeyStoreProvider {

    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();
    protected KeyStoreProvider provider;

    /** get a read lock */
    protected void readLock() {
        readLock.lock();
    }

    /** free read lock */
    protected void readUnLock() {
        readLock.unlock();
    }

    /** get a write lock */
    protected void writeLock() {
        writeLock.lock();
    }

    /** free write lock */
    protected void writeUnLock() {
        writeLock.unlock();
    }

    public LockingKeyStoreProvider(KeyStoreProvider prov) {
        this.provider = prov;
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        provider.setSecurityManager(securityManager);
    }

    @Override
    public Resource getResource() {
        return provider.getResource();
    }

    public void reloadKeyStore() throws IOException {
        writeLock();
        try {
            provider.reloadKeyStore();
        } finally {
            writeUnLock();
        }
    }

    public Key getKey(String alias) throws IOException {
        readLock();
        try {
            return provider.getKey(alias);
        } finally {
            readUnLock();
        }
    }

    public byte[] getConfigPasswordKey() throws IOException {
        readLock();
        try {
            return provider.getConfigPasswordKey();
        } finally {
            readUnLock();
        }
    }

    public boolean hasConfigPasswordKey() throws IOException {
        readLock();
        try {
            return provider.hasConfigPasswordKey();
        } finally {
            readUnLock();
        }
    }

    public boolean containsAlias(String alias) throws IOException {
        readLock();
        try {
            return provider.containsAlias(alias);
        } finally {
            readUnLock();
        }
    }

    public byte[] getUserGroupKey(String serviceName) throws IOException {
        readLock();
        try {
            return provider.getUserGroupKey(serviceName);
        } finally {
            readUnLock();
        }
    }

    public boolean hasUserGroupKey(String serviceName) throws IOException {
        readLock();
        try {
            return provider.hasUserGroupKey(serviceName);
        } finally {
            readUnLock();
        }
    }

    public SecretKey getSecretKey(String name) throws IOException {
        readLock();
        try {
            return provider.getSecretKey(name);
        } finally {
            readUnLock();
        }
    }

    public PublicKey getPublicKey(String name) throws IOException {
        readLock();
        try {
            return provider.getPublicKey(name);
        } finally {
            readUnLock();
        }
    }

    public PrivateKey getPrivateKey(String name) throws IOException {
        readLock();
        try {
            return provider.getPrivateKey(name);
        } finally {
            readUnLock();
        }
    }

    public String aliasForGroupService(String serviceName) {
        return provider.aliasForGroupService(serviceName);
    }

    public boolean isKeyStorePassword(char[] password) throws IOException {
        readLock();
        try {
            return provider.isKeyStorePassword(password);
        } finally {
            readUnLock();
        }
    }

    public void setSecretKey(String alias, char[] key) throws IOException {
        writeLock();
        try {
            provider.setSecretKey(alias, key);
        } finally {
            writeUnLock();
        }
    }

    public void setUserGroupKey(String serviceName, char[] password) throws IOException {
        writeLock();
        try {
            provider.setUserGroupKey(serviceName, password);
        } finally {
            writeUnLock();
        }
    }

    public void removeKey(String alias) throws IOException {
        writeLock();
        try {
            provider.removeKey(alias);
        } finally {
            writeUnLock();
        }
    }

    public void storeKeyStore() throws IOException {
        writeLock();
        try {
            provider.storeKeyStore();
        } finally {
            writeUnLock();
        }
    }

    public void prepareForMasterPasswordChange(char[] oldPassword, char[] newPassword)
            throws IOException {
        writeLock();
        try {
            provider.prepareForMasterPasswordChange(oldPassword, newPassword);
        } finally {
            writeUnLock();
        }
    }

    public void abortMasterPasswordChange() {
        writeLock();
        try {
            provider.abortMasterPasswordChange();
        } finally {
            writeUnLock();
        }
    }

    public void commitMasterPasswordChange() throws IOException {
        writeLock();
        try {
            provider.commitMasterPasswordChange();
        } finally {
            writeUnLock();
        }
    }
}
