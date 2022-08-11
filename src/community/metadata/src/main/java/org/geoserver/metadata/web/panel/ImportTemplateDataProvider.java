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

package org.geoserver.metadata.web.panel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * DataProvider that manages the list of linked templates for a layer.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class ImportTemplateDataProvider extends GeoServerDataProvider<MetadataTemplate> {

    private static final long serialVersionUID = -8246320435114536132L;

    public static final Property<MetadataTemplate> NAME =
            new BeanProperty<MetadataTemplate>("name", "name");

    public static final Property<MetadataTemplate> DESCRIPTION =
            new BeanProperty<MetadataTemplate>("description", "description");

    private IModel<List<MetadataTemplate>> selectedTemplates;

    public ImportTemplateDataProvider(IModel<List<MetadataTemplate>> selectedTemplates) {
        this.selectedTemplates = selectedTemplates;
    }

    @Override
    protected List<Property<MetadataTemplate>> getProperties() {
        return Arrays.asList(NAME, DESCRIPTION);
    }

    @Override
    protected List<MetadataTemplate> getItems() {
        return selectedTemplates.getObject();
    }

    public void addLink(MetadataTemplate modelObject) {
        selectedTemplates.getObject().add(modelObject);
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        selectedTemplates.getObject().sort(new MetadataTemplateComparator(service.list()));
    }

    public void removeLinks(List<MetadataTemplate> templates) {
        Iterator<MetadataTemplate> iterator = new ArrayList<>(templates).iterator();
        while (iterator.hasNext()) {
            MetadataTemplate modelObject = iterator.next();

            selectedTemplates.getObject().remove(modelObject);
        }
    }

    /** The remain values are used in the dropdown. */
    public List<MetadataTemplate> getUnlinkedItems() {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        List<MetadataTemplate> result = new ArrayList<>(service.list());
        result.removeAll(selectedTemplates.getObject());
        result.sort(new MetadataTemplateComparator(service.list()));
        return result;
    }

    private class MetadataTemplateComparator implements Comparator<MetadataTemplate> {

        private List<MetadataTemplate> list;

        public MetadataTemplateComparator(List<MetadataTemplate> list) {
            this.list = list;
        }

        public int compare(MetadataTemplate obj1, MetadataTemplate obj2) {
            int priority1 = Integer.MAX_VALUE;
            if (obj1 != null) {
                priority1 = list.indexOf(obj1);
            }
            int priority2 = Integer.MAX_VALUE;
            if (obj2 != null) {
                priority2 = list.indexOf(obj2);
            }
            return Integer.compare(priority1, priority2);
        }
    }
}
