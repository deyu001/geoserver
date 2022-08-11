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

package org.geoserver.web;

import java.io.Serializable;
import org.apache.wicket.Component;

/**
 * Information about a component being plugged into a user interface.
 *
 * <p>Subclasses of this class are used to implement user interface "extension points". For an
 * example see {@link MenuPageInfo}.
 *
 * @author Andrea Aime, The Open Planning Project
 * @author Justin Deoliveira, The Open Planning Project
 * @param <C>
 */
@SuppressWarnings("serial")
public abstract class ComponentInfo<C extends Component> implements Serializable {

    /** the id of the component */
    String id;
    /** the title of the component */
    String title;
    /** The description of the component */
    String description;
    /** the class of the component */
    Class<C> componentClass;
    /** Controls access to the component */
    ComponentAuthorizer authorizer = ComponentAuthorizer.ALLOW;

    /** The id of the component. */
    public String getId() {
        return id;
    }
    /** Sets the id of the component. */
    public void setId(String id) {
        this.id = id;
    }
    /**
     * The i18n key for the title of the component.
     *
     * <p>The exact way this title is used depends one the component. For instance if the component
     * is a page, the title could be the used for a link to the page. If the component is a panel in
     * a tabbed panel, the title might be the label on the tab.
     */
    public String getTitleKey() {
        return title;
    }
    /** The i18n key for the title of the component. */
    public void setTitleKey(String title) {
        this.title = title;
    }

    /**
     * The i18n key for the description of the component.
     *
     * <p>This description is often used as a tooltip, or some contextual help.
     */
    public String getDescriptionKey() {
        return description;
    }

    /** Sets the description of the component. */
    public void setDescriptionKey(String description) {
        this.description = description;
    }

    /** The implementation class of the component. */
    public Class<C> getComponentClass() {
        return componentClass;
    }

    /** Sets the implementation class of the component. */
    public void setComponentClass(Class<C> componentClass) {
        this.componentClass = componentClass;
    }

    /** The authorizer that controls access to the component. */
    public ComponentAuthorizer getAuthorizer() {
        return authorizer;
    }

    /** Sets the authorizer that controls access to the component. */
    public void setAuthorizer(ComponentAuthorizer authorizer) {
        this.authorizer = authorizer;
    }
}
