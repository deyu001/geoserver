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

package org.geoserver.importer.job;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.Importer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

public class JobQueue {

    static Logger LOGGER = Logging.getLogger(JobQueue.class);

    /** job id counter */
    AtomicLong counter = new AtomicLong();

    /** recent jobs */
    ConcurrentHashMap<Long, Task<?>> jobs = new ConcurrentHashMap<>();

    /** job runner */
    ThreadPoolExecutor pool =
            new ThreadPoolExecutor(
                    0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>()) {
                protected <T extends Object> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                    if (callable instanceof Job) {
                        return new Task<>((Job<T>) callable);
                    }
                    return super.newTaskFor(callable);
                };

                protected void beforeExecute(Thread t, Runnable r) {
                    if (t != null && r instanceof Task) {
                        ((Task) r).started();
                    }
                };

                protected void afterExecute(Runnable r, Throwable t) {
                    if (t != null && r instanceof Task) {
                        ((Task) r).setError(t);
                    }
                };
            };

    /** job cleaner */
    ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    {
        cleaner.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        List<Long> toremove = new ArrayList<>();
                        for (Map.Entry<Long, Task<?>> e : jobs.entrySet()) {
                            if (e.getValue().isCancelled()
                                    || (e.getValue()
                                            .isDone() /* AF: This condition is never verified ?!? && e.getValue().isRecieved() */)) {
                                try {
                                    ImportContext context = (ImportContext) e.getValue().get();

                                    if (context.getState() == ImportContext.State.COMPLETE
                                            && context.isEmpty()) {
                                        context.unlockUploadFolder(context.getUploadDirectory());
                                        toremove.add(e.getKey());
                                    }
                                } catch (Exception ex) {
                                    LOGGER.log(Level.INFO, ex.getMessage(), ex);
                                }
                            }
                        }
                        for (Long l : toremove) {
                            jobs.remove(l);
                        }

                        final Importer importer = GeoServerExtensions.bean(Importer.class);
                        File[] files = importer.getUploadRoot().listFiles();
                        if (files != null) {
                            for (File f : files) {
                                if (f.isDirectory() && new File(f, ".clean-me").exists()) {
                                    try {
                                        IOUtils.delete(f);
                                    } catch (IOException e) {
                                        LOGGER.log(
                                                Level.WARNING,
                                                "It was not possible to cleanup Importer temporary folder "
                                                        + f,
                                                e);
                                    }
                                }
                            }
                        }
                    }
                },
                60,
                60,
                TimeUnit.SECONDS);
    }

    public Long submit(Job<?> task) {
        Long jobid = counter.getAndIncrement();
        Task t = (Task) pool.submit(task);
        t.setId(jobid);

        jobs.put(jobid, t);
        return jobid;
    }

    public Task<?> getTask(Long jobid) {
        Task<?> t = jobs.get(jobid);
        t.recieve();
        return t;
    }

    public List<Task<?>> getTasks() {
        return new ArrayList<>(jobs.values());
    }

    public void shutdown() {
        cleaner.shutdownNow();
        pool.shutdownNow();
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        pool.setMaximumPoolSize(maximumPoolSize);
    }

    public int getMaximumPoolSize() {
        return pool.getMaximumPoolSize();
    }
}
