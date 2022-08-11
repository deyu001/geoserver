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

package org.geoserver.wps;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.ProgressListener;

@DescribeProcess(title = "Monkey", description = "Process used to test asynch calls")
public class MonkeyProcess {

    enum CommandType {
        Exit,
        SetProgress,
        Exception,
        Wait
    }

    static Map<String, BlockingQueue<Command>> commands = new ConcurrentHashMap<>();

    private static class Command {
        CommandType type;

        Object value;

        public Command(CommandType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    public static void exit(String id, SimpleFeatureCollection value, boolean wait)
            throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Exit, value));
        if (wait) {
            while (getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    private static synchronized BlockingQueue<Command> getCommandQueue(String id) {
        BlockingQueue<Command> queue = commands.get(id);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>();
            commands.put(id, queue);
        }

        return queue;
    }

    public static void progress(String id, float progress, boolean wait)
            throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.SetProgress, progress));
        if (wait) {
            while (getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    public static void wait(String id, long wait) throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Wait, wait));
    }

    public static void exception(String id, ProcessException exception, boolean wait)
            throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Exception, exception));
        if (wait) {
            while (getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    @DescribeResult(name = "result")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "id") String id,
            @DescribeParameter(name = "fc", min = 0) SimpleFeatureCollection fc,
            @DescribeParameter(name = "extra", min = 0) String extra,
            ProgressListener listener)
            throws Exception {
        BlockingQueue<Command> queue = getCommandQueue(id);
        while (true) {
            Command command = queue.take();
            if (command.type == CommandType.Exit) {
                listener.progress(100f);
                listener.complete();
                commands.remove(id);
                return (SimpleFeatureCollection) command.value;
            } else if (command.type == CommandType.SetProgress) {
                float progress = ((Number) command.value).floatValue();
                listener.progress(progress);
                listener.setTask(new SimpleInternationalString("Currently at " + progress));
            } else if (command.type == CommandType.Wait) {
                long wait = ((Number) command.value).longValue();
                Thread.sleep(wait);
            } else {
                ProcessException exception = (ProcessException) command.value;
                listener.exceptionOccurred(exception);
                throw exception;
            }
        }
    }

    public static final ProcessFactory getFactory() {
        return new MonkeyProcessFactory();
    }

    public static void clearCommands() {
        for (Map.Entry<String, BlockingQueue<MonkeyProcess.Command>> entry : commands.entrySet()) {
            if (entry.getValue().size() > 0) {
                throw new IllegalStateException(
                        "The command queue is not clean, queue "
                                + entry.getKey()
                                + " still has commands in: "
                                + entry.getValue());
            }
        }

        commands.clear();
    }

    private static class MonkeyProcessFactory extends AnnotatedBeanProcessFactory {

        public MonkeyProcessFactory() {
            super(new SimpleInternationalString("Monkey process"), "gs", MonkeyProcess.class);
        }
    }
}
