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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(indexes = {@Index(name = "schedulerReferenceIndex", columnList = "schedulerReference")})
public class BatchRunImpl extends BaseImpl implements BatchRun {

    private static final long serialVersionUID = 2468505054020768482L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch")
    private BatchImpl batch;

    @OneToMany(
        fetch = FetchType.EAGER,
        targetEntity = RunImpl.class,
        mappedBy = "batchRun",
        cascade = CascadeType.ALL
    )
    @OrderBy("start")
    @Fetch(FetchMode.SUBSELECT)
    private List<Run> runs = new ArrayList<Run>();

    @Column(nullable = false)
    private Boolean interruptMe = false;

    @Column private String schedulerReference;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public BatchImpl getBatch() {
        return batch;
    }

    @Override
    public void setBatch(Batch batch) {
        this.batch = (BatchImpl) batch;
    }

    @Override
    public List<Run> getRuns() {
        return runs;
    }

    @Override
    public Date getStart() {
        return BatchRun.super.getStart();
    }

    @Override
    public Date getEnd() {
        return BatchRun.super.getEnd();
    }

    @Override
    public Run.Status getStatus() {
        return BatchRun.super.getStatus();
    }

    @Override
    public String getMessage() {
        return BatchRun.super.getMessage();
    }

    @Override
    public boolean isInterruptMe() {
        return interruptMe;
    }

    @Override
    public void setInterruptMe(boolean interruptMe) {
        this.interruptMe = interruptMe;
    }

    @Override
    public String getSchedulerReference() {
        return schedulerReference;
    }

    @Override
    public void setSchedulerReference(String qReference) {
        this.schedulerReference = qReference;
    }
}
