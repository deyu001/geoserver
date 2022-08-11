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

package org.geoserver.metadata.web.panel.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeCollection;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;

public class AttributeDataProvider extends GeoServerDataProvider<AttributeConfiguration> {

    private static final long serialVersionUID = -4454769618643460913L;

    private static final Logger LOGGER = Logging.getLogger(AttributeDataProvider.class);

    public static Property<AttributeConfiguration> NAME =
            new BeanProperty<AttributeConfiguration>("name", "label");

    public static Property<AttributeConfiguration> VALUE =
            new AbstractProperty<AttributeConfiguration>("value") {
                private static final long serialVersionUID = -1889227419206718295L;

                @Override
                public Object getPropertyValue(AttributeConfiguration item) {
                    return null;
                }
            };

    private List<AttributeConfiguration> items = new ArrayList<>();

    private ResourceInfo rInfo;

    public AttributeDataProvider(ResourceInfo rInfo) {
        this.rInfo = rInfo;
        ConfigurationService metadataConfigurationService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        load(metadataConfigurationService.getMetadataConfiguration());
    }

    /** Provide attributes for the given complex type configuration. */
    public AttributeDataProvider(String typename, ResourceInfo rInfo) {
        this.rInfo = rInfo;
        ConfigurationService metadataConfigurationService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        AttributeCollection typeConfiguration =
                metadataConfigurationService.getMetadataConfiguration().findType(typename);
        if (typeConfiguration != null) {
            load(typeConfiguration);
        }
    }

    protected void load(AttributeCollection coll) {
        for (AttributeConfiguration config : coll.getAttributes()) {
            if (shouldDisplay(config)) {
                items.add(config);
            }
        }
    }

    private boolean shouldDisplay(AttributeConfiguration config) {
        if (config.getFieldType() == FieldTypeEnum.DERIVED) {
            return false; // don't display derived fields!
        }
        if (config.getCondition() != null && rInfo != null) {
            try {
                Object result = CQL.toExpression(config.getCondition()).evaluate(rInfo);
                if (!Boolean.TRUE.equals(result)) {
                    return false;
                }
            } catch (CQLException e) {
                LOGGER.log(Level.WARNING, "Failed to parse condition for " + config.getKey(), e);
            }
        }
        return true;
    }

    @Override
    protected List<Property<AttributeConfiguration>> getProperties() {
        return Arrays.asList(NAME, VALUE);
    }

    @Override
    protected List<AttributeConfiguration> getItems() {
        return items;
    }
}
