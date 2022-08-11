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

package org.geoserver.taskmanager.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.springframework.security.core.context.SecurityContextHolder;

public class BatchesModel extends GeoServerDataProvider<Batch> {

    private static final long serialVersionUID = -8246320435114536132L;

    public static class StringReversePropertyComparator<T> implements Comparator<T> {
        Property<T> property;

        public StringReversePropertyComparator(Property<T> property) {
            this.property = property;
        }

        public int compare(T o1, T o2) {
            Object p1 = property.getPropertyValue(o1);
            Object p2 = property.getPropertyValue(o2);

            // what if any property is null? We assume null < (not null)
            if (p1 == null) return p2 != null ? -1 : 0;
            else if (p2 == null) return 1;

            return new StringBuffer(p1.toString())
                    .reverse()
                    .toString()
                    .compareTo(new StringBuffer(p2.toString()).reverse().toString());
        }
    }

    public static final Property<Batch> WORKSPACE =
            new BeanProperty<Batch>("workspace", "workspace");
    public static final Property<Batch> DESCRIPTION =
            new BeanProperty<Batch>("description", "description");
    public static final Property<Batch> FREQUENCY =
            new BeanProperty<Batch>("frequency", "frequency") {
                private static final long serialVersionUID = -5039727601429342722L;

                @Override
                public Comparator<Batch> getComparator() {
                    return new StringReversePropertyComparator<Batch>(this);
                }
            };
    public static final Property<Batch> ENABLED = new BeanProperty<Batch>("enabled", "enabled");
    public static final Property<Batch> NAME = new BeanProperty<Batch>("name", "name");
    public static final Property<Batch> STARTED =
            new AbstractProperty<Batch>("started") {
                private static final long serialVersionUID = 6588177543318699677L;

                @Override
                public Object getPropertyValue(Batch batch) {
                    if (batch.getId() != null) {
                        if (batch.getLatestBatchRun() != null) {
                            return batch.getLatestBatchRun().getStart();
                        }
                    }
                    return null;
                }
            };

    public static final Property<Batch> STATUS =
            new AbstractProperty<Batch>("status") {

                private static final long serialVersionUID = 6588177543318699677L;

                @Override
                public Object getPropertyValue(Batch batch) {
                    if (batch.getId() != null) {
                        if (batch.getLatestBatchRun() != null) {
                            return batch.getLatestBatchRun().getStatus();
                        }
                    }
                    return null;
                }
            };

    public static final Property<Batch> RUN =
            new AbstractProperty<Batch>("run") {

                private static final long serialVersionUID = -978472501994535469L;

                @Override
                public Object getPropertyValue(Batch item) {
                    return null;
                }
            };

    public static final Property<Batch> FULL_NAME =
            new AbstractProperty<Batch>("name") {
                private static final long serialVersionUID = 6588177543318699677L;

                @Override
                public Object getPropertyValue(Batch item) {
                    return item.getFullName();
                }
            };

    private IModel<Configuration> configurationModel;

    private List<Batch> list;

    public BatchesModel() {}

    public BatchesModel(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }

    @Override
    protected List<Property<Batch>> getProperties() {
        return Arrays.asList(
                WORKSPACE,
                configurationModel == null ? FULL_NAME : NAME,
                DESCRIPTION,
                FREQUENCY,
                ENABLED,
                STARTED,
                RUN,
                STATUS);
    }

    public void reset() {
        list = null;
    }

    @Override
    protected List<Batch> getItems() {
        if (list == null) {
            if (configurationModel == null) {
                list = TaskManagerBeans.get().getDao().getViewableBatches();
            } else {
                if (configurationModel.getObject().getId() != null) {
                    TaskManagerBeans.get()
                            .getDao()
                            .loadLatestBatchRuns(configurationModel.getObject());
                }
                list = new ArrayList<>(configurationModel.getObject().getBatches().values());
            }

            list.removeIf(
                    b ->
                            !TaskManagerBeans.get()
                                    .getSecUtil()
                                    .isReadable(
                                            SecurityContextHolder.getContext().getAuthentication(),
                                            b));
        }

        return list;
    }
}
