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

package org.geoserver.web.data.store.shape;

import static org.geotools.data.shapefile.ShapefileDataStoreFactory.CACHE_MEMORY_MAPS;
import static org.geotools.data.shapefile.ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX;
import static org.geotools.data.shapefile.ShapefileDataStoreFactory.DBFCHARSET;
import static org.geotools.data.shapefile.ShapefileDataStoreFactory.MEMORY_MAPPED;
import static org.geotools.data.shapefile.ShapefileDataStoreFactory.URLP;

import java.util.Map;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CharsetPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;

/**
 * Provides the form components for the shapefile datastore
 *
 * @author Andrea Aime - GeoSolution
 */
@SuppressWarnings("serial")
public class ShapefileStoreEditPanel extends StoreEditPanel {

    public ShapefileStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, Object>> paramsModel =
                new PropertyModel<>(model, "connectionParameters");

        Panel file = buildFileParamPanel(paramsModel);
        add(file);

        add(
                new CharsetPanel(
                        "charset",
                        new MapModel<>(paramsModel, DBFCHARSET.key),
                        new ParamResourceModel("charset", this),
                        false));

        add(
                new CheckBoxParamPanel(
                        "memoryMapped",
                        new MapModel<>(paramsModel, MEMORY_MAPPED.key),
                        new ParamResourceModel("memoryMapped", this)));
        add(
                new CheckBoxParamPanel(
                        "cacheMemoryMaps",
                        new MapModel<>(paramsModel, CACHE_MEMORY_MAPS.key),
                        new ParamResourceModel("cacheMemoryMaps", this)));

        add(
                new CheckBoxParamPanel(
                        "spatialIndex",
                        new MapModel<>(paramsModel, CREATE_SPATIAL_INDEX.key),
                        new ParamResourceModel("spatialIndex", this)));
    }

    protected Panel buildFileParamPanel(final IModel<Map<String, Object>> paramsModel) {
        FileParamPanel file =
                new FileParamPanel(
                        "url",
                        new MapModel<>(paramsModel, URLP.key),
                        new ParamResourceModel("shapefile", this),
                        true);
        file.setFileFilter(new Model<>(new ExtensionFileFilter(".shp")));
        file.getFormComponent().add(new FileExistsValidator());
        return file;
    }
}
