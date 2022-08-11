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
import org.apache.wicket.model.IModel;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class RepeatableComplexAttributeDataProvider
        extends GeoServerDataProvider<ComplexMetadataMap> {

    private static final long serialVersionUID = -255037580716257623L;

    public static String KEY_VALUE = "value";

    public static String KEY_REMOVE_ROW = "remove";

    public static String KEY_UPDOWN_ROW = "updown";

    public static final Property<ComplexMetadataMap> VALUE =
            new BeanProperty<ComplexMetadataMap>(KEY_VALUE, "value");

    private final Property<ComplexMetadataMap> REMOVE_ROW =
            new GeoServerDataProvider.BeanProperty<ComplexMetadataMap>(KEY_REMOVE_ROW, "");

    private final Property<ComplexMetadataMap> UPDOWN_ROW =
            new BeanProperty<ComplexMetadataMap>(KEY_UPDOWN_ROW, "");

    private IModel<ComplexMetadataMap> metadataModel;

    private AttributeConfiguration attributeConfiguration;

    private List<ComplexMetadataMap> items = new ArrayList<>();

    public RepeatableComplexAttributeDataProvider(
            AttributeConfiguration attributeConfiguration,
            IModel<ComplexMetadataMap> metadataModel) {
        this.metadataModel = metadataModel;
        this.attributeConfiguration = attributeConfiguration;

        reset();
    }

    public void reset() {
        items = new ArrayList<ComplexMetadataMap>();
        for (int i = 0; i < metadataModel.getObject().size(attributeConfiguration.getKey()); i++) {
            items.add(metadataModel.getObject().subMap(attributeConfiguration.getKey(), i));
        }
    }

    @Override
    protected List<Property<ComplexMetadataMap>> getProperties() {
        return Arrays.asList(VALUE, UPDOWN_ROW, REMOVE_ROW);
    }

    @Override
    protected List<ComplexMetadataMap> getItems() {
        return items;
    }

    public void addField() {
        ComplexMetadataMap item =
                metadataModel.getObject().subMap(attributeConfiguration.getKey(), items.size());
        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        service.init(item, attributeConfiguration.getTypename());
        items.add(item);
    }

    public void removeField(ComplexMetadataMap attribute) {
        int index = items.indexOf(attribute);
        // remove from model
        metadataModel.getObject().delete(attributeConfiguration.getKey(), index);
        // remove from view
        items.remove(index);
    }

    public AttributeConfiguration getConfiguration() {
        return attributeConfiguration;
    }
}
