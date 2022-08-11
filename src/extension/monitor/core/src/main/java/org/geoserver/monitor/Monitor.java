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

package org.geoserver.monitor;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.monitor.MonitorConfig.Mode;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * The GeoServer request monitor and primary entry point into the monitor api.
 *
 * <p>For each request submitted to a GeoServer instance the monitor maintains state about the
 * request and makes operations available that control the life cycle of the request. The life cycle
 * of a monitored request advances through the following states:
 *
 * <ul>
 *   <li>the request is STARTED
 *   <li>the request is UPDATED any number of times.
 * </ul>
 *
 * @author Andrea Aime, OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
public class Monitor implements ApplicationListener<ApplicationEvent> {

    /** thread local request object. */
    static ThreadLocal<RequestData> REQUEST = new ThreadLocal<>();

    /** default page size when executing queries */
    static long PAGE_SIZE = 1000;

    MonitorConfig config;
    MonitorDAO dao;

    // info about monitored server
    // JD: look up lazily, using constructor injection causes failure to load main Geoserver
    // configuration, TODO: take another look at the initialization
    GeoServer server;

    /** The set of listeners for the monitor */
    List<RequestDataListener> listeners = new ArrayList<>();

    public Monitor(MonitorConfig config) {
        this.config = config;
        this.dao = config.createDAO();
    }

    public Monitor(MonitorDAO dao) {
        this.config = new MonitorConfig();
        this.dao = dao;
    }

    public MonitorConfig getConfig() {
        return config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public RequestData start() {
        RequestData req = new RequestData();
        req = dao.init(req);
        REQUEST.set(req);

        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestUpdated(req);
        }
        // have the DAO persist/propagate the change
        if (config.getMode() != Mode.HISTORY) {
            dao.add(req);
        }

        return req;
    }

    public RequestData current() {
        return REQUEST.get();
    }

    public void update() {
        RequestData data = REQUEST.get();
        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestUpdated(data);
        }
        // have the DAO persist/propagate the change
        if (config.getMode() != Mode.HISTORY) {
            dao.update(data);
        }
    }

    public void complete() {
        RequestData data = REQUEST.get();
        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestCompleted(data);
        }
        // have the DAO persist/propagate the change
        dao.save(data);
        REQUEST.remove();
    }

    public void postProcessed(RequestData rd) {
        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestPostProcessed(rd);
        }
        // have the DAO persist/propagate the change
        dao.update(rd);
    }

    public void dispose() {
        dao.dispose();
        dao = null;
    }

    public MonitorDAO getDAO() {
        return dao;
    }

    public GeoServer getServer() {
        return server;
    }

    public void setServer(GeoServer server) {
        this.server = server;
    }

    public void query(Query q, RequestDataVisitor visitor) {
        dao.getRequests(q, visitor);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            listeners = GeoServerExtensions.extensions(RequestDataListener.class);
        }
    }
}
