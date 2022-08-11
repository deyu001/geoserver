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

package org.geoserver.security.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.impl.Util;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A simple class to support file based stores. Simulates a write lock by creating/removing a
 * physical file on the file system
 *
 * @author Christian
 */
public class LockFile {

    protected long lockFileLastModified;
    protected Resource lockFileTarget, lockFile;

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");

    public LockFile(Resource file) throws IOException {
        lockFileTarget = file;
        if (!Resources.exists(file)) {
            throw new IOException("Cannot lock a not existing file: " + file.path());
        }
        lockFile = file.parent().get(lockFileTarget.name() + ".lock");
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                new Runnable() { // remove on shutdown

                                    @Override
                                    public void run() {
                                        lockFile.delete();
                                    }
                                }));
    }

    /** return true if a write lock is hold by this file watcher */
    public boolean hasWriteLock() throws IOException {
        return Resources.exists(lockFile) && lockFile.lastmodified() == lockFileLastModified;
    }

    /** return true if a write lock is hold by another file watcher */
    public boolean hasForeignWriteLock() throws IOException {
        return Resources.exists(lockFile) && lockFile.lastmodified() != lockFileLastModified;
    }

    /** remove the lockfile */
    public void writeUnLock() {
        if (Resources.exists(lockFile)) {
            if (lockFile.lastmodified() == lockFileLastModified) {
                lockFileLastModified = 0;
                lockFile.delete();
            } else {
                LOGGER.warning("Tried to unlock foreign lock: " + lockFile.path());
            }
        } else {
            LOGGER.warning("Tried to unlock not exisiting lock: " + lockFile.path());
        }
    }

    /** Try to get a lock */
    public void writeLock() throws IOException {

        if (hasWriteLock()) return; // already locked

        if (Resources.exists(lockFile)) {
            LOGGER.warning("Cannot obtain  lock: " + lockFile.path());
            Properties props = new Properties();

            try (InputStream in = lockFile.in()) {
                props.load(in);
            }

            throw new IOException(Util.convertPropsToString(props, "Already locked"));
        } else { // success
            writeLockFileContent(lockFile);
            lockFileLastModified = lockFile.lastmodified();
            LOGGER.info("Successful lock: " + lockFile.path());
        }
    }

    /** Write some info into the lock file hostname, ip, user and lock file path */
    protected void writeLockFileContent(Resource lockFile) throws IOException {

        Properties props = new Properties();
        try (OutputStream out = lockFile.out()) {
            props.store(out, "Locking info");

            String hostname = "UNKNOWN";
            String ip = "UNKNOWN";

            // find some network info
            try {
                hostname = InetAddress.getLocalHost().getHostName();
                InetAddress addrs[] = InetAddress.getAllByName(hostname);
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress())
                        ip = addr.getHostAddress();
                }
            } catch (UnknownHostException ex) {
            }

            props.put("hostname", hostname);
            props.put("ip", ip);
            props.put("location", lockFile.path());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            props.put("principal", auth == null ? "UNKNOWN" : auth.getName());

            props.store(out, "Locking info");
        }
    }
}
