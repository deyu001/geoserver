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
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;

/**
 * Implements a Spring Batch {@link JobExecutionListener}.
 *
 * <p>It's used to perform operations accordingly to the {@link Backup} batch {@link JobExecution}
 * status.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class BackupJobExecutionListener implements JobExecutionListener {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(BackupJobExecutionListener.class);

    private Backup backupFacade;

    private BackupExecutionAdapter backupExecution;

    public BackupJobExecutionListener(Backup backupFacade) {
        this.backupFacade = backupFacade;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if (backupFacade.getBackupExecutions().get(jobExecution.getId()) != null) {
            this.backupExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
        } else {
            Long id = null;
            BackupExecutionAdapter bkp = null;

            for (Entry<Long, BackupExecutionAdapter> entry :
                    backupFacade.getBackupExecutions().entrySet()) {
                id = entry.getKey();
                bkp = entry.getValue();

                if (bkp.getJobParameters()
                        .getLong(Backup.PARAM_TIME)
                        .equals(jobExecution.getJobParameters().getLong(Backup.PARAM_TIME))) {
                    break;
                } else {
                    id = null;
                    bkp = null;
                }
            }

            if (bkp != null) {
                Resource archiveFile = bkp.getArchiveFile();
                boolean overwrite = bkp.isOverwrite();
                List<String> options = bkp.getOptions();

                this.backupFacade.getBackupExecutions().remove(id);

                this.backupExecution =
                        new BackupExecutionAdapter(
                                jobExecution, backupFacade.getTotalNumberOfBackupSteps());
                this.backupExecution.setArchiveFile(archiveFile);
                this.backupExecution.setOverwrite(overwrite);
                this.backupExecution.setWsFilter(bkp.getWsFilter());
                this.backupExecution.setSiFilter(bkp.getSiFilter());
                this.backupExecution.setLiFilter(bkp.getLiFilter());
                this.backupExecution.getOptions().addAll(options);

                this.backupFacade
                        .getBackupExecutions()
                        .put(jobExecution.getId(), this.backupExecution);
            }
        }
    }

    @SuppressWarnings("unused")
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
                    JobParameters jobParameters = backupExecution.getJobParameters();
                    Resource sourceFolder =
                            Resources.fromURL(
                                    jobParameters.getString(Backup.PARAM_OUTPUT_FILE_PATH));

                    // Cleanup Temporary Resources
                    String cleanUpTempFolders = jobParameters.getString(Backup.PARAM_CLEANUP_TEMP);
                    if (cleanUpTempFolders != null
                            && Boolean.parseBoolean(cleanUpTempFolders)
                            && sourceFolder != null) {
                        if (Resources.exists(sourceFolder)) {
                            try {
                                if (!sourceFolder.delete()) {
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
        } catch (NoSuchJobExecutionException e) {
            if (!bestEffort) {
                this.backupExecution.addFailureExceptions(Arrays.asList(e));
                throw new RuntimeException(e);
            } else {
                this.backupExecution.addWarningExceptions(Arrays.asList(e));
            }
        }
    }
}
