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

package org.geoserver.backuprestore.imagemosaic.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.imagemosaic.ImageMosaicAdditionalResource;
import org.geoserver.backuprestore.reader.CatalogAdditionalResourcesReader;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/** @author Alessio Fabiani, GeoSolutions */
public class ImageMosaicAdditionalResourceReader extends ImageMosaicAdditionalResource
        implements CatalogAdditionalResourcesReader<StoreInfo> {

    final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

    @Override
    public boolean canHandle(Object item) {
        if (item instanceof CoverageStoreInfo
                && ((CoverageStoreInfo) item).getType().equals(COVERAGE_TYPE)) {
            return true;
        }
        return false;
    }

    @Override
    public void readAdditionalResources(Backup backupFacade, Resource base, StoreInfo item)
            throws IOException {

        final Resource sourceBackupFolder =
                BackupUtils.dir(base.parent(), IMAGEMOSAIC_INDEXES_FOLDER);

        final CoverageStoreInfo mosaicCoverageStore =
                backupFacade.getCatalog().getResourcePool().clone((CoverageStoreInfo) item, true);
        final String mosaicName = mosaicCoverageStore.getName();
        final String mosaicUrlBase = mosaicCoverageStore.getURL();

        final Resource mosaicIndexBase = Resources.fromURL(mosaicUrlBase);

        List<Resource> mosaicIndexerResources =
                Resources.list(sourceBackupFolder, resources.get("properties"), true);

        mosaicIndexerResources.addAll(
                Resources.list(sourceBackupFolder, resources.get("info"), true));

        boolean datastoreAlreadyPresent = true;
        for (Resource res : mosaicIndexerResources) {
            if (!FilenameUtils.getBaseName(res.name()).equals(mosaicName)
                    && Resources.exists(res)
                    && Resources.canRead(res)) {
                boolean result = copyFile(sourceBackupFolder, mosaicIndexBase, res, false);

                if (result && FilenameUtils.getBaseName(res.name()).equals("datastore")) {
                    // The copy of the new "datastore.properties" was successful, meaning that
                    // there wasn't an other copy of that file on the target folder.
                    datastoreAlreadyPresent = false;
                }
            }
        }

        List<Resource> mosaicIndexerTemplateResources =
                Resources.list(sourceBackupFolder, resources.get("templates"), true);

        for (Resource res : mosaicIndexerTemplateResources) {
            if (Resources.exists(res) && Resources.canRead(res)) {
                boolean result = copyFile(sourceBackupFolder, mosaicIndexBase, res, true);

                if (result) {
                    resolveTemplate(sourceBackupFolder, mosaicIndexBase, res);
                }
            }
        }

        if (!datastoreAlreadyPresent) {
            // Sine there wasn't already a "datasotre.properties" on the target folder
            // we assume this is a new mosaic.
            // We need to be sure the property "CanBeEmpty=true" is present on the
            // "indexer.properties"
            final File indexerFile = new File(mosaicIndexBase.dir(), "indexer.properties");

            Properties indexerProperties = new Properties();

            if (indexerFile.exists() && indexerFile.canRead()) {
                indexerProperties.load(new FileInputStream(indexerFile));
            }

            indexerProperties.setProperty("CanBeEmpty", "true");

            indexerProperties.store(new FileOutputStream(indexerFile), null);
        }
    }

    /** */
    private void resolveTemplate(
            final Resource sourceBackupFolder, final Resource mosaicIndexBase, Resource res)
            throws IOException, FileNotFoundException {
        // Overwrite target .properties file by resolving template placeholders
        Properties templateProperties = new Properties();
        templateProperties.load(res.in());

        Properties resolvedProperties = new Properties();
        for (Entry<Object, Object> propEntry : templateProperties.entrySet()) {
            String value = (String) propEntry.getValue();

            if (GeoServerEnvironment.allowEnvParametrization()) {
                value = (String) gsEnvironment.resolveValue(value);
            }

            resolvedProperties.setProperty((String) propEntry.getKey(), value);
        }

        final String relative =
                sourceBackupFolder.dir().toURI().relativize(res.file().toURI()).getPath();

        final String targetPropertyFileName =
                relative.substring(0, relative.length() - ".template".length());

        final File targetFile = new File(mosaicIndexBase.parent().dir(), targetPropertyFileName);

        resolvedProperties.store(new FileOutputStream(targetFile), null);
    }

    /** */
    private boolean copyFile(
            final Resource sourceBackupFolder,
            final Resource mosaicIndexBase,
            Resource res,
            boolean overwrite)
            throws IOException {
        final String relative =
                sourceBackupFolder.dir().toURI().relativize(res.file().toURI()).getPath();

        Resource targetFtl = Resources.fromPath(relative, mosaicIndexBase.parent());

        if (!Resources.exists(targetFtl) || overwrite) {
            if (!targetFtl.parent().dir().exists()) {
                targetFtl.parent().dir().mkdirs();
            }

            Resources.copy(res.file(), targetFtl.parent());

            return true;
        }

        return false;
    }
}
