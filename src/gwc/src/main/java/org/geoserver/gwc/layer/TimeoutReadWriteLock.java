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

package org.geoserver.gwc.layer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.geoserver.platform.ServiceException;

/**
 * A wrapper around a ReadWriteLock that will perform all locking operations under a timeout to
 * prevent deadlocks.
 *
 * @author Andrea Aime - GeoSolutions
 */
class TimeoutReadWriteLock {

    ReadWriteLock lock = new ReentrantReadWriteLock();

    int timeoutMs;

    String name;

    /** Builds the {@link ReadWriteLock} wrapper with a given timeout, in milliseconds */
    public TimeoutReadWriteLock(int timeoutMs, String name) {
        this.timeoutMs = timeoutMs;
        this.name = name;
    }

    /**
     * Acquires a read lock with the configured timeout, will throw a {@link ServiceException} if
     * the lock is not acquired
     */
    public void acquireReadLock() {
        boolean acquired = false;
        try {
            acquired = lock.readLock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new ServiceException(
                    "Failed to acquire read lock on '" + name + "' due to interruption", e);
        }
        if (!acquired) {
            throw new ServiceException(
                    "Failed to acquire read lock on '"
                            + name
                            + "' in less than "
                            + timeoutMs
                            + " ms");
        }
    }

    /** Releases a previously acquired read lock */
    public void releaseReadLock() {
        lock.readLock().unlock();
    }

    /**
     * Acquires a write lock with the configured timeout, will throw a {@link ServiceException} if
     * the lock is not acquired
     */
    public void acquireWriteLock() {
        boolean acquired = false;
        try {
            acquired = lock.writeLock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new ServiceException(
                    "Failed to acquire write lock on '" + name + "' due to interruption", e);
        }
        if (!acquired) {
            throw new ServiceException(
                    "Failed to acquire write lock on '"
                            + name
                            + "' in less than "
                            + timeoutMs
                            + " ms");
        }
    }

    /** Releases a previously acquired write lock */
    public void releaseWriteLock() {
        lock.writeLock().unlock();
    }

    /**
     * Downgrades a write lock to a read lock. The write lock gets released, the caller must still
     * release the read lock after this is called
     */
    public void downgradeToReadLock() {
        // Downgrade by acquiring read lock before releasing write lock
        lock.readLock().lock();
        // Unlock write, still hold read
        lock.writeLock().unlock();
    }
}
