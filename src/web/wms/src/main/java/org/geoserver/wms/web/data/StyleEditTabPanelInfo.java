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

package org.geoserver.wms.web.data;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.ComponentInfo;

/**
 * Information about panels plugged into additional tabs on style edit page.
 *
 * <p>Style edit tabs have a self declared order which describes where they end up on the style edit
 * page. Lower order panels are weighted toward the left hand side, higher order panels are weighted
 * toward the right hand side.
 */
public class StyleEditTabPanelInfo extends ComponentInfo<StyleEditTabPanel> {

    private static final long serialVersionUID = 4849692244366766812L;

    /** order of the panel with respect to other panels. */
    int order = -1;

    boolean enabledOnNew = true;

    /** Should this tab be enabled when creating a new style */
    public boolean isEnabledOnNew() {
        return enabledOnNew;
    }

    /** Returns the order of the panel. */
    public int getOrder() {
        return order;
    }

    /** Sets the order of the panel. */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * It may be that a tab contribution to the {@link AbstractStylePage} need to work on a
     * different model object that the page's layer and resource models (for example, because it
     * edits and saves related information not directly attached to the style); if such is the case,
     * this method shall return the model to be passed to the {@link StyleEditTabPanel} constructor.
     *
     * <p>This default implementation just returns {@code null} and assumes the {@link
     * StyleEditTabPanel} described by this tab panel info works against the {@link
     * AbstractStylePage} StyleInfo model. Subclasses may override as appropriate.
     *
     * @return {@code null} if no need for a custom model for the tab, the model to use otherwise
     * @see StyleEditTabPanel#save()
     */
    public IModel<?> createOwnModel(IModel<? extends StyleInfo> model, boolean isNew) {
        return null;
    }
}
