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

package org.geoserver.monitor;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * A task queue that groups tasks by key and ensures that tasks with same key execute serially.
 *
 * @author Justin Deoliveira, OpenGeo
 * @param <K> The key type.
 */
public class PipeliningTaskQueue<K> implements Runnable {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    volatile ConcurrentHashMap<K, Queue<Pipelineable<K>>> pipelines;
    ScheduledExecutorService executor;
    ExecutorService tasks;

    public PipeliningTaskQueue() {
        pipelines = new ConcurrentHashMap<>();
        tasks = Executors.newCachedThreadPool();
    }

    public void start() {
        executor = Executors.newScheduledThreadPool(4);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        executor.shutdown();
        executor = null;

        tasks.shutdown();
        tasks = null;
    }

    public void execute(K key, Runnable task) {
        execute(key, task, "");
    }

    public void execute(K key, Runnable task, String desc) {
        Queue<Pipelineable<K>> pipeline = pipelines.get(key);
        if (pipeline == null) {
            synchronized (this) {
                pipeline = pipelines.get(key);
                if (pipeline == null) {
                    pipeline = new ConcurrentLinkedQueue<>();
                    Queue<Pipelineable<K>> other = pipelines.putIfAbsent(key, pipeline);
                    if (other != null) pipeline = other;
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Queuing task into pipeline " + key);
        }
        pipeline.add(new Pipelineable<>(key, task));
    }

    public void clear(K key) {
        pipelines.remove(key);
    }

    public void shutdown() {
        executor.shutdown();
        tasks.shutdown();
    }

    public void run() {

        for (Queue<Pipelineable<K>> pipeline : pipelines.values()) {
            Pipelineable<K> job = pipeline.peek();
            if (job != null) {
                if (!job.lock.tryLock()) continue; // another thread already handling this job

                if (job.future != null) {
                    // job has been submitted, if it is done remove it from
                    // the queue
                    if (job.future.isDone()) {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Removing task from queue " + job.key);
                        }
                        pipeline.remove();
                    }
                } else {
                    // start the job
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Executing task in queue " + job.key);
                    }
                    job.future = tasks.submit(job.task);
                }

                job.lock.unlock();
            }
        }
    }

    public class Pipelineable<K> {

        K key;
        Runnable task;
        Future<?> future;
        Lock lock;
        String desc;

        public Pipelineable(K key, Runnable task) {
            this.key = key;
            this.task = task;
            this.lock = new ReentrantLock();
        }
    }
}
