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

package org.geoserver.security.ldap;

import java.io.File;
import java.util.UUID;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

/**
 * Helper class for embedded Apache Directory Server.
 *
 * <p>copied and modified from org.springframework.ldap.test.EmbeddedLdapServer to allow anonymous
 * access (there was no alternative way)
 *
 * @author Mattias Hellborg Arthursson
 * @author Niels Charlier
 */
public class EmbeddedLdapServer {

    private final DirectoryService directoryService;
    private final LdapServer ldapServer;
    private static File workingDirectory;

    private EmbeddedLdapServer(DirectoryService directoryService, LdapServer ldapServer) {
        this.directoryService = directoryService;
        this.ldapServer = ldapServer;
    }

    public static EmbeddedLdapServer newEmbeddedServer(
            String defaultPartitionName,
            String defaultPartitionSuffix,
            int port,
            boolean allowAnonymousAccess)
            throws Exception {
        DirectoryServiceFactory directoryServiceFactory = new DefaultDirectoryServiceFactory();
        directoryServiceFactory.init("geoserver-ldap" + UUID.randomUUID().toString());
        DirectoryService directoryService = directoryServiceFactory.getDirectoryService();
        workingDirectory =
                new File(
                        System.getProperty("java.io.tmpdir")
                                + "/apacheds-test"
                                + UUID.randomUUID().toString());
        directoryService.setShutdownHookEnabled(true);
        directoryService.setAllowAnonymousAccess(allowAnonymousAccess);
        directoryService.getChangeLog().setEnabled(false);

        JdbmPartition partition =
                new JdbmPartition(
                        directoryService.getSchemaManager(), directoryService.getDnFactory());
        partition.setId(defaultPartitionName);
        partition.setSuffixDn(new Dn(defaultPartitionSuffix));
        partition.setPartitionPath(workingDirectory.toURI());
        directoryService.addPartition(partition);

        directoryService.startup();

        // Inject the apache root entry if it does not already exist
        if (!directoryService.getAdminSession().exists(partition.getSuffixDn())) {
            Entry entry = directoryService.newEntry(new Dn(defaultPartitionSuffix));
            entry.add("objectClass", "top", "domain", "extensibleObject");
            entry.add("dc", defaultPartitionName);
            directoryService.getAdminSession().add(entry);
        }

        LdapServer ldapServer = new LdapServer();
        ldapServer.setDirectoryService(directoryService);

        TcpTransport ldapTransport = new TcpTransport(port);
        ldapServer.setTransports(ldapTransport);
        ldapServer.start();

        return new EmbeddedLdapServer(directoryService, ldapServer);
    }

    public void setAllowAnonymousAccess(boolean allowAnonymousAccess) {
        directoryService.setAllowAnonymousAccess(allowAnonymousAccess);
    }

    public void shutdown() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
    }
}
