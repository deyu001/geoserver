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

package org.geoserver.taskmanager.schedule.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geotools.util.logging.Logging;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the batch job service.
 *
 * @author Niels Charlier
 */
@Service("batchJobService")
public class BatchJobServiceImpl
        implements BatchJobService, ApplicationListener<ContextRefreshedEvent>, TriggerListener {

    private static final Logger LOGGER = Logging.getLogger(BatchJobServiceImpl.class);

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private Scheduler scheduler;

    private boolean init = true;

    private Map<TriggerKey, Consumer<Batch>> callbacks = new HashMap<>();

    @Transactional("tmTransactionManager")
    protected void schedule(Batch batch) throws SchedulerException {
        // check for inactive tasks
        for (BatchElement be : batch.getElements()) {
            if (!be.getTask().isActive()) {
                throw new IllegalArgumentException(
                        "Cannot save & schedule a batch with inactive tasks!");
            }
        }

        JobKey jobKey = JobKey.jobKey(batch.getId().toString());

        boolean exists = scheduler.checkExists(jobKey);

        if (!batch.isActive()) {
            if (exists) {
                scheduler.deleteJob(jobKey);
            }

            LOGGER.log(Level.INFO, "Successfully unscheduled batch " + batch.getFullName());

        } else {
            if (!exists) {
                JobDetail jobDetail =
                        JobBuilder.newJob(BatchJobImpl.class)
                                .withIdentity(jobKey)
                                .storeDurably()
                                .build();

                scheduler.addJob(jobDetail, true);
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(batch.getId().toString());
            scheduler.unscheduleJob(triggerKey);

            if (batch.isEnabled()
                    && batch.getFrequency() != null
                    && !batch.getElements().isEmpty()
                    && (batch.getConfiguration() == null
                            || batch.getConfiguration().isValidated())) {
                Trigger trigger =
                        TriggerBuilder.newTrigger()
                                .withIdentity(triggerKey)
                                .forJob(jobKey)
                                .withSchedule(
                                        CronScheduleBuilder.cronSchedule(batch.getFrequency()))
                                .build();

                scheduler.scheduleJob(trigger);
            }

            LOGGER.log(Level.INFO, "Successfully (re)scheduled batch " + batch.getFullName());
        }
    }

    @Override
    @Transactional("tmTransactionManager")
    public Batch saveAndSchedule(Batch batch) {
        batch = dao.save(batch);
        if (batch.getConfiguration() == null || !batch.getConfiguration().isTemplate()) {
            try {
                schedule(batch);
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new IllegalArgumentException(e);
            }
        }
        return batch;
    }

    @Override
    @Transactional("tmTransactionManager")
    public Configuration saveAndSchedule(Configuration config) {
        config = dao.save(config);
        if (!config.isTemplate()) {
            try {
                for (Batch batch : config.getBatches().values()) {
                    schedule(batch);
                }
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new IllegalArgumentException(e);
            }
        }
        return config;
    }

    @Override
    @Transactional("tmTransactionManager")
    public Batch remove(Batch batch) {
        try {
            scheduler.deleteJob(JobKey.jobKey(batch.getId().toString()));
        } catch (SchedulerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalArgumentException(e);
        }
        return dao.remove(batch);
    }

    @Override
    @Transactional("tmTransactionManager")
    public Configuration remove(Configuration config) {
        config = dao.lockReload(config);
        for (Batch batch : config.getBatches().values()) {
            try {
                scheduler.deleteJob(JobKey.jobKey(batch.getId().toString()));
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new IllegalArgumentException(e);
            }
        }
        return dao.remove(config);
    }

    @Override
    @Transactional("tmTransactionManager")
    public void reloadFromData() {
        LOGGER.info("Reloading scheduler from data.");

        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            LOGGER.log(Level.WARNING, "Failed to clear scheduler ", e);
            throw new IllegalStateException(e);
        }

        for (Batch batch : dao.getAllBatches()) {
            try {
                schedule(batch);
            } catch (SchedulerException | IllegalArgumentException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to schedule batch " + batch.getName() + ", disabling. ",
                        e);
                batch.setEnabled(false);
                dao.save(batch);
            }
        }

        for (BatchRun br : dao.getCurrentBatchRuns()) {
            LOGGER.log(
                    Level.WARNING,
                    "Automatically closing inactive batch run at start-up: "
                            + br.getBatch().getFullName());
            dataUtil.closeBatchRun(br, "closed at start-up");
        }
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    @PostConstruct
    public void initialize() throws SchedulerException {
        scheduler.getListenerManager().addTriggerListener(this);
    }

    @Transactional("tmTransactionManager")
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // call only once at start-up, so not for child contexts.
        if (event.getApplicationContext().getParent() == null) {
            if (init) {
                reloadFromData();
            } else {
                LOGGER.info("Skipping initialization as specified in configuration.");
            }
            try {
                scheduler.start();
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @Override
    @Transactional("tmTransactionManager")
    public String scheduleNow(Batch batch) {
        batch = dao.reload(batch);
        if (batch.getElements().isEmpty()) {
            LOGGER.log(Level.WARNING, "Ignoring manual empty batch run: " + batch.getFullName());
            return null;
        }

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        try {
            scheduler.scheduleJob(trigger);
        } catch (SchedulerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return trigger.getKey().getName();
    }

    @Override
    @Transactional("tmTransactionManager")
    public void scheduleNow(Collection<Batch> batches, int waitInSeconds, int intervalInSeconds) {
        scheduleNow(batches, waitInSeconds, intervalInSeconds, null);
    }

    @Override
    @Transactional("tmTransactionManager")
    public void scheduleNow(
            Collection<Batch> batches,
            int waitInSeconds,
            int intervalInSeconds,
            Consumer<Batch> callback) {
        long time = System.currentTimeMillis() + waitInSeconds * 1000;
        for (Batch batch : batches) {
            batch = dao.reload(batch);
            if (batch.getElements().isEmpty()) {
                LOGGER.log(
                        Level.WARNING, "Ignoring manual empty batch run: " + batch.getFullName());
            }

            Trigger trigger =
                    TriggerBuilder.newTrigger()
                            .forJob(batch.getId().toString())
                            .startAt(new Date(time))
                            .build();
            if (callback != null) {
                callbacks.put(trigger.getKey(), callback);
            }
            try {
                scheduler.scheduleJob(trigger);
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }

            time += intervalInSeconds * 1000;
        }
    }

    @Override
    @Transactional("tmTransactionManager")
    public void interrupt(BatchRun batchRun) {
        batchRun = dao.lockReload(batchRun);
        if (!batchRun.getStatus().isClosed()) {
            if (batchRun.getSchedulerReference() != null) {
                try {
                    TriggerKey triggerKey = TriggerKey.triggerKey(batchRun.getSchedulerReference());
                    Trigger trigger = scheduler.getTrigger(triggerKey);
                    boolean lastFire = trigger != null ? (trigger.getNextFireTime() == null) : true;
                    TriggerState state = scheduler.getTriggerState(triggerKey);

                    // the blocked check only works thanks to @DisallowConcurrentExecution
                    // otherwise it would go straight back to waiting and we wouldn't know
                    // when the job was finished.
                    if ((lastFire && state == TriggerState.NONE)
                            || (!lastFire && state != TriggerState.BLOCKED)) {
                        dataUtil.closeBatchRun(batchRun, "manually closed due to inactivity");
                        return;
                    }
                } catch (SchedulerException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            batchRun.setInterruptMe(true);
            dao.save(batchRun);
        }
    }

    @Override
    public String getName() {
        return "batchJobService";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {}

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {}

    @Override
    public void triggerComplete(
            Trigger trigger,
            JobExecutionContext context,
            CompletedExecutionInstruction triggerInstructionCode) {
        Consumer<Batch> callback = callbacks.remove(trigger.getKey());
        if (callback != null) {
            try {
                Integer batchId =
                        Integer.parseInt((String) context.getJobDetail().getKey().getName());
                callback.accept(dao.getBatch(batchId));
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return;
            }
        }
    }
}
