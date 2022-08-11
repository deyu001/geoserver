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

package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.styling.Style;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;

public class GetLegendGraphicKvpReaderTest extends WMSTestSupport {
    /**
     * request reader to test against, initialized by default with all parameters from <code>
     * requiredParameters</code> and <code>optionalParameters</code>
     */
    GetLegendGraphicKvpReader requestReader;

    /** test values for required parameters */
    Map<String, String> requiredParameters;

    /** test values for optional parameters */
    Map<String, String> optionalParameters;

    /** both required and optional parameters joint up */
    Map<String, String> allParameters;

    /** mock request */
    MockHttpServletRequest httpRequest;

    /** mock config object */
    WMS wms;

    /**
     * Remainder:
     *
     * <ul>
     *   <li>VERSION/Required
     *   <li>REQUEST/Required
     *   <li>LAYER/Required
     *   <li>FORMAT/Required
     *   <li>STYLE/Optional
     *   <li>FEATURETYPE/Optional
     *   <li>RULE/Optional
     *   <li>SCALE/Optional
     *   <li>SLD/Optional
     *   <li>SLD_BODY/Optional
     *   <li>WIDTH/Optional
     *   <li>HEIGHT/Optional
     *   <li>LANGUAGE/Optional
     *   <li>EXCEPTIONS/Optional
     * </ul>
     */
    @Before
    public void setParameters() throws Exception {
        requiredParameters = new HashMap<>();
        requiredParameters.put("VERSION", "1.0.0");
        requiredParameters.put("REQUEST", "GetLegendGraphic");
        requiredParameters.put("LAYER", "cite:Ponds");
        requiredParameters.put("FORMAT", "image/png");

        optionalParameters = new HashMap<>();
        optionalParameters.put("STYLE", "Ponds");
        optionalParameters.put("FEATURETYPE", "fake_not_used");
        // optionalParameters.put("RULE", "testRule");
        optionalParameters.put("SCALE", "1000");
        optionalParameters.put("WIDTH", "120");
        optionalParameters.put("HEIGHT", "90");
        optionalParameters.put("LANGUAGE", "en");
        // ??optionalParameters.put("EXCEPTIONS", "");
        allParameters = new HashMap<>(requiredParameters);
        allParameters.putAll(optionalParameters);

        wms = getWMS();

        this.requestReader = new GetLegendGraphicKvpReader(wms);
        this.httpRequest = createRequest("wms", allParameters);
    }

    /**
     * This test ensures that when a SLD parameter has been passed that refers to a SLD document
     * with multiple styles, the required one is choosed based on the LAYER parameter.
     *
     * <p>This is the case where a remote SLD document is used in "library" mode.
     */
    @org.junit.Test
    public void testRemoteSLDMultipleStyles() throws Exception {
        final URL remoteSldUrl = getClass().getResource("MultipleStyles.sld");
        this.allParameters.put("SLD", remoteSldUrl.toExternalForm());

        this.allParameters.put("LAYER", "cite:Ponds");
        this.allParameters.put("STYLE", "Ponds");

        GetLegendGraphicRequest request =
                requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);

        // the style names Ponds is declared in third position on the sld doc
        Style selectedStyle = request.getLegends().get(0).getStyle();
        assertNotNull(selectedStyle);
        assertEquals("Ponds", selectedStyle.getName());

        this.allParameters.put("LAYER", "cite:Lakes");
        this.allParameters.put("STYLE", "Lakes");

        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);

        // the style names Ponds is declared in third position on the sld doc
        selectedStyle = request.getLegends().get(0).getStyle();
        assertNotNull(selectedStyle);
        assertEquals("Lakes", selectedStyle.getName());
    }

    @org.junit.Test
    public void testMissingLayerParameter() throws Exception {
        requiredParameters.remove("LAYER");
        try {
            requestReader.read(
                    new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("LayerNotDefined", e.getCode());
        }
    }

    @org.junit.Test
    public void testMissingFormatParameter() throws Exception {
        requiredParameters.remove("FORMAT");
        try {
            requestReader.read(
                    new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("MissingFormat", e.getCode());
        }
    }

    @org.junit.Test
    public void testStrictParameter() throws Exception {
        GetLegendGraphicRequest request;

        // default value
        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertTrue(request.isStrict());

        allParameters.put("STRICT", "false");
        allParameters.remove("LAYER");
        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertFalse(request.isStrict());
    }

    @org.junit.Test
    public void testLayerGroup() throws Exception {
        GetLegendGraphicRequest request;

        request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertEquals(1, request.getLegends().size());

        requiredParameters.put("LAYER", NATURE_GROUP);
        request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertTrue(request.getLegends().size() > 1);
    }

    @org.junit.Test
    public void testLanguage() throws Exception {
        GetLegendGraphicRequest request;

        request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertNull(request.getLocale());

        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertEquals(Locale.ENGLISH, request.getLocale());
    }

    @org.junit.Test
    public void testStylesForLayerGroup() throws Exception {
        GetLegendGraphicRequest request;

        requiredParameters.put("LAYER", NATURE_GROUP);
        requiredParameters.put("STYLE", "style1,style2");
        request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertEquals(2, request.getLegends().size());
    }

    @org.junit.Test
    public void testRulesForLayerGroup() throws Exception {
        GetLegendGraphicRequest request;

        requiredParameters.put("LAYER", NATURE_GROUP);
        requiredParameters.put("RULE", "rule1,rule2");
        request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertEquals(2, request.getLegends().size());
    }

    @org.junit.Test
    public void testLabelsForLayerGroup() throws Exception {
        GetLegendGraphicRequest request;

        requiredParameters.put("LAYER", NATURE_GROUP);
        request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertNotNull(
                request.getLegend(new NameImpl("http://www.opengis.net/cite", "Lakes")).getTitle());
    }
}
