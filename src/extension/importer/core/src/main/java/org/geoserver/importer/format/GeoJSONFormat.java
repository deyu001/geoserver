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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.data.FeatureReader;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GeoJSONFormat extends VectorFormat {

    static Logger LOG = Logging.getLogger(GeoJSONFormat.class);
    private static ReferencedEnvelope EMPTY_BOUNDS = new ReferencedEnvelope();

    @Override
    public FeatureReader read(ImportData data, ImportTask item) throws IOException {
        final SimpleFeatureType featureType = item.getFeatureType();
        FeatureJSON json = new FeatureJSON();
        json.setFeatureType(featureType);
        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        final FeatureIterator it = json.streamFeatureCollection(file(data, item));
        return new FeatureReader() {

            @Override
            public FeatureType getFeatureType() {
                return featureType;
            }

            @Override
            public boolean hasNext() throws IOException {
                return it.hasNext();
            }

            @Override
            public Feature next()
                    throws IOException, IllegalArgumentException, NoSuchElementException {
                return it.next();
            }

            @Override
            public void close() throws IOException {
                it.close();
            }
        };
    }

    @Override
    public void dispose(FeatureReader reader, ImportTask item) throws IOException {
        reader.close();
    }

    @Override
    public int getFeatureCount(ImportData data, ImportTask item) throws IOException {
        return -1;
    }

    @Override
    public String getName() {
        return "GeoJSON";
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        Optional<File> file = maybeFile(data);
        if (file.isPresent()) {
            return sniff(file.get()) != null;
        }

        return false;
    }

    SimpleFeature sniff(File file) {
        try {
            try (FeatureIterator it = new FeatureJSON().streamFeatureCollection(file)) {
                if (it.hasNext()) {
                    return (SimpleFeature) it.next();
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINER, "Error reading file as json", e);
        }
        return null;
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        // direct import not supported
        return null;
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {

        if (data instanceof Directory) {
            List<ImportTask> tasks = new ArrayList<>();
            for (FileData file : ((Directory) data).getFiles()) {
                tasks.add(task(file, catalog));
            }
            return tasks;
        } else {
            return Arrays.asList(task(data, catalog));
        }
    }

    ImportTask task(ImportData data, Catalog catalog) throws IOException {
        File file = maybeFile(data).get();
        CatalogFactory factory = catalog.getFactory();
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);

        // get the composite feature type
        SimpleFeatureType featureType = new FeatureJSON().readFeatureCollectionSchema(file, false);
        LOG.log(Level.FINE, featureType.toString());

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(featureType);
        tb.setName(FilenameUtils.getBaseName(file.getName()));
        featureType = tb.buildFeatureType();

        // create the feature type
        FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
        ft.setName(FilenameUtils.getBaseName(file.getName()));
        ft.setNativeName(ft.getName());

        List<AttributeTypeInfo> attributes = ft.getAttributes();
        for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
            AttributeTypeInfo att = factory.createAttribute();
            att.setName(ad.getLocalName());
            att.setBinding(ad.getType().getBinding());
            attributes.add(att);
        }

        // crs
        CoordinateReferenceSystem crs = null;
        if (featureType != null && featureType.getCoordinateReferenceSystem() != null) {
            crs = featureType.getCoordinateReferenceSystem();
        }
        try {
            crs = crs != null ? crs : CRS.decode("EPSG:4326");
        } catch (Exception e) {
            throw new IOException(e);
        }

        ft.setNativeCRS(crs);

        String srs = srs(crs);
        if (srs != null) {
            ft.setSRS(srs);
        }

        // bounds
        ft.setNativeBoundingBox(EMPTY_BOUNDS);
        ft.setLatLonBoundingBox(EMPTY_BOUNDS);
        ft.getMetadata().put("recalculate-bounds", Boolean.TRUE);

        LayerInfo layer = catalogBuilder.buildLayer((ResourceInfo) ft);

        ImportTask task = new ImportTask(data);
        task.setLayer(layer);

        task.setFeatureType(featureType);

        return task;
    }

    File file(ImportData data, final ImportTask item) {
        if (data instanceof Directory) {
            return Iterables.find(
                            ((Directory) data).getFiles(),
                            new Predicate<FileData>() {
                                @Override
                                public boolean apply(FileData input) {
                                    return FilenameUtils.getBaseName(input.getFile().getName())
                                            .equals(item.getLayer().getName());
                                }
                            })
                    .getFile();
        } else {
            return maybeFile(data).get();
        }
    }

    Optional<File> maybeFile(ImportData data) {
        if (data instanceof FileData) {
            return Optional.of(((FileData) data).getFile());
        }
        return Optional.absent();
    }

    String srs(CoordinateReferenceSystem crs) {
        Integer epsg = null;

        try {
            epsg = CRS.lookupEpsgCode(crs, false);
            if (epsg == null) {
                epsg = CRS.lookupEpsgCode(crs, true);
            }
        } catch (Exception e) {
            LOG.log(Level.FINER, "Error looking up epsg code", e);
        }
        return epsg != null ? "EPSG:" + epsg : null;
    }
}
