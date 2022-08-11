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

package org.geoserver.wfs.v1_1;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import net.opengis.ows10.Ows10Factory;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.wfs.CapabilitiesTransformer;
import org.geoserver.wfs.GetCapabilities;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.xml.transform.TransformerBase;
import org.junit.Before;
import org.junit.Test;

public class VersionNegotiationTest extends WFSTestSupport {

    static GetCapabilities getCaps;

    static WfsFactory factory;

    static Ows10Factory owsFactory;

    @Before
    public void initialise() {
        getCaps = new GetCapabilities(getWFS(), getCatalog(), Collections.emptyList());

        factory = WfsFactory.eINSTANCE;
        owsFactory = Ows10Factory.eINSTANCE;
    }

    @Test
    public void test0() throws Exception {
        // test when provided and accepted match up
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.0");
        request.getAcceptVersions().getVersion().add("1.1.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    @Test
    public void test1() throws Exception {
        // test accepted only 1.0
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }

    @Test
    public void test2() throws Exception {
        // test accepted only 1.1
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.1.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    @Test
    public void test5() throws Exception {
        // test accepted = 0.0.0

        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("0.0.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }

    @Test
    public void test6() throws Exception {
        // test accepted = 1.1.1

        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.1.1");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    @Test
    public void test7() throws Exception {
        // test accepted = 1.0.5
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.5");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }
}
