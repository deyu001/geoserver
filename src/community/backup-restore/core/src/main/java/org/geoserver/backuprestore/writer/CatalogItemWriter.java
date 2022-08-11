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

package org.geoserver.backuprestore.writer;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.Resource;

/**
 * Concrete Spring Batch {@link ItemWriter}.
 *
 * <p>Writes unmarshalled items into the temporary {@link Catalog} in memory.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogItemWriter<T> extends CatalogWriter<T> {

    private static final Logger LOGGER = Logging.getLogger(CatalogItemWriter.class);

    public CatalogItemWriter(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        if (this.getXp() == null) {
            setXp(this.xstream.getXStream());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(List<? extends T> items) {
        for (T item : items) {
            try {
                if (item instanceof WorkspaceInfo) {
                    write((WorkspaceInfo) item);
                } else if (item instanceof NamespaceInfo) {
                    write((NamespaceInfo) item);
                } else if (item instanceof DataStoreInfo) {
                    write((DataStoreInfo) item);
                } else if (item instanceof CoverageStoreInfo) {
                    write((CoverageStoreInfo) item);
                } else if (item instanceof ResourceInfo) {
                    write((ResourceInfo) item);
                } else if (item instanceof LayerInfo) {
                    write((LayerInfo) item);
                } else if (item instanceof StyleInfo) {
                    write((StyleInfo) item);
                } else if (item instanceof LayerGroupInfo) {
                    write((LayerGroupInfo) item);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception writting catalog item : " + item, e);
                logValidationExceptions((T) null, e);
            }
        }
    }

    private void write(LayerGroupInfo layerGroupInfo) {
        try {
            getCatalog().add(layerGroupInfo);
            getCatalog().save(getCatalog().getLayerGroup(layerGroupInfo.getId()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception writting layer group : " + layerGroupInfo, e);
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
        }
    }

    private void write(StyleInfo styleInfo) {
        StyleInfo source = getCatalog().getStyleByName((styleInfo).getName());
        if (source == null) {
            getCatalog().add(styleInfo);
            getCatalog().save(getCatalog().getStyle((styleInfo).getId()));
        }
    }

    private void write(LayerInfo layerInfo) {
        if (layerInfo.getName() != null) {
            LayerInfo source = getCatalog().getLayerByName(layerInfo.getName());
            if (source == null) {
                getCatalog().add(layerInfo);
                getCatalog().save(getCatalog().getLayer(layerInfo.getId()));
            }
        }
    }

    private void write(ResourceInfo resourceInfo) {
        if (getCatalog().getResourceByName(resourceInfo.getName(), FeatureTypeInfo.class) == null
                && getCatalog().getResourceByName(resourceInfo.getName(), CoverageInfo.class)
                        == null) {
            Class<? extends ResourceInfo> clz = null;
            if (resourceInfo instanceof FeatureTypeInfo) {
                clz = FeatureTypeInfo.class;
            } else if (resourceInfo instanceof CoverageInfo) {
                clz = CoverageInfo.class;
            }
            getCatalog().add(resourceInfo);
            getCatalog().save(getCatalog().getResource(resourceInfo.getId(), clz));
        }
    }

    private void write(CoverageStoreInfo csInfo) {
        CoverageStoreInfo source = getCatalog().getCoverageStoreByName((csInfo).getName());
        if (source == null) {
            getCatalog().add(csInfo);
            getCatalog().save(getCatalog().getCoverageStore((csInfo).getId()));
        }
    }

    private void write(DataStoreInfo dsInfo) {
        DataStoreInfo source = getCatalog().getDataStoreByName(dsInfo.getName());
        if (source == null) {
            getCatalog().add(dsInfo);
            getCatalog().save(getCatalog().getDataStore(dsInfo.getId()));
        }
    }

    private void write(NamespaceInfo nsInfo) {
        NamespaceInfo source = getCatalog().getNamespaceByPrefix((nsInfo).getPrefix());
        if (source == null) {
            getCatalog().add(nsInfo);
            getCatalog().save(getCatalog().getNamespace((nsInfo).getId()));
        }
    }

    private void write(WorkspaceInfo wsInfo) {
        WorkspaceInfo source = getCatalog().getWorkspaceByName(wsInfo.getName());
        if (source == null) {
            getCatalog().add(wsInfo);
            getCatalog().save(getCatalog().getWorkspace(wsInfo.getId()));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Nothing to do.
    }

    /** Setter for resource. Represents a file that can be written. */
    @Override
    public void setResource(Resource resource) {
        // Nothing to do
    }
}
