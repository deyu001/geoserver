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

package org.geoserver.csw;

import java.io.File;
import java.util.List;
import net.opengis.cat.csw20.CapabilitiesType;
import net.opengis.cat.csw20.DescribeRecordType;
import net.opengis.cat.csw20.GetCapabilitiesType;
import net.opengis.cat.csw20.GetDomainType;
import net.opengis.cat.csw20.GetRecordByIdType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.HarvestResponseType;
import net.opengis.cat.csw20.HarvestType;
import net.opengis.cat.csw20.TransactionType;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.response.CSWRecordsResult;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.csw.store.RepositoryItem;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.opengis.feature.type.AttributeDescriptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The default CSW implementation
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultWebCatalogService implements WebCatalogService, ApplicationContextAware {

    private GeoServer gs;

    private CatalogStore store;

    protected ApplicationContext context;

    public DefaultWebCatalogService(GeoServer gs) {
        this.gs = gs;
    }

    public CSWInfo getServiceInfo() {
        return gs.getService(CSWInfo.class);
    }

    @Override
    public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws ServiceException {
        checkStore();
        CapabilitiesType caps =
                new GetCapabilities(getServiceInfo(), this.store, context).run(request);

        // check for decorator extensions
        for (CapabilitiesDecorator decorator :
                GeoServerExtensions.extensions(CapabilitiesDecorator.class)) {
            caps = decorator.decorate(caps, this.store);
        }

        return caps;
    }

    @Override
    public AttributeDescriptor[] describeRecord(DescribeRecordType request)
            throws ServiceException {
        checkStore();

        return new DescribeRecord(getServiceInfo(), store).run(request);
    }

    @Override
    public CSWRecordsResult getRecords(GetRecordsType request) throws ServiceException {
        checkStore();
        List<RecordDescriptor> descriptors =
                GeoServerExtensions.extensions(RecordDescriptor.class, context);
        return new GetRecords(getServiceInfo(), store, descriptors).run(request);
    }

    @Override
    public CSWRecordsResult getRecordById(GetRecordByIdType request) throws ServiceException {
        checkStore();
        List<RecordDescriptor> descriptors =
                GeoServerExtensions.extensions(RecordDescriptor.class, context);
        return new GetRecordById(getServiceInfo(), store, descriptors).run(request);
    }

    @Override
    public CloseableIterator<String> getDomain(GetDomainType request) throws ServiceException {
        checkStore();
        return new GetDomain(getServiceInfo(), this.store).run(request);
    }

    @Override
    public RepositoryItem getRepositoryItem(GetRepositoryItemType request) throws ServiceException {
        checkStore();
        return new GetRepositoryItem(getServiceInfo(), this.store).run(request);
    }

    @Override
    public HarvestResponseType harvest(HarvestType request) throws ServiceException {
        checkStore();
        throw new ServiceException("Harvest operation is not supported by this CSW service");
    }

    @Override
    public HarvestResponseType transaction(TransactionType request) throws ServiceException {
        checkStore();
        throw new ServiceException("Transactions are not supported by this CSW service");
    }

    @Override
    public List<File> directDownload(DirectDownloadType request) throws ServiceException {
        checkStore();
        return new DirectDownload(getServiceInfo(), this.store).run(request);
    }

    /** Checks we have a store to use */
    private void checkStore() {
        if (store == null) {
            throw new ServiceException(
                    "Catalog service could not find a CatalogStore implementation registered in the Spring application context",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        // pick the implementation of CatalogStore that has the higher priority
        List<CatalogStore> storeCandidates =
                GeoServerExtensions.extensions(CatalogStore.class, applicationContext);

        if (storeCandidates != null && !storeCandidates.isEmpty()) {
            String defaultStore = System.getProperty("DefaultCatalogStore");
            if (defaultStore != null) {
                for (CatalogStore store : storeCandidates) {
                    if (store.getClass().getName().equals(defaultStore)) {
                        this.store = store;
                        break;
                    }
                }
            }

            if (store == null) {
                store = storeCandidates.get(0);
            }
        }
    }
}
