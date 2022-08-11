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

import java.io.Serializable;
import java.time.LocalTime;
import org.geotools.util.Converters;

/**
 * Stores values and configuration of system information metrics This object is serialized by
 * MonitorRest to provide XML, JSON and HTML view of data
 *
 * @author sandr
 */
public class MetricValue implements Serializable {

    private static final long serialVersionUID = 344784541680947799L;

    Object value;

    Boolean available;

    String description;

    String name;

    String unit;

    String category;

    String identifier;

    int priority;

    ValueHolder holder;

    /**
     * Initialize the metric value coping the definition from infomration obejct {@link MetricInfo}
     *
     * @param info the data associated with information to retrieve
     */
    public MetricValue(MetricInfo info) {
        this.priority = info.getPriority();
        this.name = info.name();
        this.description = info.getDescription();
        this.unit = info.getUnit();
        this.category = info.getCategory();
        this.identifier = info.name();
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        this.holder = new ValueHolder(value);
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getValueUnit() {
        if (!available || value == null) {
            return BaseSystemInfoCollector.DEFAULT_VALUE;
        }
        if (value instanceof Double || value instanceof Float) {
            final Number numberValue = (Number) value;
            return String.format(
                    "%.2f %s",
                    value instanceof Double ? numberValue.doubleValue() : numberValue.floatValue(),
                    unit);
        }
        if (unit != null && unit.equalsIgnoreCase("bytes")) {
            long bytes = Converters.convert(value, Long.class);
            return humanReadableByteCount(bytes);
        } else if (unit != null && unit.equalsIgnoreCase("sec")) {
            long seconds = Converters.convert(value, Long.class);
            return LocalTime.MIN.plusSeconds(seconds).toString();
        }
        return String.format("%s %s", value, unit);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Based on this article:
     * http://programming.guide/java/formatting-byte-size-to-human-readable-format.html
     */
    private static String humanReadableByteCount(long bytes) {
        // df -h and du -h use 1024 by default, system monitoring use MB
        int unit = 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %siB", bytes / Math.pow(unit, exp), pre);
    }

    /** Value holder used for XML and JSOn encoding. */
    public static class ValueHolder implements Serializable {

        private final Object valueOlder;

        public ValueHolder(Object valueOlder) {
            this.valueOlder = valueOlder;
        }

        public Object getValue() {
            return valueOlder;
        }
    }
}
