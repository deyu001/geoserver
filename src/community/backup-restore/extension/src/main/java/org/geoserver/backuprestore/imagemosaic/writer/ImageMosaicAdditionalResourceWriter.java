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

package org.geoserver.backuprestore.imagemosaic.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.imagemosaic.ImageMosaicAdditionalResource;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.backuprestore.writer.CatalogAdditionalResourcesWriter;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;

/** @author Alessio Fabiani, GeoSolutions */
public class ImageMosaicAdditionalResourceWriter extends ImageMosaicAdditionalResource
        implements CatalogAdditionalResourcesWriter<StoreInfo> {

    @Override
    public boolean canHandle(Object item) {
        if (item instanceof CoverageStoreInfo
                && ((CoverageStoreInfo) item).getType().equals(COVERAGE_TYPE)) {
            return true;
        }
        return false;
    }

    @Override
    public void writeAdditionalResources(Backup backupFacade, Resource base, StoreInfo item)
            throws IOException {

        final Resource targetBackupFolder =
                BackupUtils.dir(base.parent(), IMAGEMOSAIC_INDEXES_FOLDER);

        // Create folder if not exists
        Resources.directory(targetBackupFolder, !Resources.exists(targetBackupFolder));

        final CoverageStoreInfo mosaicCoverageStore =
                backupFacade.getCatalog().getResourcePool().clone((CoverageStoreInfo) item, true);
        final String mosaicName = mosaicCoverageStore.getName();
        final String mosaicUrlBase = mosaicCoverageStore.getURL();

        final Resource mosaicIndexBase = Resources.fromURL(mosaicUrlBase);

        final Resource mosaicBaseFolder =
                Files.asResource(
                        (Resources.directory(mosaicIndexBase) != null
                                ? Resources.directory(mosaicIndexBase)
                                : Resources.directory(mosaicIndexBase.parent())));

        // Create the target mosaic folder
        Resource targetMosaicBaseFolder =
                BackupUtils.dir(targetBackupFolder, mosaicBaseFolder.name());

        if (Resources.exists(mosaicIndexBase)) {
            for (Entry<String, Filter<Resource>> entry : resources.entrySet()) {
                List<Resource> mosaicIndexerResources =
                        Resources.list(mosaicIndexBase, entry.getValue(), true);

                for (Resource res : mosaicIndexerResources) {
                    if (!FilenameUtils.getBaseName(res.name()).equals(mosaicName)
                            && !FilenameUtils.getBaseName(res.name())
                                    .equals(mosaicBaseFolder.name())
                            && Resources.exists(res)
                            && Resources.canRead(res)) {
                        final String relative =
                                mosaicIndexBase
                                        .parent()
                                        .dir()
                                        .toURI()
                                        .relativize(res.file().toURI())
                                        .getPath();

                        Resource targetFtl = Resources.fromPath(relative, targetBackupFolder);

                        if (!targetFtl.parent().dir().exists()) {
                            targetFtl.parent().dir().mkdirs();
                        }

                        Resources.copy(res.file(), targetFtl.parent());
                    }
                }
            }
        }

        // Populate "Name=<mosaicName>" property into the indexer
        final File indexerFile = new File(targetMosaicBaseFolder.dir(), "indexer.properties");

        Properties indexerProperties = new Properties();

        if (indexerFile.exists() && indexerFile.canRead()) {
            indexerProperties.load(new FileInputStream(indexerFile));
        }

        indexerProperties.setProperty("Name", mosaicName);

        indexerProperties.store(new FileOutputStream(indexerFile), null);
    }
}
