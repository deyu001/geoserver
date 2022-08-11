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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataAttributeModel;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;

/**
 * Factory to generate a component based on the configuration.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class EditorFactory {

    private static final EditorFactory instance = new EditorFactory();

    // private constructor to avoid client applications to use constructor
    private EditorFactory() {}

    public static EditorFactory getInstance() {
        return instance;
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration,
            String id,
            ComplexMetadataMap metadataMap,
            ResourceInfo rInfo) {
        IModel<T> model =
                new ComplexMetadataAttributeModel<T>(
                        metadataMap.get(getItemClass(configuration), configuration.getKey()));
        return create(
                configuration, id, model, metadataMap.subMap(configuration.getKey()), null, rInfo);
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration,
            String id,
            ComplexMetadataAttribute<T> metadataAttribute,
            ResourceInfo rInfo) {
        return create(configuration, id, metadataAttribute, null, rInfo);
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration,
            String id,
            ComplexMetadataAttribute<T> metadataAttribute,
            IModel<List<String>> selection,
            ResourceInfo rInfo) {
        IModel<T> model = new ComplexMetadataAttributeModel<T>(metadataAttribute);
        return create(
                configuration,
                id,
                model,
                new ComplexMetadataMapImpl(new HashMap<String, Serializable>()),
                selection,
                rInfo);
    }

    @SuppressWarnings("unchecked")
    private Component create(
            AttributeConfiguration configuration,
            String id,
            IModel<?> model,
            ComplexMetadataMap submap,
            IModel<List<String>> selection,
            ResourceInfo rInfo) {

        switch (configuration.getFieldType()) {
            case TEXT:
                return new TextFieldPanel(id, (IModel<String>) model);
            case NUMBER:
                return new NumberFieldPanel(id, (IModel<Integer>) model);
            case BOOLEAN:
                return new CheckBoxPanel(id, (IModel<Boolean>) model);
            case DROPDOWN:
                return new DropDownPanel(
                        id,
                        configuration.getKey(),
                        (IModel<String>) model,
                        configuration.getValues(),
                        selection);
            case TEXT_AREA:
                return new TextAreaPanel(id, (IModel<String>) model);
            case DATE:
                return new DateTimeFieldPanel(id, (IModel<Date>) model, false);
            case DATETIME:
                return new DateTimeFieldPanel(id, (IModel<Date>) model, true);
            case UUID:
                return new UUIDFieldPanel(id, (IModel<String>) model);
            case SUGGESTBOX:
                return new AutoCompletePanel(
                        id,
                        (IModel<String>) model,
                        configuration.getValues(),
                        false,
                        configuration,
                        selection);
            case REQUIREBOX:
                return new AutoCompletePanel(
                        id,
                        (IModel<String>) model,
                        configuration.getValues(),
                        true,
                        configuration,
                        selection);
            case COMPLEX:
                return new AttributesTablePanel(
                        id,
                        new AttributeDataProvider(configuration.getTypename(), rInfo),
                        new Model<ComplexMetadataMap>(submap),
                        null,
                        rInfo);
            default:
                break;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> Class<T> getItemClass(
            AttributeConfiguration attributeConfiguration) {
        switch (attributeConfiguration.getFieldType()) {
            case NUMBER:
                return (Class<T>) Integer.class;
            case DATE:
            case DATETIME:
                return (Class<T>) Date.class;
            case BOOLEAN:
                return (Class<T>) Boolean.class;
            default:
                break;
        }
        return (Class<T>) String.class;
    }
}
