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

package org.geoserver.taskmanager.report.impl;

import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.report.Report;
import org.geoserver.taskmanager.report.Report.Type;
import org.geoserver.taskmanager.report.ReportBuilder;
import org.springframework.stereotype.Service;

/**
 * A very simple report builder. Contains all information that matters in simple text format.
 *
 * @author Niels Charlier
 */
@Service
public class SimpleReportBuilderImpl implements ReportBuilder {

    @Override
    public Report buildBatchRunReport(BatchRun batchRun) {
        StringBuilder reportContent = new StringBuilder();

        for (Run run : batchRun.getRuns()) {
            BatchElement element = run.getBatchElement();
            Task task = element.getTask();
            reportContent.append(
                    task.getFullName()
                            + ", started "
                            + run.getStart()
                            + ", ended "
                            + run.getEnd()
                            + ", status is "
                            + run.getStatus()
                            + "\n");
            if (run.getMessage() != null) {
                reportContent.append(
                        "\tmessage: " + run.getMessage() + " (check logs for more details) \n");
            }
        }

        StringBuilder reportTitle =
                new StringBuilder("Report: Batch " + batchRun.getBatch().getFullName() + " ");
        Type type;

        switch (batchRun.getStatus()) {
            case FAILED:
            case NOT_COMMITTED:
            case /* shouldn't happen */ READY_TO_COMMIT:
            case /* shouldn't happen */ RUNNING:
            case /* shouldn't happen */ COMMITTING:
            case /* shouldn't happen */ ROLLING_BACK:
                reportTitle.append("has failed");
                type = Type.FAILED;
                break;
            case ROLLED_BACK:
            case NOT_ROLLED_BACK:
                reportTitle.append("was cancelled");
                type = Type.CANCELLED;
                break;
            default:
                reportTitle.append("was successful");
                type = Type.SUCCESS;
        }

        return new Report(reportTitle.toString(), reportContent.toString(), type);
    }
}
