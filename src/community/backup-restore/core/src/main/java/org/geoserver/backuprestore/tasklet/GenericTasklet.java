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

package org.geoserver.backuprestore.tasklet;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/** Extension point for generic backup / restore jobs. */
public final class GenericTasklet extends AbstractCatalogBackupRestoreTasklet {

    // key used to register in the job context the handlers that need to run again
    private static final String GENERIC_CONTINUABLE_HANDLERS_KEY = "GENERIC_CONTINUABLE_HANDLERS";

    public GenericTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        // this is invoked for each run, which means that this may be invoked multiple times for
        // continuable handlers
        getAllHandlers().forEach(handler -> handler.initialize(stepExecution, this));
    }

    @Override
    public RepeatStatus doExecute(
            StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        // get the available generic handlers or the continuable ones
        List<GenericTaskletHandler> handlers = getHandlers(jobExecution);
        List<GenericTaskletHandler> continuable = new ArrayList<>();
        // execute each handler and store the continuable ones
        handlers.forEach(
                handler -> {
                    RepeatStatus status =
                            handler.handle(contribution, chunkContext, jobExecution, this);
                    if (status == RepeatStatus.CONTINUABLE) {
                        continuable.add(handler);
                    }
                });
        // register the continuable ones overriding the existing ones
        putContinuableHandlers(jobExecution, continuable);
        if (continuable.isEmpty()) {
            // no continuable jobs, we are done
            return RepeatStatus.FINISHED;
        }
        // there is continuable jobs
        return RepeatStatus.CONTINUABLE;
    }

    /**
     * Put the provided continuable jobs in the job execution context overriding any existing ones.
     */
    private void putContinuableHandlers(
            JobExecution jobExecution, List<GenericTaskletHandler> handlers) {
        jobExecution.getExecutionContext().put(GENERIC_CONTINUABLE_HANDLERS_KEY, handlers);
    }

    /**
     * Helper method that return the handlers that should be executed. If there is any pending
     * continuable handler we only run the pending continuable handlers otherwise we run all the
     * available handlers.
     */
    @SuppressWarnings("unchecked")
    private List<GenericTaskletHandler> getHandlers(JobExecution jobExecution) {
        // let's see if we have any pending continuable jobs
        Object value = jobExecution.getExecutionContext().get(GENERIC_CONTINUABLE_HANDLERS_KEY);
        if (value == null || !List.class.isAssignableFrom(value.getClass())) {
            // no pending continuable handlers, use the normal handlers
            return getAllHandlers();
        }
        List values = (List) value;
        if (values.isEmpty()
                || !GenericTaskletHandler.class.isAssignableFrom(values.get(0).getClass())) {
            // not what we expect, use the normal handlers
            return getAllHandlers();
        }
        // pending continuable handlers
        return (List<GenericTaskletHandler>) values;
    }

    /**
     * Helper method that just retrieves all the available generic handlers contributed by
     * extensions.
     */
    private List<GenericTaskletHandler> getAllHandlers() {
        return GeoServerExtensions.extensions(GenericTaskletHandler.class);
    }
}
