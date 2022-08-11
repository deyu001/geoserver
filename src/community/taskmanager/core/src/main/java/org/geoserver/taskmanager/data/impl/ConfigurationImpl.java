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

package org.geoserver.taskmanager.data.impl;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "removeStamp"})})
@FilterDefs({
    @FilterDef(name = "activeTaskFilter", defaultCondition = "removeStamp = 0"),
    @FilterDef(name = "activeBatchFilter", defaultCondition = "removeStamp = 0")
})
public class ConfigurationImpl extends BaseImpl implements Configuration {

    private static final long serialVersionUID = 7562166441281067057L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    @XStreamOmitField
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @XStreamOmitField
    private Boolean template = false;

    @Column(nullable = false)
    private Boolean validated = false;

    @Column private String workspace;

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = AttributeImpl.class,
        mappedBy = "configuration",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("id")
    @MapKey(name = "name")
    private Map<String, Attribute> attributes = new LinkedHashMap<String, Attribute>();

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = TaskImpl.class,
        mappedBy = "configuration",
        cascade = CascadeType.ALL
    )
    @OrderBy("id")
    @MapKey(name = "name")
    @Filter(name = "activeTaskFilter")
    private Map<String, Task> tasks = new LinkedHashMap<String, Task>();

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = BatchImpl.class,
        mappedBy = "configuration",
        cascade = CascadeType.ALL
    )
    @OrderBy("id")
    @MapKey(name = "name")
    @Filter(name = "activeBatchFilter")
    private Map<String, Batch> batches = new LinkedHashMap<String, Batch>();

    @Column(nullable = false)
    @XStreamOmitField
    private Long removeStamp = 0L;

    @Column private String description;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean isTemplate() {
        return template;
    }

    @Override
    public void setTemplate(boolean template) {
        this.template = template;
    }

    @Override
    public String getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public Map<String, Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public Map<String, Task> getTasks() {
        return tasks;
    }

    @Override
    public Map<String, Batch> getBatches() {
        return batches;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setRemoveStamp(long removeStamp) {
        this.removeStamp = removeStamp;
    }

    @Override
    public long getRemoveStamp() {
        return removeStamp;
    }

    @Override
    public boolean isValidated() {
        return validated;
    }

    @Override
    public void setValidated(boolean initMode) {
        this.validated = initMode;
    }
}
