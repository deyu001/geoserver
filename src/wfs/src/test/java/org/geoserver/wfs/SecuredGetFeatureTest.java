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

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SecuredGetFeatureTest extends WFSTestSupport {

    public static QName NULL_GEOMETRIES =
            new QName(SystemTestData.CITE_URI, "NullGeometries", SystemTestData.CITE_PREFIX);

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {

        addUser("cite", "cite", null, Collections.singletonList("ROLE_CITE_READER"));

        addLayerAccessRule("*", "*", AccessMode.READ, "ROLE_NO_ONE");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "ROLE_NO_ONE");
        addLayerAccessRule(SystemTestData.CITE_PREFIX, "*", AccessMode.READ, "ROLE_CITE_READER");
    }

    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList((Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testGetNoAuthHide() throws Exception {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.HIDE);

        // no auth, hide mode, we should get an error stating the layer is not there
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        checkOws10Exception(doc);
        assertXpathEvaluatesTo("Unknown namespace [cite]", "//ows:ExceptionText/text()", doc);
    }

    @Test
    public void testGetNoAuthChallenge() throws Exception {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        // this test seems to fail on the build server without storing the rules...
        dao.storeRules();

        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        assertEquals(401, resp.getStatus());
        assertEquals("Basic realm=\"GeoServer Realm\"", resp.getHeader("WWW-Authenticate"));
    }

    @Test
    public void testInvalidAuthChallenge() throws Exception {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        MockHttpServletRequest request =
                createRequest(
                        "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        request.setMethod("GET");
        request.addHeader(
                "Authorization",
                "Basic " + new String(Base64.encodeBase64("cite:wrongpassword".getBytes())));

        MockHttpServletResponse resp = dispatch(request);
        assertEquals(401, resp.getStatus());
        assertEquals("Basic realm=\"GeoServer Realm\"", resp.getHeader("WWW-Authenticate"));
    }

    @Test
    public void testValidAuth() throws Exception {
        checkValidAuth("cite", "cite");
    }

    @Test
    public void testValidAuthAdmin() throws Exception {
        checkValidAuth("admin", "geoserver");
    }

    private void checkValidAuth(String username, String password)
            throws Exception, ParserConfigurationException, SAXException, IOException,
                    XpathException {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        setRequestAuth(username, password);
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", doc);
    }
}
