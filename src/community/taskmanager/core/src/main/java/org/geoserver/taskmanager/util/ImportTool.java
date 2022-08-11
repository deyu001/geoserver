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

package org.geoserver.taskmanager.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/taskmanager-import")
public class ImportTool {

    private static final Logger LOGGER = Logging.getLogger(ImportTool.class);

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    private static final String SPLIT_BY = ";";

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{template}", method = RequestMethod.POST)
    public boolean doImportWithTemplate(
            @PathVariable String template,
            @RequestBody String csvFile,
            @RequestParam(defaultValue = "true") boolean validate)
            throws IOException {

        boolean success = true;

        if (!SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .contains(GeoServerRole.ADMIN_ROLE)) {
            throw new AccessDeniedException("You must be administrator.");
        }

        try (Scanner scanner = new Scanner(csvFile)) {
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] attNames = line.split(SPLIT_BY);

                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    if (line.isEmpty()) {
                        continue;
                    }
                    String[] split = line.split(SPLIT_BY);
                    Map<String, String> record = new HashMap<String, String>();
                    for (int i = 0; i < Math.min(attNames.length, split.length); i++) {
                        record.put(attNames[i], split[i]);
                    }

                    String configName = record.remove("name");
                    Configuration config = dao.getConfiguration(configName);
                    if (config == null) {
                        config = dao.copyConfiguration(template);
                        config.setName(configName);
                    } else {
                        config = dao.init(config);
                    }
                    config.setTemplate(false);
                    if (record.containsKey("description")) {
                        config.setDescription(record.remove("description"));
                    }
                    if (record.containsKey("workspace")) {
                        config.setWorkspace(record.remove("workspace"));
                    }

                    for (Map.Entry<String, String> entry : record.entrySet()) {
                        dataUtil.setConfigurationAttribute(
                                config, entry.getKey(), entry.getValue());
                    }

                    if (validate) {
                        List<ValidationError> errors = taskUtil.validate(config);
                        if (!errors.isEmpty()) {
                            for (ValidationError error : errors) {
                                LOGGER.severe(
                                        "Failed to import configuration "
                                                + config.getName()
                                                + ", validation error: "
                                                + error.toString());
                                success = false;
                            }
                        } else {
                            config.setValidated(true);
                            try {
                                bjService.saveAndSchedule(config);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.SEVERE,
                                        "Failed to import configuration " + config.getName(),
                                        e);
                                success = false;
                            }
                        }
                    } else {
                        try {
                            bjService.saveAndSchedule(config);
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failed to import configuration " + config.getName(),
                                    e);
                            success = false;
                        }
                    }
                }
            }
        }

        return success;
    }
}
