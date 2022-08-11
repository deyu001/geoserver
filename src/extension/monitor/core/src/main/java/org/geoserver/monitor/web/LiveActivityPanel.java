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

package org.geoserver.monitor.web;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestData.Status;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

public class LiveActivityPanel extends Panel {

    private static final long serialVersionUID = -2807950039989311964L;

    public LiveActivityPanel(String id) {
        super(id);

        GeoServerTablePanel<RequestData> requests =
                new GeoServerTablePanel<RequestData>("requests", new LiveRequestDataProvider()) {
                    private static final long serialVersionUID = -431473636413825153L;

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<RequestData> itemModel,
                            Property<RequestData> property) {
                        Object prop = property.getPropertyValue(itemModel.getObject());

                        String value = prop != null ? prop.toString() : "";
                        return new Label(id, value);
                    }
                };
        add(requests);
    }

    static class LiveRequestDataProvider extends GeoServerDataProvider<RequestData> {

        private static final long serialVersionUID = -5576324995486786071L;

        static final Property<RequestData> ID = new BeanProperty<>("id", "id");
        static final Property<RequestData> PATH = new BeanProperty<>("path", "path");
        static final Property<RequestData> STATUS = new BeanProperty<>("status", "status");

        @Override
        protected List<RequestData> getItems() {
            MonitorDAO dao = getApplication().getBeanOfType(Monitor.class).getDAO();
            Query q =
                    new Query()
                            .filter(
                                    "status",
                                    Arrays.asList(
                                            Status.RUNNING, Status.WAITING, Status.CANCELLING),
                                    Comparison.IN);

            return dao.getRequests(q);
        }

        @Override
        protected List<Property<RequestData>> getProperties() {
            return Arrays.asList(ID, PATH, STATUS);
        }
    }
}
