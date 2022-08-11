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

package org.geoserver.csw.store.simple;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.cat.csw20.RecordType;
import net.opengis.cat.csw20.SimpleLiteral;
import net.opengis.ows10.BoundingBoxType;
import org.geoserver.csw.records.CSWRecordBuilder;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.csw.CSWConfiguration;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.Parser;
import org.opengis.feature.Feature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Builds features scanning xml files in the specified folder, and parsing them as CSW Record
 * objects
 *
 * @author Andrea Aime - GeoSolutions
 */
class SimpleRecordIterator implements Iterator<Feature> {

    static final Logger LOGGER = Logging.getLogger(SimpleRecordIterator.class);

    Iterator<Resource> files;

    RecordType record;

    Resource lastFile;

    Parser parser;

    CSWRecordBuilder builder = new CSWRecordBuilder();

    int offset;

    public SimpleRecordIterator(Resource root, int offset) {
        List<Resource> fileArray = Resources.list(root, new Resources.ExtensionFilter("XML"));
        files = fileArray.iterator();
        parser = new Parser(new CSWConfiguration());
        this.offset = offset;
    }

    @Override
    public boolean hasNext() {
        while ((record == null || offset > 0) && files.hasNext()) {
            Resource file = files.next();
            lastFile = file;
            try (InputStream is = file.in()) {
                record = (RecordType) parser.parse(is);
                if (offset > 0) {
                    offset--;
                    record = null;
                }
            } catch (Exception e) {
                LOGGER.log(
                        Level.INFO,
                        "Failed to parse the contents of " + file.path() + " as a CSW Record",
                        e);
            }
        }

        return record != null;
    }

    @Override
    public Feature next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more records to retrieve");
        }

        Feature next = convertToFeature(record);
        record = null;
        return next;
    }

    private Feature convertToFeature(RecordType r) {
        String id = null;

        // add all the elements
        for (SimpleLiteral sl : r.getDCElement()) {
            Object value = sl.getValue();
            String scheme = sl.getScheme() == null ? null : sl.getScheme().toString();
            String name = sl.getName();
            if (value != null && sl.getName() != null) {
                builder.addElementWithScheme(name, scheme, value.toString());
                if ("identifier".equals(name)) {
                    id = value.toString();
                }
            }
        }

        // move on to the bounding boxes
        for (BoundingBoxType bbox : r.getBoundingBox()) {
            if (bbox != null) {
                CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
                if (bbox.getCrs() != null) {
                    try {
                        crs = CRS.decode(bbox.getCrs());
                    } catch (Exception e) {
                        LOGGER.log(Level.INFO, "Failed to parse original record bbox");
                    }
                }
                ReferencedEnvelope re =
                        new ReferencedEnvelope(
                                (Double) bbox.getLowerCorner().get(0),
                                (Double) bbox.getUpperCorner().get(0),
                                (Double) bbox.getLowerCorner().get(1),
                                (Double) bbox.getUpperCorner().get(1),
                                crs);
                builder.addBoundingBox(re);
            }
        }

        return builder.build(id);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This iterator is read only");
    }

    public Resource getLastFile() {
        return lastFile;
    }
}
