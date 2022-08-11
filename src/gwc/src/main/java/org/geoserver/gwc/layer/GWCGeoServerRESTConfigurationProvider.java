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

package org.geoserver.gwc.layer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geowebcache.config.ContextualConfigurationProvider;
import org.geowebcache.config.Info;
import org.geowebcache.config.XMLConfigurationProvider;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.rest.exception.RestException;
import org.springframework.http.HttpStatus;

/**
 * GWC xml configuration {@link XMLConfigurationProvider contributor} so that GWC knows how to
 * marshal and unmarshal {@link GeoServerTileLayer} instances for its REST API.
 *
 * <p>Note this provider is different than {@link GWCGeoServerConfigurationProvider}, which is used
 * to save the configuration objects. In contrast, this one is used only for the GWC REST API, as it
 * doesn't distinguish betwee {@link TileLayer} objects and tile layer configuration objects (as the
 * GWC/GeoServer integration does with {@link GeoServerTileLayer} and {@link GeoServerTileLayerInfo}
 * ).
 */
public class GWCGeoServerRESTConfigurationProvider implements ContextualConfigurationProvider {

    private final Catalog catalog;

    public GWCGeoServerRESTConfigurationProvider(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public XStream getConfiguredXStream(XStream xs) {
        xs.alias("GeoServerLayer", GeoServerTileLayer.class);
        xs.processAnnotations(GeoServerTileLayerInfoImpl.class);
        xs.processAnnotations(StyleParameterFilter.class);
        xs.registerConverter(new RESTConverterHelper());
        xs.addDefaultImplementation(GeoServerTileLayerInfoImpl.class, GeoServerTileLayerInfo.class);

        // Omit the values cached from the backing layer.  They are only needed for the
        // persisted config file.
        xs.omitField(StyleParameterFilter.class, "availableStyles");
        xs.omitField(StyleParameterFilter.class, "defaultStyle");

        // Omit autoCacheStyles as it is no longer needed.
        // It'd be better to read it but not write it, but blocking it from REST is good enough and
        // a lot easier to get XStream to do.
        // TODO Remove this
        // xs.omitField(GeoServerTileLayerInfoImpl.class, "autoCacheStyles");
        return xs;
    }

    /** @author groldan */
    private final class RESTConverterHelper implements Converter {
        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
            return GeoServerTileLayer.class.equals(type);
        }

        @Override
        public GeoServerTileLayer unmarshal(
                HierarchicalStreamReader reader, UnmarshallingContext context) {

            Object current = new GeoServerTileLayerInfoImpl();
            Class<?> type = GeoServerTileLayerInfo.class;
            GeoServerTileLayerInfo info =
                    (GeoServerTileLayerInfo) context.convertAnother(current, type);
            String id = info.getId();
            String name = info.getName();
            if (id != null && id.length() == 0) {
                id = null;
            }
            if (name != null && name.length() == 0) {
                name = null;
            }
            if (name == null) { // name is mandatory
                throw new RestException("Layer name not provided", HttpStatus.BAD_REQUEST);
            }
            LayerInfo layer = null;
            LayerGroupInfo layerGroup = null;
            if (id != null) {
                layer = catalog.getLayer(id);
                if (layer == null) {
                    layerGroup = catalog.getLayerGroup(id);
                    if (layerGroup == null) {
                        throw new RestException(
                                "No GeoServer Layer or LayerGroup exists with id '" + id + "'",
                                HttpStatus.BAD_REQUEST);
                    }
                }
            } else {
                layer = catalog.getLayerByName(name);
                if (layer == null) {
                    layerGroup = catalog.getLayerGroupByName(name);
                    if (layerGroup == null) {
                        throw new RestException(
                                "GeoServer Layer or LayerGroup '" + name + "' not found",
                                HttpStatus.NOT_FOUND);
                    }
                }
            }

            final String actualId = layer != null ? layer.getId() : layerGroup.getId();
            final String actualName =
                    layer != null ? GWC.tileLayerName(layer) : GWC.tileLayerName(layerGroup);

            if (id != null && !name.equals(actualName)) {
                throw new RestException(
                        "Layer with id '"
                                + id
                                + "' found but name does not match: '"
                                + name
                                + "'/'"
                                + actualName
                                + "'",
                        HttpStatus.BAD_REQUEST);
            }

            info.setId(actualId);
            info.setName(actualName);

            GeoServerTileLayer tileLayer;
            final GridSetBroker gridsets = GWC.get().getGridSetBroker();
            if (layer != null) {
                tileLayer = new GeoServerTileLayer(layer, gridsets, info);
            } else {
                tileLayer = new GeoServerTileLayer(layerGroup, gridsets, info);
            }
            return tileLayer;
        }

        @Override
        public void marshal(
                /* GeoServerTileLayer */ Object source,
                HierarchicalStreamWriter writer,
                MarshallingContext context) {
            GeoServerTileLayer tileLayer = (GeoServerTileLayer) source;
            GeoServerTileLayerInfo info = tileLayer.getInfo();
            context.convertAnother(info);
        }
    }

    @Override
    public boolean appliesTo(Context ctxt) {
        return Context.REST == ctxt;
    }

    /**
     * @see ContextualConfigurationProvider#canSave(Info)
     *     <p>Always returns false, as persistence is not relevant for REST.
     * @param i Info to save
     * @return <code>false</code>
     */
    @Override
    public boolean canSave(Info i) {
        return false;
    }
}
