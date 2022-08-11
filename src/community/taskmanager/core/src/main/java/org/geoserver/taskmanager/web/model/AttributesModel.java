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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class AttributesModel extends GeoServerDataProvider<Attribute> {

    private static final long serialVersionUID = -8846370782957169591L;

    public static final Property<Attribute> NAME = new BeanProperty<Attribute>("name", "name");

    public static final Property<Attribute> VALUE = new BeanProperty<Attribute>("value", "value");

    public static final Property<Attribute> ACTIONS =
            new AbstractProperty<Attribute>("actions") {

                private static final long serialVersionUID = -978472501994535469L;

                @Override
                public Object getPropertyValue(Attribute item) {
                    return null;
                }
            };

    private IModel<Configuration> configurationModel;

    private Map<String, Attribute> attributes = new HashMap<String, Attribute>();

    public AttributesModel(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }

    @Override
    protected List<Property<Attribute>> getProperties() {
        return Arrays.asList(NAME, VALUE, ACTIONS);
    }

    @Override
    public List<Attribute> getItems() {
        attributes.putAll(configurationModel.getObject().getAttributes());

        Set<String> taskAttNames = new LinkedHashSet<String>();
        for (Task task : configurationModel.getObject().getTasks().values()) {
            for (Parameter pam : task.getParameters().values()) {
                String attName =
                        TaskManagerBeans.get().getDataUtil().getAssociatedAttributeName(pam);
                if (attName != null) {
                    taskAttNames.add(attName);
                }
            }
        }

        Set<String> configAttNames = new LinkedHashSet<String>(attributes.keySet());

        List<Attribute> attList = new ArrayList<Attribute>();
        for (String attName : taskAttNames) {
            Attribute att = attributes.get(attName);
            if (att == null) {
                att = TaskManagerBeans.get().getFac().createAttribute();
                att.setConfiguration(InitConfigUtil.unwrap(configurationModel.getObject()));
                att.setName(attName);
                attributes.put(attName, att);
            }
            attList.add(att);
            configAttNames.remove(attName);
        }
        for (String attName : configAttNames) {
            Attribute att = attributes.get(attName);
            if (att.getValue() != null && !"".equals(att.getValue())) {
                attList.add(att);
            }
        }
        return attList;
    }

    public void refresh() {
        attributes.clear();
    }

    public void save(boolean removeEmpty) {
        getItems();
        for (Attribute att : attributes.values()) {
            if (!removeEmpty || att.getValue() != null && !"".equals(att.getValue())) {
                configurationModel.getObject().getAttributes().put(att.getName(), att);
            } else {
                configurationModel.getObject().getAttributes().remove(att.getName());
            }
        }
    }
}
