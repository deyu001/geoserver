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

package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.SRSProvider.SRS;

/**
 * A panel which contains a list of all coordinate reference systems available to GeoServer.
 *
 * <p>Using this compontent in a page would look like:
 *
 * <pre>
 * public class MyPage {
 *
 *     public MyPage() {
 *     ...
 *     add( new SRSListPanel( &quot;srsList&quot; ) );
 *     ...
 *   }
 * }
 * </pre>
 *
 * And the markup:
 *
 * <pre>
 * ...
 *  &lt;body&gt;
 *    &lt;div wicket:id=&quot;srsList&gt;&lt;/div&gt;
 *  &lt;/body&gt;
 *  ...
 * </pre>
 *
 * <p>Client could should override the method {@link #createLinkForCode(String, IModel)} to provide
 * some action when the user clicks on a SRS code in the list.
 *
 * @author Andrea Aime, OpenGeo
 * @author Justin Deoliveira, OpenGeo
 * @authos Gabriel Roldan, OpenGeo
 */
public abstract class SRSListPanel extends Panel {

    private static final long serialVersionUID = 3777350932084160337L;
    GeoServerTablePanel<SRS> table;

    /** Creates the new SRS list panel. */
    public SRSListPanel(String id) {
        this(id, new SRSProvider());
    }

    /** Creates the new SRS list panel using SRS provider */
    public SRSListPanel(String id, SRSProvider srsProvider) {
        super(id);

        table =
                new GeoServerTablePanel<SRS>("table", srsProvider) {

                    private static final long serialVersionUID = 6182776235846912573L;

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<SRS> itemModel, Property<SRS> property) {

                        SRS srs = itemModel.getObject();

                        if (SRSProvider.CODE.equals(property)) {

                            Component linkForCode = createLinkForCode(id, itemModel);

                            return linkForCode;

                        } else if (SRSProvider.DESCRIPTION.equals(property)) {
                            String description = srs.getDescription();
                            return new Label(id, description.trim());

                        } else {
                            throw new IllegalArgumentException("Unknown property: " + property);
                        }
                    }
                };

        add(table);
    }

    /**
     * Hides the top pager so that the panel shows nicely in a small space (such as in a popup
     * window)
     */
    public void setCompactMode(boolean compact) {
        table.getTopPager().setVisible(!compact);
    }

    /**
     * Creates a link for an epsgCode.
     *
     * <p>Subclasses may override to perform an action when an epsg code has been selected. This
     * default implementation returns a link that does nothing.
     *
     * @param linkId The id of the link component to be created.
     * @param itemModel The epsg code (integer).
     */
    @SuppressWarnings("unchecked")
    protected Component createLinkForCode(String linkId, IModel<SRS> itemModel) {
        return new SimpleAjaxLink<Object>(
                linkId, (IModel<Object>) SRSProvider.CODE.getModel(itemModel)) {

            private static final long serialVersionUID = -1330723116026268069L;

            @Override
            protected void onClick(AjaxRequestTarget target) {
                onCodeClicked(target, getDefaultModelObjectAsString());
            }
        };
    }

    /** Suclasses must override and perform whatever they see fit when a SRS code link is clicked */
    protected abstract void onCodeClicked(AjaxRequestTarget target, String epsgCode);
}
