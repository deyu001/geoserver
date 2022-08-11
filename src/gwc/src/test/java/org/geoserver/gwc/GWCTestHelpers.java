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

package org.geoserver.gwc;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class GWCTestHelpers {

    public static LayerInfoImpl mockLayer(
            String resourceName, String[] extraStyles, PublishedType type) {
        return mockLayer(resourceName, null, extraStyles, type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static LayerInfoImpl mockLayer(
            String resourceName, String workSpaceName, String[] extraStyles, PublishedType type) {
        StoreInfo store = new DataStoreInfoImpl(null);
        store.setName(resourceName + "-store");
        store.setEnabled(true);

        if (workSpaceName != null) {
            WorkspaceInfo workspace = new WorkspaceInfoImpl();
            workspace.setName(workSpaceName);
            store.setWorkspace(workspace);
        }

        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setPrefix("test");
        ns.setURI("http://example.com");
        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(null);
        resource.setName(resourceName);
        resource.setNamespace(ns);

        ReferencedEnvelope box = new ReferencedEnvelope(-180, 0, 0, 90, DefaultGeographicCRS.WGS84);
        resource.setLatLonBoundingBox(box);
        resource.setNativeBoundingBox(box);
        resource.setEnabled(true);
        resource.setStore(store);

        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId("id-" + resource.prefixedName());
        layer.setResource(resource);
        layer.setEnabled(true);

        StyleInfoImpl defaultStyle = new StyleInfoImpl(null);
        defaultStyle.setName("default");
        layer.setDefaultStyle(defaultStyle);

        if (extraStyles != null) {
            Set styles = new HashSet();
            for (String name : extraStyles) {
                StyleInfoImpl extra = new StyleInfoImpl(null);
                extra.setName(name);
                styles.add(extra);
            }
            layer.setStyles(styles);
        }

        layer.setType(type);

        return layer;
    }

    public static LayerGroupInfoImpl mockGroup(String name, PublishedInfo... layers) {
        LayerGroupInfoImpl lg = new LayerGroupInfoImpl();
        lg.setId("id-" + name);
        lg.setName(name);
        lg.setLayers(Lists.newArrayList(layers));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, DefaultGeographicCRS.WGS84));
        return lg;
    }
}
