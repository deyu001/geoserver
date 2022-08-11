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

package org.geoserver.system.status;

/** This enum defines the system metrics that can be retrieved by a collector. */
public enum MetricInfo {

    // system metrics
    OPERATING_SYSTEM("SYSTEM", 1, "Operating system"),
    SYSTEM_UPTIME("SYSTEM", 2, "Uptime", "sec"),
    SYSTEM_AVERAGE_LOAD_1("SYSTEM", 3, "System average load 1 minute", ""),
    SYSTEM_AVERAGE_LOAD_5("SYSTEM", 3, "System average load 5 minutes", ""),
    SYSTEM_AVERAGE_LOAD_15("SYSTEM", 3, "System average load 15 minutes", ""),
    // cpu metrics
    PHYSICAL_CPUS("CPU", 100, "Number of physical CPUs"),
    LOGICAL_CPUS("CPU", 101, "Number of logical CPUs"),
    RUNNING_PROCESS("CPU", 102, "Number of running process"),
    RUNNING_THREADS("CPU", 103, "Number of running threads"),
    CPU_LOAD("CPU", 104, "CPU average load", "%"),
    PER_CPU_LOAD("CPU", 105, "CPU load", "%"),
    // memory metrics
    MEMORY_USED("MEMORY", 200, "Used physical memory ", "%"),
    MEMORY_TOTAL("MEMORY", 201, "Total physical memory ", "bytes"),
    MEMORY_FREE("MEMORY", 201, "Free physical memory", "bytes"),
    // swap metrics
    SWAP_USED("SWAP", 300, "Used swap memory", "%"),
    SWAP_TOTAL("SWAP", 301, "Total swap memory", "bytes"),
    SWAP_FREE("SWAP", 302, "Free swap memory", "bytes"),
    // file system metrics
    FILE_SYSTEM_TOTAL_USAGE("FILE_SYSTEM", 400, "File system usage", "%"),
    PARTITION_USED("FILE_SYSTEM", 500, "Partition space used", "%"),
    PARTITION_TOTAL("FILE_SYSTEM", 501, "Partition total space", "bytes"),
    PARTITION_FREE("FILE_SYSTEM", 502, "Partition free space", "bytes"),
    // network metrics
    NETWORK_INTERFACES_SEND("NETWORK", 800, "Network interfaces send", "bytes"),
    NETWORK_INTERFACES_RECEIVED("NETWORK", 801, "Network interfaces received", "bytes"),
    NETWORK_INTERFACE_SEND("NETWORK", 900, "Network interface band usage", "bytes"),
    NETWORK_INTERFACE_RECEIVED("NETWORK", 901, "Network interface available band", "bytes"),
    // sensors metrics
    TEMPERATURE("SENSORS", 1200, "CPU temperature", "°C"),
    VOLTAGE("SENSORS", 1201, "CPU voltage", "V"),
    FAN_SPEED("SENSORS", 1202, "Fan speed", "rpm"),
    // geoserver metrics
    GEOSERVER_CPU_USAGE("GEOSERVER", 1300, "GeoServer CPU usage", "%"),
    GEOSERVER_THREADS("GEOSERVER", 1301, "GeoServer threads"),
    GEOSERVER_JVM_MEMORY_USAGE("GEOSERVER", 1302, "GeoServer JVM memory usage", "%");

    private String category;
    private int priority;
    private String description;
    private String unit;

    MetricInfo(String category, int priority, String description) {
        this(category, priority, description, "");
    }

    MetricInfo(String category, int priority, String description, String unit) {
        this.description = description;
        this.unit = unit;
        this.category = category;
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public String getCategory() {
        return category;
    }

    public int getPriority() {
        return priority;
    }
}
