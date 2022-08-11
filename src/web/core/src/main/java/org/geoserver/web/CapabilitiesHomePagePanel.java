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

package org.geoserver.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geotools.util.Version;

/**
 * Default component for a {@link CapabilitiesHomePageLinkProvider} implementation to provide a list
 * of getcapabilities links discriminated by service name and version.
 *
 * @author Gabriel Roldan
 */
public class CapabilitiesHomePagePanel extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * A complete reference to a GetCapabilities or other service description document acting as the
     * model object to this panel's ListView.
     */
    public static class CapsInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        String service;

        Version version;

        String capsLink;

        public CapsInfo(String service, Version version, String capsLink) {
            this.service = service;
            this.version = version;
            this.capsLink = capsLink;
        }

        public String getService() {
            return service;
        }

        public Version getVersion() {
            return version;
        }

        public String getCapsLink() {
            return capsLink;
        }

        public boolean equals(Object o) {
            if (!(o instanceof CapsInfo)) {
                return false;
            }
            CapsInfo ci = (CapsInfo) o;
            return service.equals(ci.service)
                    && version.equals(ci.version)
                    && capsLink.equals(ci.capsLink);
        }

        @Override
        public int hashCode() {
            return Objects.hash(service, version, capsLink);
        }
    }

    /**
     * @param id this component's wicket id
     * @param capsLinks the list of getcapabilities link to create the component for
     */
    public CapabilitiesHomePagePanel(final String id, final List<CapsInfo> capsLinks) {

        super(id);

        final Map<String, List<CapsInfo>> byService = new HashMap<>();
        for (CapsInfo c : capsLinks) {
            final String key =
                    c.getService().toLowerCase(); // to avoid problems with uppercase definitions
            List<CapsInfo> serviceLinks = byService.get(key);
            if (serviceLinks == null) {
                serviceLinks = new ArrayList<>();
                byService.put(key, serviceLinks);
            }
            serviceLinks.add(c);
        }

        ArrayList<String> services = new ArrayList<>(byService.keySet());
        Collections.sort(services);

        ListView<String> view =
                new ListView<String>("services", services) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        final String serviceId = item.getModelObject();
                        item.add(new Label("service", serviceId.toUpperCase()));
                        item.add(
                                new ListView<CapsInfo>("versions", byService.get(serviceId)) {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void populateItem(ListItem<CapsInfo> item) {
                                        CapsInfo capsInfo = item.getModelObject();
                                        Version version = capsInfo.getVersion();
                                        String capsLink = capsInfo.getCapsLink();
                                        ExternalLink link = new ExternalLink("link", capsLink);
                                        item.add(link);

                                        link.add(new Label("version", version.toString()));
                                    }
                                });
                    }
                };

        add(view);
    }
}
