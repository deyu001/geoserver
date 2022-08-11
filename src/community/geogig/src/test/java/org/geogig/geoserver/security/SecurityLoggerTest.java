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

package org.geogig.geoserver.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.geogig.geoserver.config.LogStore;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.porcelain.BlameOp;
import org.locationtech.geogig.porcelain.CleanOp;
import org.locationtech.geogig.porcelain.DiffOp;
import org.locationtech.geogig.remotes.CloneOp;
import org.locationtech.geogig.remotes.FetchOp;
import org.locationtech.geogig.remotes.PullOp;
import org.locationtech.geogig.remotes.PushOp;
import org.locationtech.geogig.remotes.RemoteAddOp;
import org.locationtech.geogig.remotes.RemoteRemoveOp;
import org.locationtech.geogig.repository.Remote;
import org.mockito.ArgumentCaptor;

public class SecurityLoggerTest {

    @SuppressWarnings("unused")
    private SecurityLogger logger;

    private LogStore mockStore;

    @Before
    public void before() throws Exception {
        mockStore = mock(LogStore.class);
        logger = new SecurityLogger(mockStore);
        logger.afterPropertiesSet();
    }

    @Test
    public void testCommandsOfInterest() {
        assertTrue(SecurityLogger.interestedIn(RemoteAddOp.class));
        assertTrue(SecurityLogger.interestedIn(RemoteRemoveOp.class));
        assertTrue(SecurityLogger.interestedIn(PushOp.class));
        assertTrue(SecurityLogger.interestedIn(PullOp.class));
        assertTrue(SecurityLogger.interestedIn(FetchOp.class));
        assertTrue(SecurityLogger.interestedIn(CloneOp.class));

        assertFalse(SecurityLogger.interestedIn(BlameOp.class));
        assertFalse(SecurityLogger.interestedIn(CleanOp.class));
        assertFalse(SecurityLogger.interestedIn(DiffOp.class));
        assertFalse(SecurityLogger.interestedIn(RefParse.class));
    }

    @Test
    public void testRemoteAdd() {
        RemoteAddOp command = new RemoteAddOp();
        String remoteName = "upstream";
        String mappedBranch = "master";
        String username = "gabriel";
        String password = "passw0rd";

        String fetchurl = "http://demo.example.com/testrepo";
        String pushurl = fetchurl;
        String fetch =
                "+"
                        + Ref.append(Ref.HEADS_PREFIX, mappedBranch)
                        + ":"
                        + Ref.append(Ref.append(Ref.REMOTES_PREFIX, remoteName), mappedBranch);
        boolean mapped = true;

        command.setName(remoteName)
                .setBranch(mappedBranch)
                .setMapped(mapped)
                .setPassword(password)
                .setURL(username)
                .setURL(fetchurl);

        ArgumentCaptor<CharSequence> arg = ArgumentCaptor.forClass(CharSequence.class);
        SecurityLogger.logPre(command);
        verify(mockStore).debug(anyString(), arg.capture());

        // Remote add: Parameters: name='upstream', url='http://demo.example.com/testrepo'
        String msg = String.valueOf(arg.getValue());
        assertTrue(msg.startsWith("Remote add"));
        assertTrue(msg.contains(remoteName));
        assertTrue(msg.contains(fetchurl));

        Remote retVal =
                new Remote(
                        remoteName,
                        fetchurl,
                        pushurl,
                        fetch,
                        mapped,
                        mappedBranch,
                        username,
                        password);
        SecurityLogger.logPost(command, retVal, null);
        verify(mockStore).info(anyString(), arg.capture());

        msg = String.valueOf(arg.getValue());
        assertTrue(msg.startsWith("Remote add success"));
        assertTrue(msg.contains(remoteName));
        assertTrue(msg.contains(fetchurl));

        ArgumentCaptor<Throwable> exception = ArgumentCaptor.forClass(Throwable.class);
        SecurityLogger.logPost(command, null, new RuntimeException("test exception"));
        verify(mockStore).error(anyString(), arg.capture(), exception.capture());

        msg = String.valueOf(arg.getValue());
        assertTrue(msg.startsWith("Remote add failed"));
        assertTrue(msg.contains(remoteName));
        assertTrue(msg.contains(fetchurl));
    }
}
