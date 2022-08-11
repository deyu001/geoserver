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

package org.geoserver.geoserver.authentication.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.rest.xml.JaxbRule.Limits;
import org.geoserver.geofence.rest.xml.JaxbRuleList;
import org.geoserver.geoserver.authentication.GeoFenceXStreamPersisterInitializer;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeofencePersisterSerializationTest {

    private XStreamPersister persister;

    @Before
    public void setup() {
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        GeoFenceXStreamPersisterInitializer initializer = new GeoFenceXStreamPersisterInitializer();
        xpf.addInitializer(initializer);

        persister = xpf.createXMLPersister();
    }

    @Test
    public void testDeserialization() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<Rule>"
                        + "<access>LIMIT</access>"
                        + "<layer>DE_USNG_UTM18</layer>"
                        + "<limits>"
                        + "     <allowedArea>SRID=4326;MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))</allowedArea>"
                        + "     <catalogMode>HIDDEN</catalogMode>"
                        + "</limits>"
                        + "<priority>1</priority>"
                        + "<workspace>geonode</workspace>"
                        + "</Rule>";

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));

        JaxbRule rule = persister.load(bais, JaxbRule.class);

        assertNotNull(rule);

        assertEquals("LIMIT", rule.getAccess());
        assertEquals("DE_USNG_UTM18", rule.getLayer());
        assertEquals("geonode", rule.getWorkspace());
        assertEquals(1, rule.getPriority().intValue());

        assertNotNull(rule.getLimits());

        assertEquals(
                "SRID=4326;MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))",
                rule.getLimits().getAllowedArea());

        assertEquals("HIDDEN", rule.getLimits().getCatalogMode());
    }

    @Test
    public void testSerialization() throws Exception {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(1L);
        rule.setUserName("pippo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");
        Limits limits = new Limits();
        limits.setCatalogMode("HIDDEN");
        WKTReader reader = new WKTReader();
        limits.setAllowedArea(
                (MultiPolygon)
                        reader.read("MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))"));
        rule.setLimits(limits);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        persister.save(rule, baos);
        baos.flush();

        String xml = new String(baos.toByteArray(), "UTF-8");

        // System.err.println(xml);
        assertTrue(xml.contains("pippo"));
        assertTrue(xml.contains("clown"));
        assertTrue(xml.contains("HIDDEN"));
        assertTrue(xml.contains("MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))"));

        Rule rule2 = new Rule();
        rule2.setPriority(2L);
        rule2.setUsername("topolino");
        rule2.setRolename("minnie");
        rule2.setService("wfs");
        rule2.setRequest("getFeature");
        rule2.setWorkspace("workspace");
        rule2.setLayer("layer");
        rule2.setAccess(GrantType.ALLOW);

        Rule[] rules = new Rule[] {rule2};
        JaxbRuleList ruleList = new JaxbRuleList(Arrays.asList(rules));

        persister.save(ruleList, baos);
        baos.flush();

        xml = new String(baos.toByteArray(), "UTF-8");

        // System.err.println(xml);
        assertTrue(xml.contains("topolino"));
        assertTrue(xml.contains("minnie"));
    }
}
