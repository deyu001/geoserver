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

import java.io.IOException;
import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupRestoreItem;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

/**
 * Abstract Spring Batch {@link ItemReader}.
 *
 * <p>Configures the {@link Catalog} and initizializes the {@link XStreamPersister}.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
@SuppressWarnings("rawtypes")
public abstract class CatalogWriter<T> extends BackupRestoreItem
        implements ItemStreamWriter<T>, ResourceAwareItemWriterItemStream<T>, InitializingBean {

    protected Class clazz;

    public CatalogWriter(Class<T> clazz, Backup backupFacade) {
        super(backupFacade);
        this.clazz = clazz;

        this.setExecutionContextName(ClassUtils.getShortName(clazz));
    }

    private final ExecutionContextUserSupport executionContextUserSupport =
            new ExecutionContextUserSupport();

    /**
     * No-op.
     *
     * @see org.springframework.batch.item.ItemStream#close()
     */
    @Override
    public void close() {}

    /**
     * No-op.
     *
     * @see org.springframework.batch.item.ItemStream#open(ExecutionContext)
     */
    @Override
    public void open(ExecutionContext executionContext) {}

    /**
     * Return empty {@link ExecutionContext}.
     *
     * @see org.springframework.batch.item.ItemStream#update(ExecutionContext)
     */
    @Override
    public void update(ExecutionContext executionContext) {}

    /**
     * The name of the component which will be used as a stem for keys in the {@link
     * ExecutionContext}. Subclasses should provide a default value, e.g. the short form of the
     * class name.
     *
     * @param name the name for the component
     */
    public void setName(String name) {
        this.setExecutionContextName(name);
    }

    protected void setExecutionContextName(String name) {
        executionContextUserSupport.setName(name);
    }

    public String getExecutionContextKey(String key) {
        return executionContextUserSupport.getKey(key);
    }

    protected String getItemName(XStreamPersister xp) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

    @SuppressWarnings("unchecked")
    protected void firePostWrite(T item, Resource resource) throws IOException {
        List<CatalogAdditionalResourcesWriter> additionalResourceWriters =
                GeoServerExtensions.extensions(CatalogAdditionalResourcesWriter.class);

        for (CatalogAdditionalResourcesWriter wr : additionalResourceWriters) {
            if (wr.canHandle(item)) {
                wr.writeAdditionalResources(
                        backupFacade, Files.asResource(resource.getFile()), item);
            }
        }
    }
}
