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


/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geoserver.backuprestore.reader;

import java.util.Arrays;
import java.util.Comparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.backuprestore.Backup;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResourceAware;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Reads items from multiple resources sequentially - resource list is given by {@link
 * #setResources(Resource[])}, the actual reading is delegated to {@link
 * #setDelegate(ResourceAwareItemReaderItemStream)}.
 *
 * <p>Input resources are ordered using {@link #setComparator(Comparator)} to make sure resource
 * ordering is preserved between job runs in restart scenario.
 *
 * <p>Code based on original {@link MultiResourceItemReader} by Robert Kasanicky and Lucas Ward.
 *
 * @author Robert Kasanicky
 * @author Lucas Ward
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogMultiResourceItemReader<T> extends CatalogReader<T> {

    private static final Log logger = LogFactory.getLog(CatalogMultiResourceItemReader.class);

    private static final String RESOURCE_KEY = "resourceIndex";

    private CatalogReader<? extends T> delegate;

    private Resource[] resources;

    private boolean saveState = true;

    private int currentResource = -1;

    // signals there are no resources to read -> just return null on first read
    private boolean noInput;

    private boolean strict = false;

    public CatalogMultiResourceItemReader(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    protected void initialize(StepExecution stepExecution) {
        delegate.retrieveInterstepData(stepExecution);
    }

    /**
     * In strict mode the reader will throw an exception on {@link
     * #open(org.springframework.batch.item.ExecutionContext)} if there are no resources to read.
     *
     * @param strict false by default
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    private Comparator<Resource> comparator =
            new Comparator<Resource>() {

                /** Compares resource filenames. */
                @Override
                public int compare(Resource r1, Resource r2) {
                    return r1.getFilename().compareTo(r2.getFilename());
                }
            };

    /** Reads the next item, jumping to next resource if necessary. */
    @Override
    public T read() throws Exception, UnexpectedInputException, ParseException {

        if (noInput) {
            return null;
        }

        // If there is no resource, then this is the first item, set the current
        // resource to 0 and open the first delegate.
        if (currentResource == -1) {
            currentResource = 0;
            delegate.setResource(resources[currentResource]);
            delegate.open(new ExecutionContext());
        }

        return readNextItem();
    }

    /**
     * Use the delegate to read the next item, jump to next resource if current one is exhausted.
     * Items are appended to the buffer.
     *
     * @return next item from input
     */
    private T readNextItem() throws Exception {

        T item = readFromDelegate();

        while (item == null) {

            currentResource++;

            if (currentResource >= resources.length) {
                return null;
            }

            delegate.close();
            delegate.setResource(resources[currentResource]);
            delegate.open(new ExecutionContext());

            item = readFromDelegate();
        }

        return item;
    }

    private T readFromDelegate() throws Exception {
        T item = delegate.read();
        if (item instanceof ResourceAware) {
            ((ResourceAware) item).setResource(getCurrentResource());
        }
        return item;
    }

    /**
     * Close the {@link #setDelegate(ResourceAwareItemReaderItemStream)} reader and reset instance
     * variable values.
     */
    @Override
    public void close() throws ItemStreamException {
        super.close();

        if (!this.noInput) {
            delegate.close();
        }

        noInput = false;
    }

    /**
     * Figure out which resource to start with in case of restart, open the delegate and restore
     * delegate's position in the resource.
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);
        Assert.notNull(resources, "Resources must be set");

        noInput = false;
        if (resources.length == 0) {
            if (strict) {
                throw new IllegalStateException(
                        "No resources to read. Set strict=false if this is not an error condition.");
            } else {
                logger.warn(
                        "No resources to read. Set strict=true if this should be an error condition.");
                noInput = true;
                return;
            }
        }

        Arrays.sort(resources, comparator);

        if (executionContext.containsKey(getExecutionContextKey(RESOURCE_KEY))) {
            currentResource = executionContext.getInt(getExecutionContextKey(RESOURCE_KEY));

            // context could have been saved before reading anything
            if (currentResource == -1) {
                currentResource = 0;
            }

            delegate.setResource(resources[currentResource]);
            delegate.open(executionContext);
        } else {
            currentResource = -1;
        }
    }

    /** Store the current resource index and position in the resource. */
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
        if (saveState) {
            executionContext.putInt(getExecutionContextKey(RESOURCE_KEY), currentResource);
            delegate.update(executionContext);
        }
    }

    /** @param delegate reads items from single {@link Resource}. */
    public void setDelegate(CatalogReader<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Set the boolean indicating whether or not state should be saved in the provided {@link
     * ExecutionContext} during the {@link ItemStream} call to update.
     */
    public void setSaveState(boolean saveState) {
        this.saveState = saveState;
    }

    /**
     * @param comparator used to order the injected resources, by default compares {@link
     *     Resource#getFilename()} values.
     */
    public void setComparator(Comparator<Resource> comparator) {
        this.comparator = comparator;
    }

    /** @param resources input resources */
    public void setResources(Resource[] resources) {
        Assert.notNull(resources, "The resources must not be null");
        this.resources = Arrays.asList(resources).toArray(new Resource[resources.length]);
    }

    public Resource getCurrentResource() {
        if (currentResource >= resources.length || currentResource < 0) {
            return null;
        }
        return resources[currentResource];
    }

    @Override
    public void setResource(Resource resource) {}

    @Override
    public void afterPropertiesSet() throws Exception {}

    @Override
    protected T doRead() throws Exception {
        return null;
    }

    @Override
    protected void doOpen() throws Exception {}

    @Override
    protected void doClose() throws Exception {}
}
