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

package org.geoserver.backuprestore.listener;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;

/**
 * Implements a Spring Batch {@link JobExecutionListener}.
 *
 * <p>It's used to perform operations accordingly to the {@link Backup} batch {@link JobExecution}
 * status.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RestoreJobExecutionListener implements JobExecutionListener {

    static Logger LOGGER = Logging.getLogger(RestoreJobExecutionListener.class);

    private Backup backupFacade;

    private RestoreExecutionAdapter restoreExecution;

    public RestoreJobExecutionListener(Backup backupFacade) {
        this.backupFacade = backupFacade;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Prior starting the JobExecution, lets store a new empty GeoServer Catalog into the
        // context.
        // It will be used to load the resources on a temporary in-memory configuration, which will
        // be
        // swapped at the end of the Restore if everything goes well.
        if (backupFacade.getRestoreExecutions().get(jobExecution.getId()) != null) {
            this.restoreExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());
            this.restoreExecution.setRestoreCatalog(
                    createRestoreCatalog(jobExecution.getJobParameters()));
        } else {
            Long id = null;
            RestoreExecutionAdapter rst = null;

            for (Entry<Long, RestoreExecutionAdapter> entry :
                    backupFacade.getRestoreExecutions().entrySet()) {
                id = entry.getKey();
                rst = entry.getValue();

                if (rst.getJobParameters()
                        .getLong(Backup.PARAM_TIME)
                        .equals(jobExecution.getJobParameters().getLong(Backup.PARAM_TIME))) {
                    break;
                } else {
                    id = null;
                    rst = null;
                }
            }

            if (rst != null) {
                Resource archiveFile = rst.getArchiveFile();
                Catalog restoreCatalog = rst.getRestoreCatalog();
                List<String> options = rst.getOptions();

                this.backupFacade.getRestoreExecutions().remove(id);

                this.restoreExecution =
                        new RestoreExecutionAdapter(
                                jobExecution, backupFacade.getTotalNumberOfRestoreSteps());
                this.restoreExecution.setArchiveFile(archiveFile);
                this.restoreExecution.setRestoreCatalog(restoreCatalog);
                this.restoreExecution.setWsFilter(rst.getWsFilter());
                this.restoreExecution.setSiFilter(rst.getSiFilter());
                this.restoreExecution.setLiFilter(rst.getLiFilter());

                this.restoreExecution.getOptions().addAll(options);

                this.backupFacade
                        .getRestoreExecutions()
                        .put(jobExecution.getId(), this.restoreExecution);
            }
        }
    }

    private synchronized Catalog createRestoreCatalog(JobParameters params) {
        boolean purge = getPurgeResources(params);
        CatalogImpl restoreCatalog = new CatalogImpl();
        Catalog gsCatalog = unwrapGsCatalog();

        restoreCatalog.setResourceLoader(gsCatalog.getResourceLoader());
        restoreCatalog.setResourcePool(gsCatalog.getResourcePool());
        // only synchronize catalogs if purge flag is not set to true
        if (!purge) {
            syncCatalogs(restoreCatalog, gsCatalog);
        }

        for (CatalogListener listener : gsCatalog.getListeners()) {
            restoreCatalog.addListener(listener);
        }

        return restoreCatalog;
    }

    private Catalog unwrapGsCatalog() {
        Catalog gsCatalog = backupFacade.getGeoServer().getCatalog();
        if (gsCatalog instanceof Wrapper) {
            gsCatalog = ((Wrapper) gsCatalog).unwrap(Catalog.class);
        }
        return gsCatalog;
    }

    private boolean getPurgeResources(JobParameters params) {
        String value = params.getString(Backup.PARAM_PURGE_RESOURCES);
        if (value == null) return false;
        return Boolean.valueOf(value.trim());
    }

    /** Synchronizes catalogs content. */
    private void syncCatalogs(CatalogImpl restoreCatalog, Catalog gsCatalog) {
        LOGGER.fine("Synchronizing catalogs items.");
        if (gsCatalog instanceof CatalogImpl) {
            restoreCatalog.sync((CatalogImpl) gsCatalog);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        boolean dryRun =
                Boolean.parseBoolean(
                        jobExecution
                                .getJobParameters()
                                .getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        boolean bestEffort =
                Boolean.parseBoolean(
                        jobExecution
                                .getJobParameters()
                                .getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));
        try {
            final Long executionId = jobExecution.getId();

            this.restoreExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());

            LOGGER.fine("Running Executions IDs : " + executionId);

            if (jobExecution.getStatus() != BatchStatus.STOPPED) {
                LOGGER.fine(
                        "Executions Step Summaries : "
                                + backupFacade
                                        .getJobOperator()
                                        .getStepExecutionSummaries(executionId));
                LOGGER.fine(
                        "Executions Parameters : "
                                + backupFacade.getJobOperator().getParameters(executionId));
                LOGGER.fine(
                        "Executions Summary : "
                                + backupFacade.getJobOperator().getSummary(executionId));

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    cleanUp();
                }
            }
            // Collect errors
        } catch (Exception e) {
            if (!bestEffort) {
                this.restoreExecution.addFailureExceptions(Arrays.asList(e));
                throw new RuntimeException(e);
            } else {
                this.restoreExecution.addWarningExceptions(Arrays.asList(e));
            }
        }
    }

    /** Clean up temp folder if required on settings. */
    private void cleanUp() {
        JobParameters jobParameters = restoreExecution.getJobParameters();
        Resource tempFolder =
                Resources.fromURL(jobParameters.getString(Backup.PARAM_INPUT_FILE_PATH));

        // Cleanup Temporary Resources
        String cleanUpTempFolders = jobParameters.getString(Backup.PARAM_CLEANUP_TEMP);
        if (cleanUpTempFolders != null
                && Boolean.parseBoolean(cleanUpTempFolders)
                && tempFolder != null) {
            if (Resources.exists(tempFolder)) {
                try {
                    if (!tempFolder.delete()) {
                        LOGGER.warning(
                                "It was not possible to cleanup Temporary Resources. Please double check that Resources inside the Temp GeoServer Data Directory have been removed.");
                    }
                } catch (Exception e) {
                    LOGGER.log(
                            Level.WARNING,
                            "It was not possible to cleanup Temporary Resources. Please double check that Resources inside the Temp GeoServer Data Directory have been removed.",
                            e);
                }
            }
        }
    }
}
