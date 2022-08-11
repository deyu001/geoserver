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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

@Entity
@Table(
    uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "configuration", "removeStamp"})}
)
@FilterDef(name = "activeTaskElementFilter", defaultCondition = "removeStamp = 0")
// TODO: need alias support for filters, for now need to filter this out manually
// @FilterDef(name="activeTaskElementFilter", defaultCondition="removeStamp = 0 and
// batch.removeStamp = 0")
public class TaskImpl extends BaseImpl implements Task {

    private static final long serialVersionUID = -4050889394621568829L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    @XStreamOmitField
    private Long id;

    @Column private String name;

    @Column private String type;

    @ManyToOne
    @JoinColumn(name = "configuration")
    private ConfigurationImpl configuration;

    @OneToMany(
        fetch = FetchType.EAGER,
        targetEntity = ParameterImpl.class,
        mappedBy = "task",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @MapKey(name = "name")
    @OrderBy("id")
    private Map<String, Parameter> parameters = new LinkedHashMap<String, Parameter>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = BatchElementImpl.class, mappedBy = "task")
    @OrderBy("index")
    @Filter(name = "activeTaskElementFilter")
    private List<BatchElement> batchElements = new ArrayList<BatchElement>();

    @Column(nullable = false)
    @XStreamOmitField
    private Long removeStamp = 0L;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    @Override
    public List<BatchElement> getBatchElements() {
        return batchElements;
    }

    public void setBatchElements(List<BatchElement> batchElements) {
        this.batchElements = batchElements;
    }

    @Override
    public ConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = (ConfigurationImpl) configuration;
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
    public void setRemoveStamp(long removeStamp) {
        this.removeStamp = removeStamp;
    }

    @Override
    public long getRemoveStamp() {
        return removeStamp;
    }
}
