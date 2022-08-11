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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Run.Status;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** @author Niels Charlier */
@Service
public class InitConfigUtil {

    public static final String INIT_BATCH = "@Initialize";

    @Autowired private TaskManagerDao dao;

    public boolean isInitConfig(Configuration config) {
        if (config.isTemplate()) {
            return false;
        }
        Batch batch = getInitBatch(config);
        if (batch != null) {
            if (batch.getId() != null) {
                batch = dao.initHistory(batch);
                for (BatchRun batchRun : batch.getBatchRuns()) {
                    if (batchRun.getStatus() == Status.COMMITTED) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public Configuration wrap(Configuration config) {
        if (!(config instanceof ConfigurationWrapper)) {
            Batch batch = getInitBatch(config);
            if (batch != null) {
                return new ConfigurationWrapper(config, batch);
            }
        }
        return config;
    }

    public static Configuration unwrap(Configuration config) {
        if (config instanceof ConfigurationWrapper) {
            return ((ConfigurationWrapper) config).getDelegate();
        } else {
            return config;
        }
    }

    public static Batch getInitBatch(Configuration config) {
        return config.getBatches().get(INIT_BATCH);
    }

    public static boolean isInitBatch(Batch batch) {
        return batch.getConfiguration() != null && batch.getName().equals(INIT_BATCH);
    }

    private static class ConfigurationWrapper implements Configuration {

        private static final long serialVersionUID = 8073599284694547987L;

        private Configuration delegate;

        private Map<String, Task> tasks = new HashMap<String, Task>();;

        private Map<String, Batch> batches;

        public ConfigurationWrapper(Configuration delegate, Batch initBatch) {
            this.delegate = delegate;

            if (initBatch != null) {
                for (BatchElement element : initBatch.getElements()) {
                    tasks.put(element.getTask().getName(), element.getTask());
                }

                batches = Collections.singletonMap(initBatch.getName(), initBatch);
            } else {
                batches = Collections.emptyMap();
            }
        }

        public Configuration getDelegate() {
            return delegate;
        }

        @Override
        public void setRemoveStamp(long removeStamp) {
            delegate.setRemoveStamp(removeStamp);
        }

        @Override
        public long getRemoveStamp() {
            return delegate.getRemoveStamp();
        }

        @Override
        public Long getId() {
            return delegate.getId();
        }

        @Override
        public boolean isTemplate() {
            return delegate.isTemplate();
        }

        @Override
        public void setTemplate(boolean template) {
            delegate.setTemplate(template);
        }

        @Override
        public String getWorkspace() {
            return delegate.getWorkspace();
        }

        @Override
        public void setWorkspace(String workspace) {
            delegate.setWorkspace(workspace);
        }

        @Override
        public Map<String, Attribute> getAttributes() {
            return delegate.getAttributes();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public void setName(String name) {
            delegate.setName(name);
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public void setDescription(String name) {
            delegate.setDescription(name);
        }

        @Override
        public boolean isValidated() {
            return delegate.isValidated();
        }

        @Override
        public void setValidated(boolean initMode) {
            delegate.setValidated(initMode);
        }

        @Override
        public Map<String, Task> getTasks() {
            return tasks;
        }

        @Override
        public Map<String, Batch> getBatches() {
            return batches;
        }
    }
}
