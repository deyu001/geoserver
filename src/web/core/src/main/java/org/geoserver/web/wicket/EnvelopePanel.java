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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A form component for a {@link Envelope} object.
 *
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, OpenGeo
 */
public class EnvelopePanel extends FormComponentPanel<ReferencedEnvelope> {

    private static final long serialVersionUID = -2975427786330616705L;

    protected Label minXLabel, minYLabel, maxXLabel, maxYLabel, minZLabel, maxZLabel;
    protected Double minX, minY, maxX, maxY, minZ, maxZ;
    protected DecimalTextField minXInput, minYInput, maxXInput, maxYInput, minZInput, maxZInput;

    protected CoordinateReferenceSystem crs;
    protected WebMarkupContainer crsContainer;
    protected CRSPanel crsPanel;
    protected boolean crsRequired;

    public EnvelopePanel(String id) {
        super(id);

        initComponents();
    }

    public EnvelopePanel(String id, ReferencedEnvelope e) {
        this(id, new Model<>(e));
    }

    public EnvelopePanel(String id, IModel<ReferencedEnvelope> model) {
        super(id, model);

        initComponents();
    }

    public void setCRSFieldVisible(boolean visible) {
        crsContainer.setVisible(visible);
    }

    public boolean isCRSFieldVisible() {
        return crsContainer.isVisible();
    }

    public boolean isCrsRequired() {
        return crsRequired;
    }

    /**
     * Makes the CRS bounds a required component of the envelope. It is warmly suggested that the
     * crs field be made visible too
     */
    public void setCrsRequired(boolean crsRequired) {
        this.crsRequired = crsRequired;
    }

    public boolean is3D() {
        return crs != null && crs.getCoordinateSystem().getDimension() >= 3;
    }

    public void setLabelsVisibility(boolean visible) {
        minXLabel.setVisible(visible);
        minYLabel.setVisible(visible);
        maxXLabel.setVisible(visible);
        maxYLabel.setVisible(visible);
        minZLabel.setVisible(visible && is3D());
        maxZLabel.setVisible(visible && is3D());
    }

    void initComponents() {
        updateFields();

        add(minXLabel = new Label("minXL", new ResourceModel("minX")));
        add(minYLabel = new Label("minYL", new ResourceModel("minY")));
        add(minZLabel = new Label("minZL", new ResourceModel("minZ")));
        add(maxXLabel = new Label("maxXL", new ResourceModel("maxX")));
        add(maxYLabel = new Label("maxYL", new ResourceModel("maxY")));
        add(maxZLabel = new Label("maxZL", new ResourceModel("maxZ")));

        add(minXInput = new DecimalTextField("minX", new PropertyModel<>(this, "minX")));
        add(minYInput = new DecimalTextField("minY", new PropertyModel<>(this, "minY")));
        add(minZInput = new DecimalTextField("minZ", new PropertyModel<>(this, "minZ")));
        add(maxXInput = new DecimalTextField("maxX", new PropertyModel<>(this, "maxX")));
        add(maxYInput = new DecimalTextField("maxY", new PropertyModel<>(this, "maxY")));
        add(maxZInput = new DecimalTextField("maxZ", new PropertyModel<>(this, "maxZ")));

        minZInput.setVisible(is3D());
        minZLabel.setVisible(is3D());
        maxZInput.setVisible(is3D());
        maxZLabel.setVisible(is3D());

        crsContainer = new WebMarkupContainer("crsContainer");
        crsContainer.setVisible(false);
        crsPanel = new CRSPanel("crs", new PropertyModel<>(this, "crs"));
        crsContainer.add(crsPanel);
        add(crsContainer);
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        ReferencedEnvelope e = getModelObject();
        if (e != null) {
            this.minX = e.getMinX();
            this.minY = e.getMinY();
            this.maxX = e.getMaxX();
            this.maxY = e.getMaxY();
            this.crs = e.getCoordinateReferenceSystem();
            if (is3D()) {
                if (e instanceof ReferencedEnvelope3D) {
                    this.minZ = ((ReferencedEnvelope3D) e).getMinZ();
                    this.maxZ = ((ReferencedEnvelope3D) e).getMaxZ();
                } else {
                    this.minZ = Double.NaN;
                    this.maxZ = Double.NaN;
                }
            } else {
                this.minZ = Double.NaN;
                this.maxZ = Double.NaN;
            }
        }
    }

    public EnvelopePanel setReadOnly(final boolean readOnly) {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    component.setEnabled(!readOnly);
                });
        crsPanel.setReadOnly(readOnly);

        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convertInput() {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField<String>) component).processInput();
                });

        if (isCRSFieldVisible()) {
            crsPanel.processInput();
        }

        // update the envelope model
        if (minX != null && maxX != null && minY != null && maxY != null) {
            if (crsRequired && crs == null) {
                setConvertedInput(null);
            } else {
                if (is3D()) {
                    double minZsafe = minZ == null ? Double.NaN : minZ;
                    double maxZsafe = maxZ == null ? Double.NaN : maxZ;
                    setConvertedInput(
                            new ReferencedEnvelope3D(
                                    minX, maxX, minY, maxY, minZsafe, maxZsafe, crs));
                } else {
                    setConvertedInput(new ReferencedEnvelope(minX, maxX, minY, maxY, crs));
                }
            }
        } else {
            setConvertedInput(null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onModelChanged() {
        // when the client programmatically changed the model, update the fields
        // so that the textfields will change too
        updateFields();
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField<String>) component).clearInput();
                });
    }

    /** Returns the coordinate reference system added by the user in the GUI, if any and valid */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }
}
