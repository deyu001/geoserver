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

package org.geoserver.wms.map;

import static org.geoserver.template.TemplateUtils.FM_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OpenLayersMapTemplateTest extends WMSTestSupport {

    @Test
    public void test() throws Exception {
        Configuration cfg = TemplateUtils.getSafeConfiguration();
        cfg.setClassForTemplateLoading(OpenLayersMapOutputFormat.class, "");
        cfg.setObjectWrapper(new BeansWrapper(FM_VERSION));

        Template template = cfg.getTemplate("OpenLayers2MapTemplate.ftl");
        assertNotNull(template);

        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        WMSMapContent mapContent = new WMSMapContent();
        mapContent.addLayer(createMapLayer(MockData.BASIC_POLYGONS));
        mapContent.setRequest(request);
        mapContent.setMapWidth(256);
        mapContent.setMapHeight(256);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Map<String, Object> map = new HashMap<>();
        map.put("context", mapContent);
        map.put("request", mapContent.getRequest());
        map.put("maxResolution", Double.valueOf(0.0005)); // just a random number
        map.put("baseUrl", "http://localhost:8080/geoserver/wms");
        map.put("relBaseUrl", "//localhost:8080/geoserver/wms");
        map.put("parameters", new ArrayList<>());
        map.put("layerName", "layer");
        map.put("units", "degrees");
        map.put("pureCoverage", "false");
        map.put("supportsFiltering", "true");
        map.put("styles", new ArrayList<>());
        map.put("servicePath", "wms");
        map.put("yx", "false");
        template.process(map, new OutputStreamWriter(output));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        docBuilder.setEntityResolver(
                new EntityResolver() {

                    public InputSource resolveEntity(String publicId, String systemId)
                            throws SAXException, IOException {
                        StringReader reader =
                                new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                        InputSource source = new InputSource(reader);
                        source.setPublicId(publicId);
                        source.setSystemId(systemId);
                        return source;
                    }
                });

        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertNotNull(document);

        assertEquals("html", document.getDocumentElement().getNodeName());
    }
}
