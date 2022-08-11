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

package org.geoserver.wfs;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Collections;
import net.opengis.wfs20.AbstractTransactionActionType;
import net.opengis.wfs20.ReplaceType;
import net.opengis.wfs20.TransactionType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Tests for {@link WFSWorkspaceQualifier}.
 *
 * @author awaterme
 */
public class WFSWorkspaceQualifierTest {

    private static final String WORKSPACE_URI = "http://workspace/namespace";
    private Catalog mockCatalog = createMock(Catalog.class);
    private WorkspaceInfo mockWorkspaceInfo = createMock(WorkspaceInfo.class);
    private NamespaceInfo mockNamespaceInfo = createMock(NamespaceInfo.class);
    private FeatureType mockFeatureType = createMock(FeatureType.class);
    private Feature mockFeature = createMock(Feature.class);
    private Name mockName = createMock(Name.class);

    private WFSWorkspaceQualifier sut = new WFSWorkspaceQualifier(mockCatalog);

    @Before
    public void setup() {
        String workspaceName = "workspacename";

        expect(mockCatalog.getNamespaceByPrefix(workspaceName)).andReturn(mockNamespaceInfo);
        expect(mockWorkspaceInfo.getName()).andReturn(workspaceName);
        expect(mockNamespaceInfo.getURI()).andReturn(WORKSPACE_URI);
        expect(mockFeature.getType()).andReturn(mockFeatureType);
        expect(mockFeatureType.getName()).andReturn(mockName);
    }

    /**
     * Test for {@link WFSWorkspaceQualifier#qualifyRequest(WorkspaceInfo,
     * org.geoserver.catalog.LayerInfo, Operation, org.geoserver.ows.Request)} .Simulates a WFS-T
     * Replace, having one Feature. The namespaceURI of the workspace and the feature match. Result:
     * No exception.
     */
    @Test
    public void testQualifyRequestWithReplaceNamespaceValidationHavingMatchingNamespaces() {
        expect(mockName.getNamespaceURI()).andReturn(WORKSPACE_URI).anyTimes();
        invokeQualifyRequest();
    }

    /**
     * Test for {@link WFSWorkspaceQualifier#qualifyRequest(WorkspaceInfo,
     * org.geoserver.catalog.LayerInfo, Operation, org.geoserver.ows.Request)} . Simulates a WFS-T
     * Replace, having one Feature. The namespaceURI of the workspace and the feature do not match.
     * Result: Exception.
     */
    @Test(expected = WFSException.class)
    public void testQualifyRequestWithReplaceNamespaceValidationHavingNonMatchingNamespaces() {
        expect(mockName.getNamespaceURI()).andReturn("http://foo").anyTimes();
        invokeQualifyRequest();
    }

    private void invokeQualifyRequest() {
        TransactionType transactionType = Wfs20Factory.eINSTANCE.createTransactionType();
        ReplaceType replaceType = Wfs20Factory.eINSTANCE.createReplaceType();
        EList<AbstractTransactionActionType> action =
                transactionType.getAbstractTransactionAction();
        action.add(replaceType);
        replaceType.getAny().add(mockFeature);

        Version version = new Version("2.0.0");
        Service service =
                new Service("id", "service", version, Collections.singletonList("Transaction"));
        Operation operation = new Operation("id", service, null, new Object[] {transactionType});

        replay(
                mockCatalog,
                mockFeature,
                mockFeatureType,
                mockName,
                mockNamespaceInfo,
                mockWorkspaceInfo);
        sut.qualifyRequest(mockWorkspaceInfo, null, operation, null);
    }
}
