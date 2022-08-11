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

package org.geoserver.web.data.layergroup;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogIntegrationTest;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public abstract class LayerGroupBaseTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // TODO: see GEOS-3040
        Catalog catalog = getCatalog();
        String lakes = MockData.LAKES.getLocalPart();
        String forests = MockData.FORESTS.getLocalPart();
        String bridges = MockData.BRIDGES.getLocalPart();

        setNativeBox(catalog, lakes);
        setNativeBox(catalog, forests);
        setNativeBox(catalog, bridges);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("lakes");
        lg.getLayers().add(catalog.getLayerByName(lakes));
        lg.getStyles().add(catalog.getStyleByName(lakes));
        lg.getLayers().add(catalog.getLayerByName(forests));
        lg.getStyles().add(catalog.getStyleByName(forests));
        lg.getLayers().add(catalog.getLayerByName(bridges));
        lg.getStyles().add(catalog.getStyleByName(bridges));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);

        WorkspaceInfo ws = catalog.getWorkspaceByName("cite");
        LayerGroupInfo wslg = catalog.getFactory().createLayerGroup();

        wslg.setName("bridges");
        wslg.setWorkspace(ws);
        wslg.getLayers().add(catalog.getLayerByName(bridges));
        wslg.getStyles().add(catalog.getStyleByName(bridges));
        builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(wslg);
        catalog.add(wslg);

        lg = catalog.getFactory().createLayerGroup();
        lg.setName("nestedLayerGroup");
        lg.getLayers().add(catalog.getLayerByName(lakes));
        lg.getStyles().add(catalog.getStyleByName(lakes));
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);

        testData.addStyle(
                "multiStyleGroup",
                "multiStyleGroup.sld",
                CatalogIntegrationTest.class,
                getCatalog());
        lg = catalog.getFactory().createLayerGroup();
        lg.setName("styleGroup");
        lg.getLayers().add(null);
        lg.getStyles().add(catalog.getStyleByName("multiStyleGroup"));
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }

    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(
                new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }
}
