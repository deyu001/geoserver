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

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.geoserver.importer.transform;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Allow a string or number field to be used as a year Date. The number is interpreted as an
 * Integer.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class IntegerFieldToDateTransform extends AttributeRemapTransform {

    private static final long serialVersionUID = 1L;

    transient Calendar calendar;

    public IntegerFieldToDateTransform(String field) {
        super(field, Date.class);
    }

    public void init() {
        calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(Calendar.DAY_OF_YEAR, 1);
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object val = oldFeature.getAttribute(field);
        Date parsed = null;
        if (val instanceof String) {
            String s = ((String) val).trim();
            if (s.length() > 0) {
                val = Double.parseDouble(s);
            } else {
                val = null;
            }
        }
        if (val != null) {
            parsed = parseDate((Number) val);
        }
        feature.setAttribute(field, parsed);
        return feature;
    }

    private Date parseDate(Number val) {
        calendar.set(Calendar.YEAR, val.intValue());
        return calendar.getTime();
    }
}
