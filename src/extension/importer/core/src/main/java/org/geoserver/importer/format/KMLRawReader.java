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

package org.geoserver.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xsd.PullParser;
import org.opengis.feature.simple.SimpleFeatureType;

public class KMLRawReader implements Iterable<Object>, Iterator<Object> {

    private final PullParser parser;

    private Object next;

    public static enum ReadType {
        FEATURES,
        SCHEMA_AND_FEATURES
    }

    public KMLRawReader(InputStream inputStream) {
        this(inputStream, KMLRawReader.ReadType.FEATURES, null);
    }

    public KMLRawReader(InputStream inputStream, KMLRawReader.ReadType readType) {
        this(inputStream, readType, null);
    }

    public KMLRawReader(
            InputStream inputStream,
            KMLRawReader.ReadType readType,
            SimpleFeatureType featureType) {
        if (KMLRawReader.ReadType.SCHEMA_AND_FEATURES.equals(readType)) {
            if (featureType == null) {
                parser =
                        new PullParser(
                                new KMLConfiguration(), inputStream, KML.Placemark, KML.Schema);
            } else {
                parser =
                        new PullParser(
                                new KMLConfiguration(),
                                inputStream,
                                pullParserArgs(
                                        featureTypeSchemaNames(featureType),
                                        KML.Placemark,
                                        KML.Schema));
            }
        } else if (KMLRawReader.ReadType.FEATURES.equals(readType)) {
            if (featureType == null) {
                parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark);
            } else {
                parser =
                        new PullParser(
                                new KMLConfiguration(),
                                inputStream,
                                pullParserArgs(featureTypeSchemaNames(featureType), KML.Placemark));
            }
        } else {
            throw new IllegalArgumentException("Unknown parse read type: " + readType.toString());
        }
        next = null;
    }

    private Object[] pullParserArgs(List<QName> featureTypeSchemaNames, Object... args) {
        Object[] parserArgs = new Object[featureTypeSchemaNames.size() + args.length];
        System.arraycopy(args, 0, parserArgs, 0, args.length);
        System.arraycopy(
                featureTypeSchemaNames.toArray(),
                0,
                parserArgs,
                args.length,
                featureTypeSchemaNames.size());
        return parserArgs;
    }

    @SuppressWarnings("unchecked")
    private List<QName> featureTypeSchemaNames(SimpleFeatureType featureType) {
        Map<Object, Object> userData = featureType.getUserData();
        if (userData.containsKey("schemanames")) {
            List<String> names = (List<String>) userData.get("schemanames");
            List<QName> qnames = new ArrayList<>(names.size());
            for (String name : names) {
                qnames.add(new QName(name));
            }
            return qnames;
        }
        return Collections.emptyList();
    }

    private Object read() throws IOException {
        Object parsedObject;
        try {
            parsedObject = parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        return parsedObject;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = read();
        } catch (IOException e) {
            next = null;
        }
        return next != null;
    }

    @Override
    public Object next() {
        if (next != null) {
            Object result = next;
            next = null;
            return result;
        }
        Object feature;
        try {
            feature = read();
        } catch (IOException e) {
            feature = null;
        }
        if (feature == null) {
            throw new NoSuchElementException();
        }
        return feature;
    }

    @Override
    public Iterator<Object> iterator() {
        return this;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
