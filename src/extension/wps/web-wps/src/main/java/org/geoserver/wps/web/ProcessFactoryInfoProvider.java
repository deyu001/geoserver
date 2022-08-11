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

package org.geoserver.wps.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

/** Provides entries for the process filtering table in the {@link WPSAccessRulePage} */
@SuppressWarnings("serial")
public class ProcessFactoryInfoProvider extends GeoServerDataProvider<ProcessGroupInfo> {

    private List<ProcessGroupInfo> processFactories;
    private Locale locale;

    public ProcessFactoryInfoProvider(List<ProcessGroupInfo> processFactories, Locale locale) {
        this.processFactories = processFactories;
        this.locale = locale;
    }

    @Override
    protected List<Property<ProcessGroupInfo>> getProperties() {
        List<Property<ProcessGroupInfo>> props = new ArrayList<>();
        props.add(new BeanProperty<>("enabled", "enabled"));
        props.add(
                new AbstractProperty<ProcessGroupInfo>("prefix") {

                    @Override
                    public Object getPropertyValue(ProcessGroupInfo item) {
                        Class factoryClass = item.getFactoryClass();
                        Set<String> prefixes = new HashSet<>();
                        ProcessFactory pf =
                                GeoServerProcessors.getProcessFactory(factoryClass, false);
                        if (pf != null) {
                            Set<Name> names = pf.getNames();
                            for (Name name : names) {
                                prefixes.add(name.getNamespaceURI());
                            }
                        }

                        // if we cannot find a title use the class name
                        if (prefixes.isEmpty()) {
                            return "";
                        } else {
                            // build a comma separated list with the prefixes
                            List<String> pl = new ArrayList<>(prefixes);
                            Collections.sort(pl);
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < pl.size(); i++) {
                                sb.append(pl.get(i));
                                if (i < pl.size() - 1) {
                                    sb.append(", ");
                                }
                            }

                            return sb.toString();
                        }
                    }
                });
        props.add(
                new AbstractProperty<ProcessGroupInfo>("title") {

                    @Override
                    public Object getPropertyValue(ProcessGroupInfo item) {
                        Class factoryClass = item.getFactoryClass();
                        String title = null;
                        ProcessFactory pf =
                                GeoServerProcessors.getProcessFactory(factoryClass, false);
                        if (pf != null) {
                            title = pf.getTitle().toString(locale);
                        }

                        // if we cannot find a title use the class name
                        if (title == null) {
                            title = factoryClass.getName();
                        }

                        return title;
                    }
                });
        props.add(
                new AbstractProperty<ProcessGroupInfo>("summary") {

                    @Override
                    public Object getPropertyValue(final ProcessGroupInfo item) {
                        return new LoadableDetachableModel<String>() {

                            @Override
                            protected String load() {
                                if (item.getFilteredProcesses().isEmpty()) {
                                    // all processes are enabled
                                    return new ParamResourceModel("WPSAdminPage.filter.all", null)
                                            .getString();
                                }

                                Class factoryClass = item.getFactoryClass();
                                ProcessFactory pf =
                                        GeoServerProcessors.getProcessFactory(factoryClass, false);
                                if (pf != null) {
                                    Set<Name> names = new HashSet<>(pf.getNames());
                                    int total = names.size();
                                    for (ProcessInfo toRemove : item.getFilteredProcesses()) {
                                        if (!toRemove.isEnabled()) {
                                            names.remove(toRemove.getName());
                                        }
                                    }
                                    int active = names.size();
                                    if (active != total) {
                                        return new ParamResourceModel(
                                                        "WPSAdminPage.filter.active",
                                                        null,
                                                        active,
                                                        total)
                                                .getString();
                                    } else {
                                        return new ParamResourceModel(
                                                        "WPSAdminPage.filter.all", null)
                                                .getString();
                                    }
                                }

                                return "?";
                            }
                        };
                    }
                });
        props.add(
                new AbstractProperty<ProcessGroupInfo>("roles") {
                    @Override
                    public Object getPropertyValue(ProcessGroupInfo item) {
                        return item.getRoles();
                    }

                    @Override
                    public IModel getModel(IModel itemModel) {
                        return new PropertyModel(itemModel, "roles");
                    }
                });
        props.add(new PropertyPlaceholder<>("edit"));

        return props;
    }

    @Override
    protected List<ProcessGroupInfo> getItems() {
        return processFactories;
    }
}
