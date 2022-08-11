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

package org.geoserver.backuprestore;

import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.backuprestore.tasklet.GenericTaskletHandler;
import org.geoserver.backuprestore.tasklet.GenericTaskletUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/** Test generic handler that backup and restore an extra file that is not used by GeoServer. */
public final class ExtraFileHandler implements GenericTaskletHandler {

    public static final String EXTRA_FILE_NAME = "extra_file.properties";

    private final GeoServerDataDirectory dataDirectory;

    public ExtraFileHandler(GeoServerDataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void initialize(StepExecution stepExecution, BackupRestoreItem context) {
        // nothing to do here
    }

    @Override
    public RepeatStatus handle(
            StepContribution contribution,
            ChunkContext chunkContext,
            JobExecution jobExecution,
            BackupRestoreItem context) {
        Resource inputDirectory;
        Resource outputDirectory;
        if (GenericTaskletUtils.isBackup(context)) {
            // we are doing a backup
            inputDirectory = dataDirectory.getRoot();
            outputDirectory = GenericTaskletUtils.getOutputDirectory(jobExecution);
        } else {
            // we are doing a restore
            inputDirectory = GenericTaskletUtils.getInputDirectory(jobExecution);
            outputDirectory = dataDirectory.getRoot();
        }
        copyFile(inputDirectory, EXTRA_FILE_NAME, outputDirectory, EXTRA_FILE_NAME);
        return RepeatStatus.FINISHED;
    }

    /** Helper method for copying a file from a directory to another. */
    private void copyFile(
            Resource inputDirectory,
            String inputFileName,
            Resource outputDirectory,
            String outputFileName) {
        Resource inputFile = inputDirectory.get(inputFileName);
        if (!Resources.exists(inputFile)) {
            // nothing to copy
            return;
        }
        if (Resources.exists(outputDirectory) && outputDirectory.getType() == Type.DIRECTORY) {
            Resource outputFile = outputDirectory.get(outputFileName);
            try (InputStream input = inputFile.in();
                    // copy the file to is destination
                    OutputStream output = outputFile.out()) {
                IOUtils.copy(input, output);
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format(
                                "Error copying file '%s' to file '%s'.", inputFile, outputFile),
                        exception);
            }
        }
    }
}
