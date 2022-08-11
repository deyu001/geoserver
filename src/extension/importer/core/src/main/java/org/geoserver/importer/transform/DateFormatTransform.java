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

package org.geoserver.importer.transform;

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.importer.DatePattern;
import org.geoserver.importer.Dates;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ValidationException;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Transform that converts a non date attribute in a date attribute. This class is not thread-safe.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DateFormatTransform extends AttributeRemapTransform {

    private static final long serialVersionUID = 1L;

    DatePattern datePattern;

    private String enddate;

    private String presentation;

    /**
     * Default Constructor taking two parameters - [mandatory] The field used as time dimension -
     * [optional] The date-time pattern to be used in case of String fields
     */
    public DateFormatTransform(String field, String datePattern) throws ValidationException {
        this(field, datePattern, null, null);
    }

    /**
     * Default Constructor taking four parameters - [mandatory] The field used as time dimension -
     * [optional] The date-time pattern to be used in case of String fields - [optional] The field
     * used as end date for the time dimension - [optional] The time dimension presentation type;
     * one of {LIST; DISCRETE_INTERVAL; CONTINUOUS_INTERVAL}
     */
    public DateFormatTransform(
            String field, String datePattern, String enddate, String presentation)
            throws ValidationException {
        init(field, datePattern, enddate, presentation);
        init();
    }

    DateFormatTransform() {
        this(null, null);
    }

    public DatePattern getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(DatePattern datePattern) {
        this.datePattern = datePattern;
    }

    /** @return the enddate */
    public String getEnddate() {
        return enddate;
    }

    /** @param enddate the enddate to set */
    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    /** @return the presentation */
    public String getPresentation() {
        return presentation;
    }

    /** @param presentation the presentation to set */
    public void setPresentation(String presentation) {
        this.presentation = presentation;
    }

    private void init(String field, String datePattern, String enddate, String presentation)
            throws ValidationException {
        setType(Date.class);
        setField(field);
        if (datePattern != null) {
            this.datePattern = new DatePattern(datePattern, null, true, false);

            // parse the date format to ensure its legal
            try {
                this.datePattern.dateFormat();
            } catch (IllegalArgumentException iae) {
                throw new ValidationException("Invalid date parsing format", iae);
            }
        }
        this.enddate = enddate;
        this.presentation = presentation != null ? presentation : "LIST";
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object val = oldFeature.getAttribute(field);
        if (val != null) {
            Date parsed = (val instanceof Date ? (Date) val : parseDate(val.toString()));
            if (parsed == null) {
                task.addMessage(
                        Level.WARNING,
                        "Invalid date '" + val + "' specified for " + feature.getID());
                feature = null;
            } else {
                feature.setAttribute(field, parsed);

                if (enddate != null) {
                    val = oldFeature.getAttribute(field);
                    if (val != null) {
                        parsed = (val instanceof Date ? (Date) val : parseDate(val.toString()));
                        if (parsed != null) {
                            feature.setAttribute(enddate, parsed);
                        }
                    }
                }
            }
        }

        // set up the time dimension object
        if (task.getLayer() != null) {
            ResourceInfo r = task.getLayer().getResource();
            if (r != null && r.getMetadata().get(ResourceInfo.TIME) == null) {
                DimensionInfo dim = new DimensionInfoImpl();
                dim.setEnabled(true);
                dim.setAttribute(field);
                dim.setEndAttribute(enddate);
                dim.setPresentation(DimensionPresentation.valueOf(presentation));
                dim.setUnits("ISO8601"); // TODO: is there an enumeration for this?

                r.getMetadata().put(ResourceInfo.TIME, dim);
            }
        }

        return feature;
    }

    public Date parseDate(String value) throws ParseException {
        Date parsed = null;

        // if a format was provided, use it
        if (datePattern != null) {
            parsed = datePattern.parse(value);
        }

        // fall back to others
        if (parsed == null) {
            parsed = Dates.parse(value);
        }
        if (parsed != null) {
            return parsed;
        }

        throw new ParseException("Invalid date '" + value + "'", 0);
    }
}
