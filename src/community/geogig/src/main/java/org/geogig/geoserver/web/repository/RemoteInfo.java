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

package org.geogig.geoserver.web.repository;

import static com.google.common.base.Objects.equal;

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.repository.Remote;

/** A {@link Remote} representation for the presentation layer */
public class RemoteInfo implements Serializable {

    private static final long serialVersionUID = 242699247252608741L;

    private Integer id;

    private String name, URL, userName, password;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RemoteInfo)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        RemoteInfo r = (RemoteInfo) o;
        return equal(name, r.name)
                && equal(URL, r.URL)
                && equal(userName, r.userName)
                && equal(password, r.password);
    }

    @Nullable
    Integer getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(RemoteInfo.class, name, URL, userName, password);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String uRL) {
        URL = uRL;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Remote toRemote() {
        String fetchurl = this.URL;
        String pushurl = this.URL;
        String fetch = "+" + Ref.HEADS_PREFIX + "*:" + Ref.REMOTES_PREFIX + name + "/*";
        boolean mapped = false;
        String mappedBranch = null;
        Remote r =
                new Remote(
                        name, fetchurl, pushurl, fetch, mapped, mappedBranch, userName, password);
        return r;
    }

    public static RemoteInfo create(Remote remote) {
        RemoteInfo ri = new RemoteInfo();

        String name = remote.getName();
        ri.setName(name);
        String url = remote.getFetchURL();
        ri.setURL(url);
        String userName = remote.getUserName();
        ri.setUserName(userName);
        String password = remote.getPassword();
        if (password != null) {
            password = Remote.decryptPassword(password);
        }
        ri.setPassword(password);
        ri.id = ri.hashCode();
        return ri;
    }

    public static ArrayList<RemoteInfo> fromList(List<Remote> remotes) {
        ArrayList<RemoteInfo> ris = new ArrayList<>();
        for (Remote remote : remotes) {
            ris.add(create(remote));
        }
        return ris;
    }
}
