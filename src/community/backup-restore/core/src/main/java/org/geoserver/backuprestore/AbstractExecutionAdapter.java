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

package org.geoserver.backuprestore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.platform.resource.Resource;
import org.opengis.filter.Filter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

/**
 * Base Class for {@link JobExecution} wrappers. Those will be used to share objects, I/O parameters
 * and GeoServer B/R specific variables and the batch contexts.
 *
 * <p>{@link ConcurrentHashMap}s are populated from the {@link Backup} facade in order to allow
 * external classes to follow jobs executions and retrieve configuration, parameters and statuses.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class AbstractExecutionAdapter {

    private Integer totalNumberOfSteps;

    private JobExecution delegate;

    private List<String> options = Collections.synchronizedList(new ArrayList<String>());

    private List<Throwable> warningsList = Collections.synchronizedList(new ArrayList<Throwable>());

    private Resource archiveFile;

    private Filter wsFilter;

    private Filter siFilter;

    private Filter liFilter;

    /** Default Constructor */
    public AbstractExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        this.delegate = jobExecution;
        this.totalNumberOfSteps = totalNumberOfSteps;
    }

    /** @return the delegate */
    public JobExecution getDelegate() {
        return delegate;
    }

    /** @param delegate the delegate to set */
    public void setDelegate(JobExecution delegate) {
        this.delegate = delegate;
    }

    /** The Unique Job Execution ID */
    public Long getId() {
        if (delegate != null) {
            return delegate.getId();
        }
        return null;
    }

    /**
     * Convenience getter for for the id of the enclosing job. Useful for DAO implementations.
     *
     * @return the id of the enclosing job
     */
    public Long getJobId() {
        return delegate.getJobId();
    }

    /**
     * The Spring Batch {@link JobParameters}
     *
     * @return JobParameters of the enclosing job
     */
    public JobParameters getJobParameters() {
        return delegate.getJobParameters();
    }

    /** The Spring Batch Job TimeStamp */
    public Date getTime() {
        return new Date(delegate.getJobParameters().getLong(Backup.PARAM_TIME));
    }

    /**
     * The Spring Batch {@link BatchStatus}
     *
     * <p>ABANDONED COMPLETED FAILED STARTED STARTING STOPPED STOPPING UNKNOWN
     *
     * @return BatchStatus of the enclosing job
     */
    public BatchStatus getStatus() {
        return delegate.getStatus();
    }

    /**
     * The Spring Batch {@link ExitStatus}
     *
     * @return the exitCode of the enclosing job
     */
    public ExitStatus getExitStatus() {
        return delegate.getExitStatus();
    }

    /** Set {@link ExitStatus} of the current Spring Batch Execution */
    public void setExitStatus(ExitStatus exitStatus) {
        delegate.setExitStatus(exitStatus);
    }

    /** Returns all {@link StepExecution}s of the current Spring Batch Execution */
    public Collection<StepExecution> getStepExecutions() {
        return delegate.getStepExecutions();
    }

    /**
     * The Spring Batch {@link JobInstance}
     *
     * @return the Job that is executing.
     */
    public JobInstance getJobInstance() {
        return delegate.getJobInstance();
    }

    /**
     * Test if this {@link JobExecution} indicates that it is running. It should be noted that this
     * does not necessarily mean that it has been persisted as such yet.
     *
     * @return true if the end time is null
     */
    public boolean isRunning() {
        return delegate.isRunning();
    }

    /**
     * Test if this {@link JobExecution} indicates that it has been signalled to stop.
     *
     * @return true if the status is {@link BatchStatus#STOPPING}
     */
    public boolean isStopping() {
        return delegate.isStopping();
    }

    /**
     * Return all failure causing exceptions for this JobExecution, including step executions.
     *
     * @return List&lt;Throwable&gt; containing all exceptions causing failure for this
     *     JobExecution.
     */
    public List<Throwable> getAllFailureExceptions() {
        return delegate.getAllFailureExceptions();
    }

    /**
     * Return all failure marked as warnings by this JobExecution, including step executions.
     *
     * @return List&lt;Throwable&gt; containing all warning exceptions.
     */
    public List<Throwable> getAllWarningExceptions() {
        return warningsList;
    }

    /** Adds exceptions to the current executions marking it as FAILED. */
    public void addFailureExceptions(List<Throwable> exceptions) {
        for (Throwable t : exceptions) {
            this.delegate.addFailureException(t);
        }

        this.delegate.setExitStatus(ExitStatus.FAILED);
    }

    /** Adds exceptions to the current executions as Warnings. */
    public void addWarningExceptions(List<Throwable> exceptions) {
        for (Throwable t : exceptions) {
            this.warningsList.add(t);
        }
    }

    /**
     * Returns the total number of Job steps
     *
     * @return the totalNumberOfSteps
     */
    public Integer getTotalNumberOfSteps() {
        return totalNumberOfSteps;
    }

    /** Returns the current number of executed steps. */
    public Integer getExecutedSteps() {
        return delegate.getStepExecutions().size();
    }

    /** @return the options */
    public List<String> getOptions() {
        return options;
    }

    /** @return */
    public String getProgress() {
        final StringBuffer progress = new StringBuffer();
        progress.append(getExecutedSteps()).append("/").append(getTotalNumberOfSteps());
        return progress.toString();
    }

    /** @return the archiveFile */
    public Resource getArchiveFile() {
        return archiveFile;
    }

    /** @param archiveFile the archiveFile to set */
    public void setArchiveFile(Resource archiveFile) {
        this.archiveFile = archiveFile;
    }

    /** @return the wsFilter */
    public Filter getWsFilter() {
        return wsFilter;
    }

    /** @param wsFilter the wsFilter to set */
    public void setWsFilter(Filter wsFilter) {
        this.wsFilter = wsFilter;
    }

    /** @return the siFilter */
    public Filter getSiFilter() {
        return siFilter;
    }

    /** @param siFilter the siFilter to set */
    public void setSiFilter(Filter siFilter) {
        this.siFilter = siFilter;
    }

    /** @return the liFilter */
    public Filter getLiFilter() {
        return liFilter;
    }

    /** @param liFilter the liFilter to set */
    public void setLiFilter(Filter liFilter) {
        this.liFilter = liFilter;
    }
}
