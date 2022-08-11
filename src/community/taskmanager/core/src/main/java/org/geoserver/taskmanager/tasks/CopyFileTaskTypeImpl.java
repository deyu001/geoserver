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

package org.geoserver.taskmanager.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyFileTaskTypeImpl implements TaskType {

    public static final String NAME = "CopyFile";

    public static final String PARAM_SOURCE_SERVICE = "sourceService";

    public static final String PARAM_TARGET_SERVICE = "targetService";

    public static final String PARAM_SOURCE_PATH = "sourcePath";

    public static final String PARAM_TARGET_PATH = "targetPath";

    public static final String PARAM_AUTO_VERSIONED = "auto-versioned";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @Autowired protected ExtTypes extTypes;

    @Override
    public String getName() {
        return NAME;
    }

    @PostConstruct
    public void initParamInfo() {
        ParameterInfo sourceService =
                new ParameterInfo(PARAM_SOURCE_SERVICE, extTypes.fileService, true);
        paramInfo.put(PARAM_SOURCE_SERVICE, sourceService);
        paramInfo.put(
                PARAM_SOURCE_PATH,
                new ParameterInfo(PARAM_SOURCE_PATH, extTypes.file(false, true), true)
                        .dependsOn(sourceService));
        ParameterInfo targetService =
                new ParameterInfo(PARAM_TARGET_SERVICE, extTypes.fileService, true);
        paramInfo.put(PARAM_TARGET_SERVICE, targetService);
        ParameterInfo autoVersioned =
                new ParameterInfo(PARAM_AUTO_VERSIONED, ParameterType.BOOLEAN, false);
        paramInfo.put(
                PARAM_TARGET_PATH,
                new ParameterInfo(PARAM_TARGET_PATH, extTypes.file(false, false), true)
                        .dependsOn(targetService)
                        .dependsOn(autoVersioned));
        paramInfo.put(
                PARAM_AUTO_VERSIONED,
                new ParameterInfo(PARAM_AUTO_VERSIONED, ParameterType.BOOLEAN, false));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final FileReference source =
                (FileReference)
                        ctx.getBatchContext().get(ctx.getParameterValues().get(PARAM_SOURCE_PATH));
        final FileReference target =
                (FileReference) ctx.getParameterValues().get(PARAM_TARGET_PATH);

        try {
            if (target.getLatestVersion().equals(target.getNextVersion())) {
                target.getService().delete(target.getNextVersion());
            }
            try (InputStream is = source.getService().read(source.getLatestVersion())) {
                target.getService().create(target.getNextVersion(), is);
            }
        } catch (IOException e) {
            throw new TaskException(e);
        }

        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                try {
                    if (!target.getLatestVersion().equals(target.getNextVersion())) {
                        target.getService().delete(target.getLatestVersion());
                    }
                } catch (IOException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try {
                    if (!target.getLatestVersion().equals(target.getNextVersion())) {
                        target.getService().delete(target.getNextVersion());
                    }
                } catch (IOException e) {
                    throw new TaskException(e);
                }
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final FileReference target =
                (FileReference) ctx.getParameterValues().get(PARAM_TARGET_PATH);

        try {
            target.getService().delete(target.getLatestVersion());
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }
}
