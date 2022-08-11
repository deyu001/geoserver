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

package org.geoserver.wcs.web.demo;

import java.awt.geom.AffineTransform;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * A form component for a {@link AffineTransform} object.
 *
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, GeoSolutions
 */
public class AffineTransformPanel extends FormComponentPanel<AffineTransform> {

    Double scaleX, shearX, originX, scaleY, shearY, originY;
    private WebMarkupContainer originXContainer;
    private WebMarkupContainer shearXContainer;
    private WebMarkupContainer originYContainer;
    private WebMarkupContainer shearYContainer;
    private WebMarkupContainer newline;

    public AffineTransformPanel(String id) {
        super(id);

        initComponents();
    }

    public AffineTransformPanel(String id, AffineTransform e) {
        this(id, new Model<>(e));
    }

    public AffineTransformPanel(String id, IModel<AffineTransform> model) {
        super(id, model);

        initComponents();
    }

    void initComponents() {
        updateFields();

        originXContainer = new WebMarkupContainer("originXContainer");
        add(originXContainer);
        newline = new WebMarkupContainer("newline");
        add(newline);
        shearXContainer = new WebMarkupContainer("shearXContainer");
        add(shearXContainer);
        originYContainer = new WebMarkupContainer("originYContainer");
        add(originYContainer);
        shearYContainer = new WebMarkupContainer("shearYContainer");
        add(shearYContainer);

        add(new TextField<>("scaleX", new PropertyModel<>(this, "scaleX")));
        shearXContainer.add(new TextField<>("shearX", new PropertyModel<>(this, "shearX")));
        originXContainer.add(new TextField<>("originX", new PropertyModel<>(this, "originX")));
        add(new TextField<>("scaleY", new PropertyModel<>(this, "scaleY")));
        shearYContainer.add(new TextField<>("shearY", new PropertyModel<>(this, "shearY")));
        originYContainer.add(new TextField<>("originY", new PropertyModel<>(this, "originY")));
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        AffineTransform at = getModelObject();
        if (at != null) {
            this.scaleX = at.getScaleX();
            this.shearX = at.getShearX();
            this.originX = at.getTranslateX();
            this.scaleY = at.getScaleY();
            this.shearY = at.getShearY();
            this.originY = at.getTranslateY();
        }
    }

    public AffineTransformPanel setReadOnly(final boolean readOnly) {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    component.setEnabled(!readOnly);
                });

        return this;
    }

    @Override
    public void convertInput() {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField) component).processInput();
                });

        // update the grid envelope
        if (isResolutionModeEnabled() && scaleX != null && scaleY != null) {
            setConvertedInput(AffineTransform.getScaleInstance(scaleX, scaleY));
        } else if (scaleX != null
                && shearX != null
                && originX != null
                && scaleY != null
                && shearY != null
                && originY != null) {
            setConvertedInput(
                    new AffineTransform(scaleX, shearX, shearY, scaleY, originX, originY));
        } else {
            setConvertedInput(null);
        }
    }

    @Override
    protected void onModelChanged() {
        // when the client programmatically changed the model, update the fields
        // so that the textfields will change too
        updateFields();
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField) component).clearInput();
                });
    }

    /** Turns the editor in a pure resolution editor */
    public void setResolutionModeEnabled(boolean enabled) {
        shearXContainer.setVisible(!enabled);
        shearYContainer.setVisible(!enabled);
        originXContainer.setVisible(!enabled);
        originYContainer.setVisible(!enabled);
        newline.setVisible(!enabled);
    }

    public boolean isResolutionModeEnabled() {
        return !shearXContainer.isVisible();
    }
}
