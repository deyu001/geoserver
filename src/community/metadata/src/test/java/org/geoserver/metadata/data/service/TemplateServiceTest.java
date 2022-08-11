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
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test the template service.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class TemplateServiceTest extends AbstractMetadataTest {

    @Autowired private GeoServerDataDirectory dataDirectory;

    @After
    public void after() throws Exception {
        restoreTemplates();
        restoreLayers();
    }

    @Test
    public void testList() throws IOException {
        List<MetadataTemplate> actual = templateService.list();
        Assert.assertEquals(6, actual.size());
        Assert.assertEquals("simple fields", actual.get(0).getName());
        Assert.assertNotNull(actual.get(0).getMetadata());
    }

    @Test
    public void testLoad() throws IOException {
        MetadataTemplate actual = templateService.findByName("allData");

        Assert.assertNotNull(actual.getName());
        Assert.assertEquals("allData", actual.getName());
        Assert.assertNotNull(actual.getMetadata());
    }

    @Test
    public void testSave() throws IOException {
        Resource dir = dataDirectory.get(MetadataConstants.TEMPLATES_DIRECTORY);
        int nof = dir.list().size();

        MetadataTemplateImpl metadataTemplate = new MetadataTemplateImpl();
        metadataTemplate.setId(UUID.randomUUID().toString());
        metadataTemplate.setName("new-record");

        templateService.save(metadataTemplate);

        MetadataTemplate actual = templateService.findByName("new-record");
        Assert.assertEquals("new-record", actual.getName());
        Assert.assertNotNull(actual.getMetadata());

        // assert was stored in dir
        assertEquals(nof + 1, dir.list().size());
    }

    @Test
    public void testSaveErrorFlow() throws IOException {

        MetadataTemplateImpl metadataTemplate = new MetadataTemplateImpl();
        // id required
        try {
            templateService.save(metadataTemplate);
            Assert.fail("Should throw error");
        } catch (IllegalArgumentException ignored) {

        }
        metadataTemplate.setId("newTemplate");

        // name required
        try {
            templateService.save(metadataTemplate);
            Assert.fail("Should throw error");
        } catch (IllegalArgumentException ignored) {

        }
        // no duplicate names
        metadataTemplate.setName("allData");
        try {
            templateService.save(metadataTemplate);
            Assert.fail("Should throw error");
        } catch (IllegalArgumentException ignored) {
        }
    }

    /** Test if: 1) the template data is updated 2) the metadata for linked layers is updated. */
    @Test
    public void testUpdate() throws IOException {
        MetadataTemplate initial = templateService.findByName("simple fields");
        Assert.assertEquals("template-identifier", initial.getMetadata().get("identifier-single"));
        Assert.assertTrue(initial.getLinkedLayers().contains("mylayerFeatureId"));

        initial.getMetadata().put("identifier-single", "updated-value");

        // check if the linked metadata is updated.
        LayerInfo initialMyLayer = geoServer.getCatalog().getLayer("myLayerId");
        Serializable initialCustom = initialMyLayer.getResource().getMetadata().get("custom");
        @SuppressWarnings("unchecked")
        IModel<ComplexMetadataMap> initialMetadataModel =
                new Model<ComplexMetadataMap>(
                        new ComplexMetadataMapImpl((HashMap<String, Serializable>) initialCustom));
        Assert.assertEquals(
                1, initialMetadataModel.getObject().size("feature-catalog/feature-attribute/type"));

        templateService.save(initial);
        templateService.update(initial, null);

        MetadataTemplate actual = templateService.findByName("simple fields");
        Assert.assertEquals("updated-value", actual.getMetadata().get("identifier-single"));

        // check if the linked metadata is updated.
        LayerInfo myLayer = geoServer.getCatalog().getLayer("myLayerId");
        Serializable custom = myLayer.getResource().getMetadata().get("custom");
        @SuppressWarnings("unchecked")
        IModel<ComplexMetadataMap> metadataModel =
                new Model<ComplexMetadataMap>(
                        new ComplexMetadataMapImpl((HashMap<String, Serializable>) custom));

        Assert.assertEquals(
                "updated-value",
                metadataModel.getObject().get(String.class, "identifier-single").getValue());
        // only linked data from the linked template should change
        Assert.assertEquals(
                1, metadataModel.getObject().size("feature-catalog/feature-attribute/type"));
    }
}
