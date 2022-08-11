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
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

/** @author Niels Charlier */
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "configuration", "removeStamp"}),
        @UniqueConstraint(columnNames = {"nameNoConfig", "removeStamp"})
    }
)
@FilterDef(name = "activeElementFilter", defaultCondition = "removeStamp = 0")
public class BatchImpl extends BaseImpl implements Batch {

    private static final long serialVersionUID = 3321130631692899821L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    @XStreamOmitField
    private Long id;

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = BatchElementImpl.class,
        mappedBy = "batch",
        cascade = CascadeType.ALL
    )
    @OrderBy("index, id")
    @Filter(name = "activeElementFilter")
    private List<BatchElement> elements = new ArrayList<BatchElement>();

    @Column private String workspace;

    @Column(nullable = false)
    private String name;

    // stupid work-around
    // duplicate of name only set if configuration == null, just for unique constraint
    @Column @XStreamOmitField private String nameNoConfig;

    @ManyToOne
    @JoinColumn(name = "configuration", nullable = true)
    private ConfigurationImpl configuration;

    @Column private String description;

    @Column(nullable = true)
    private String frequency;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    @XStreamOmitField
    private Long removeStamp = 0L;

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = BatchRunImpl.class,
        mappedBy = "batch",
        cascade = CascadeType.ALL
    )
    @OrderBy("id")
    @XStreamOmitField
    private List<BatchRun> batchRuns = new ArrayList<BatchRun>();

    @Transient @XStreamOmitField private BatchRun latestBatchRun;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public List<BatchElement> getElements() {
        return elements;
    }

    @Override
    public String getFrequency() {
        return frequency;
    }

    @Override
    public void setFrequency(String frequency) {
        this.frequency = frequency;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        if (configuration == null) {
            this.nameNoConfig = name;
        }
    }

    @Override
    public ConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = (ConfigurationImpl) configuration;
        if (configuration == null) {
            nameNoConfig = name;
        } else {
            nameNoConfig = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
    public List<BatchRun> getBatchRuns() {
        return batchRuns;
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
    public BatchRun getLatestBatchRun() {
        return latestBatchRun;
    }

    public void setLatestBatchRun(BatchRun latestBatchRun) {
        this.latestBatchRun = latestBatchRun;
    }
}
