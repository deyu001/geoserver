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

package org.geoserver.web.admin;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.web.GeoServerSecuredPage;

/** @author Arne Kepp, The Open Planning Project */
@SuppressWarnings("serial")
public abstract class ServerAdminPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = 4712657652337914993L;

    public IModel<GeoServer> getGeoServerModel() {
        return new LoadableDetachableModel<GeoServer>() {
            public GeoServer load() {
                return getGeoServerApplication().getGeoServer();
            }
        };
    }

    public IModel<GeoServerInfo> getGlobalInfoModel() {
        return new LoadableDetachableModel<GeoServerInfo>() {
            public GeoServerInfo load() {
                return getGeoServerApplication().getGeoServer().getGlobal();
            }
        };
    }

    public IModel<JAIInfo> getJAIModel() {
        // Notes setup on top of an explanation provided by Gabriel Roldan for
        // his patch which fixes the modificationProxy unable to detect changes
        // --------------------------------------------------------------------
        // with this change, we will edit a clone of the original JAIInfo.
        // By this way, the modification proxy will count it as a change.
        // The previous code wasn't working as expected.
        // the reason is that the model used to edit JAIInfo is a
        // LoadableDetachableModel, so when the edit page does gobal.setJAI, it
        // is actually setting the same object reference, and hence the
        // modificationproxy does not count it as a change.

        JAIInfo currJaiInfo = getGeoServerApplication().getGeoServer().getGlobal().getJAI().clone();
        return new Model<>(currJaiInfo);
    }

    public IModel<CoverageAccessInfo> getCoverageAccessModel() {
        // Notes setup on top of an explanation provided by Gabriel Roldan for
        // his patch which fixes the modificationProxy unable to detect changes
        // --------------------------------------------------------------------
        // with this change, we will edit a clone of the original Info.
        // By this way, the modification proxy will count it as a change.
        // The previous code wasn't working as expected.
        // the reason is that the model used to edit the page is a
        // LoadableDetachableModel, so when the edit page does gobal.setJAI, it
        // is actually setting the same object reference, and hence the
        // modificationProxy does not count it as a change.

        CoverageAccessInfo currCoverageAccessInfo =
                getGeoServerApplication().getGeoServer().getGlobal().getCoverageAccess().clone();
        return new Model<>(currCoverageAccessInfo);
    }

    public IModel<ContactInfo> getContactInfoModel() {
        return new LoadableDetachableModel<ContactInfo>() {
            public ContactInfo load() {
                return getGeoServerApplication()
                        .getGeoServer()
                        .getGlobal()
                        .getSettings()
                        .getContact();
            }
        };
    }

    public IModel<LoggingInfo> getLoggingInfoModel() {
        return new LoadableDetachableModel<LoggingInfo>() {
            @Override
            protected LoggingInfo load() {
                return getGeoServer().getLogging();
            }
        };
    }
}
