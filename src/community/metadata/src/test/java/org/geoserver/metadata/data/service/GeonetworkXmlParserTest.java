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

package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Test Import geonetwork. Test if the imported xml is mapped on the model in the correct way.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class GeonetworkXmlParserTest extends AbstractMetadataTest {

    @Autowired GeonetworkXmlParser xmlParser;

    @Autowired private GeoServerDataDirectory dataDirectory;

    @After
    public void after() throws Exception {
        restoreLayers();
    }

    @Test
    public void testMapping() throws IOException {
        MetadataMap metadataMap = new MetadataMap();
        ComplexMetadataMapImpl complexMetadataMap = new ComplexMetadataMapImpl(metadataMap);

        complexMetadataMap.get(String.class, "refsystem-as-list", 0).setValue("old-value");

        ResourceInfo rInfo = new FeatureTypeInfoImpl(null);

        Document fileAsResource =
                getDocument("geonetwork-1a2c6739-3c62-432b-b2a0-aaa589a9e3a1.xml");

        xmlParser.parseMetadata(fileAsResource, rInfo, complexMetadataMap);

        // simple single
        Assert.assertEquals(
                "1a2c6739-3c62-432b-b2a0-aaa589a9e3a1", metadataMap.get("identifier-single"));
        // simple list
        Serializable actualList = metadataMap.get("refsystem-as-list");
        Assert.assertTrue(actualList instanceof List);
        Assert.assertEquals(4, ((List<?>) actualList).size());
        Assert.assertEquals("Belge_Lambert_1972 (31370)", ((List<?>) actualList).get(0));
        Assert.assertEquals("TAW", ((List<?>) actualList).get(1));
        Assert.assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/3043", ((List<?>) actualList).get(2));
        assertEquals("G3Dv2_01_Q, dikte niet-tabulair Quartair", ((List<?>) actualList).get(3));

        // complex single
        Assert.assertEquals("EPSG", metadataMap.get("referencesystem-object/code-space"));
        Assert.assertEquals(
                "Belge_Lambert_1972 (31370)", metadataMap.get("referencesystem-object/code"));

        // complex list
        Serializable actualObjectCodeSpaceList =
                metadataMap.get("referencesystem-object-list/code-space");
        Assert.assertTrue(actualObjectCodeSpaceList instanceof List);
        Assert.assertEquals(6, ((List<?>) actualObjectCodeSpaceList).size());
        Assert.assertEquals("EPSG", ((List<?>) actualObjectCodeSpaceList).get(0));
        Assert.assertEquals("NGI", ((List<?>) actualObjectCodeSpaceList).get(1));
        Assert.assertEquals("EPSG", ((List<?>) actualObjectCodeSpaceList).get(2));

        Serializable actualObjectCodeList = metadataMap.get("referencesystem-object-list/code");
        Assert.assertTrue(actualObjectCodeList instanceof List);
        Assert.assertEquals(6, ((List<?>) actualObjectCodeList).size());
        Assert.assertEquals("Belge_Lambert_1972 (31370)", ((List<?>) actualObjectCodeList).get(0));
        Assert.assertEquals("TAW", ((List<?>) actualObjectCodeList).get(1));
        Assert.assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/3043",
                ((List<?>) actualObjectCodeList).get(2));

        // check equal sizes for complex repeatables
        List<?> names = (List<?>) metadataMap.get("contact/name");
        assertEquals(3, names.size());
        List<?> urls = (List<?>) metadataMap.get("contact/url");
        assertEquals(3, urls.size());

        // check multidimensional
        List<?> phones = (List<?>) metadataMap.get("contact/phone");
        assertEquals(3, phones.size());
        assertEquals(2, ((List<?>) phones.get(2)).size());

        // test native mappings
        assertEquals("G3Dv2_01_Q, dikte niet-tabulair Quartair", rInfo.getTitle());
        assertEquals(2, rInfo.getAlias().size());
        assertEquals(
                "Geologisch 3D model Vlaamse Ondergrond versie2- Dikte niet-tabulair Quartair",
                rInfo.getAlias().get(0));
        assertEquals("AndereTitel", rInfo.getAlias().get(1));
    }

    private Document getDocument(String fileName) throws IOException {
        for (Resource resource : dataDirectory.get("metadata").list()) {
            if (resource.name().equals(fileName)) {
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    InputStream stream = resource.in();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(stream);
                    doc.getDocumentElement().normalize();
                    return doc;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
        throw new IOException("Resource not found: " + fileName);
    }
}
