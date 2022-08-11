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

package org.geoserver.taskmanager.data;

import java.util.List;

/**
 * The task manager DAO
 *
 * @author Niels Charlier
 */
public interface TaskManagerDao {

    /**
     * Save a run.
     *
     * @param run the run
     * @return the saved run
     */
    Run save(Run run);

    /**
     * Save a batch run.
     *
     * @param run the batch run
     * @return the saved batch run
     */
    BatchRun save(BatchRun br);

    /**
     * List all active configurations.
     *
     * @param templates include (true) or exclude (false) templates
     * @return list of active configurations.
     */
    List<Configuration> getConfigurations(Boolean templates);

    /**
     * Get configuration by name.
     *
     * @param name name of the configuration.
     * @return the configuration.
     */
    Configuration getConfiguration(String name);

    /**
     * Get configuration by id.
     *
     * @param id the id of the configuration.
     * @return the configuration.
     */
    Configuration getConfiguration(long id);

    /**
     * Save a configuration.
     *
     * @param config the configuration.
     * @return the saved configuration.
     */
    Configuration save(Configuration config);

    /**
     * List all active non-template batches.
     *
     * @return the list of active batches.
     */
    List<Batch> getAllBatches();

    /**
     * Get a batch by its full name.
     *
     * @param name the name of the batch.
     * @return the batch.
     */
    Batch getBatch(String fullName);

    /**
     * Get a batch by id.
     *
     * @param id the id of the batch.
     * @return the batch.
     */
    Batch getBatch(long id);

    /**
     * Save a batch.
     *
     * @param batch the batch.
     * @return the saved batch.
     */
    Batch save(Batch batch);

    /**
     * Get a batch element that combines a certain batch or task, active or non-active.
     *
     * @param batch the batch.
     * @param task the task.
     * @return the batch element.
     */
    BatchElement getBatchElement(Batch batch, Task task);

    /**
     * Remove a soft removable object.
     *
     * @param item the object to be removed.
     * @return the soft removed object.
     */
    <T extends SoftRemove> T remove(T item);

    /**
     * If a task is currently being run, return the current run. This method must be protected
     * against concurrency.
     *
     * @param task the task.
     * @return the current run, or null if it is not being run.
     */
    Run getCurrentRun(Task task);

    /**
     * If a task is currently being committed, return the current run.
     *
     * @param task the task
     * @return the current run, or null if it is not being committed.
     */
    Run getCommittingRun(final Task task);

    /**
     * Permanently delete a batch. All historical information (runs) associated with this batch will
     * be removed.
     *
     * @param batch the batch
     */
    void delete(Batch batch);

    /**
     * Permanently delete a configuration. All historical information (tasks + runs + batches)
     * associated with this configuration will be removed. Note that this will fail if some of the
     * tasks are still used in existing independent batches.
     *
     * @param batch the batch
     */
    void delete(Configuration config);

    /**
     * Permanently delete a batch element. All historical information (runs) associated with this
     * batch element will be removed.
     *
     * @param batchElement the batch element
     */
    void delete(BatchElement batchElement);

    /**
     * Permanently delete a task. All historical information associated with this task will be
     * removed (runs). Note that this will fail if the task is still used in existing batches.
     *
     * @param task the task
     */
    void delete(Task task);

    /**
     * Get the latest run of a certain batch element.
     *
     * @param batchElement the batch element.
     * @return the latest run.
     */
    Run getLatestRun(BatchElement batchElement);

    /**
     * Copy configuration
     *
     * @param configName the name of the configuration you wish to copy
     * @return copied configuration
     */
    Configuration copyConfiguration(String configName);

    /**
     * List all tasks available for a batch. That means - no tasks already in the batch - only
     * active tasks - if the batch is part of a configuration or template, only tasks of that
     * configuration or template - if the batch is not part of a configuration or template, only
     * tasks in a real configuration (not template).
     *
     * @return the list of available tasks.
     */
    List<Task> getTasksAvailableForBatch(Batch batch);

    /**
     * Get current batch runs for batch
     *
     * @param batch the batch
     * @return the list of currently running batch runs for batch
     */
    List<BatchRun> getCurrentBatchRuns(Batch batch);

    /**
     * Reloads object.
     *
     * @param object the object to be reloaded
     * @return the reloaded object
     */
    <T extends Identifiable> T reload(T object);

    /**
     * Reloads object with lock in transaction.
     *
     * @param object the object to be reloaded
     * @return the reloaded object
     */
    <T extends Identifiable> T lockReload(T object);

    /**
     * Return batch run on the basis of the scheduler reference If the scheduler reference is not
     * unique, the most recent batch run is returned.
     *
     * @param schedulerReference scheduler reference
     */
    BatchRun getBatchRunBySchedulerReference(String schedulerReference);

    List<Batch> getViewableBatches();

    void loadLatestBatchRuns(Configuration config);

    Batch init(Batch b);

    Configuration init(Configuration c);

    Batch initHistory(Batch b);

    List<BatchRun> getCurrentBatchRuns();

    List<Batch> findBatches(String workspacePattern, String configNamePattern, String namePattern);

    List<Batch> findInitBatches(String workspacePattern, String configNamePattern);
}
