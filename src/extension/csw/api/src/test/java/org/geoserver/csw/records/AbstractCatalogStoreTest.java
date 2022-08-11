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

package org.geoserver.csw.records;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.store.AbstractCatalogStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

public class AbstractCatalogStoreTest {

    @Test
    public void testNamespaceSupport() throws IOException, URISyntaxException {
        AbstractCatalogStore store =
                new AbstractCatalogStore() {
                    {
                        support(CSWRecordDescriptor.getInstance());
                        support(GSRecordDescriptor.getInstance());
                    }

                    @Override
                    public FeatureCollection<FeatureType, Feature> getRecordsInternal(
                            RecordDescriptor rd, RecordDescriptor rdOutput, Query q, Transaction t)
                            throws IOException {
                        if (rd == GSRecordDescriptor.getInstance()) {
                            return new MemoryFeatureCollection(
                                    GSRecordDescriptor.getInstance().getFeatureType());
                        } else {
                            throw new RuntimeException(
                                    "Was expecting the geoserver record descriptor");
                        }
                    }
                };

        RecordDescriptor[] descriptors = store.getRecordDescriptors();
        assertEquals(2, descriptors.length);
        assertEquals(CSWRecordDescriptor.getInstance(), descriptors[0]);
        assertEquals(GSRecordDescriptor.getInstance(), descriptors[1]);
        Query query = new Query("Record");
        query.setNamespace(new URI(GSRecordDescriptor.GS_NAMESPACE));
        FeatureCollection records = store.getRecords(query, Transaction.AUTO_COMMIT, null);
        assertEquals(GSRecordDescriptor.getInstance().getFeatureType(), records.getSchema());
    }

    static class GSRecordDescriptor extends AbstractRecordDescriptor {
        static final String GS_NAMESPACE = "http://www.geoserver.org/csw";
        CSWRecordDescriptor delegate = CSWRecordDescriptor.getInstance();
        static final GSRecordDescriptor INSTANCE = new GSRecordDescriptor();

        public static GSRecordDescriptor getInstance() {
            return INSTANCE;
        }

        public FeatureType getFeatureType() {
            FeatureType ft = delegate.getFeatureType();
            FeatureTypeFactory factory = new FeatureTypeFactoryImpl();
            FeatureType gsft =
                    factory.createFeatureType(
                            new NameImpl(GS_NAMESPACE, "Record"),
                            ft.getDescriptors(),
                            null,
                            false,
                            null,
                            ft.getSuper(),
                            null);
            return gsft;
        }

        public AttributeDescriptor getFeatureDescriptor() {
            AttributeTypeBuilder builder = new AttributeTypeBuilder();
            AttributeDescriptor descriptor =
                    builder.buildDescriptor(
                            new NameImpl(GS_NAMESPACE, "Record"), delegate.getFeatureType());
            return descriptor;
        }

        public String getOutputSchema() {
            return delegate.getOutputSchema();
        }

        public List<Name> getPropertiesForElementSet(ElementSetType elementSet) {
            return delegate.getPropertiesForElementSet(elementSet);
        }

        public NamespaceSupport getNamespaceSupport() {
            return delegate.getNamespaceSupport();
        }

        public Query adaptQuery(Query query) {
            return delegate.adaptQuery(query);
        }

        public String getBoundingBoxPropertyName() {
            return delegate.getBoundingBoxPropertyName();
        }

        public List<Name> getQueryables() {
            return delegate.getQueryables();
        }

        public String getQueryablesDescription() {
            return delegate.getQueryablesDescription();
        }

        public PropertyName translateProperty(Name name) {
            return delegate.translateProperty(name);
        }

        public void verifySpatialFilters(Filter filter) {
            delegate.verifySpatialFilters(filter);
        }
    }
}
