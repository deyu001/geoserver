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

package org.geoserver.web.data.resource;

import java.math.BigDecimal;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;

/**
 * Helps edit a time period
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class PeriodEditor extends FormComponentPanel<BigDecimal> {

    static final long yearMS = 31536000000L;

    static final long monthMS = 2628000000L;

    static final long weekMS = 604800000L;

    static final long dayMS = 86400000L;

    static final long hourMS = 3600000L;

    static final long minuteMS = 60000L;

    static final long secondMS = 1000L;

    int years;

    int months;

    int weeks;

    int days;

    int hours;

    int minutes;

    int seconds;

    public PeriodEditor(String id, IModel<BigDecimal> model) {
        super(id, model);
        initComponents();
    }

    void initComponents() {
        updateFields();

        final RangeValidator<Integer> validator = new RangeValidator<>(0, Integer.MAX_VALUE);
        add(new TextField<>("years", new PropertyModel<>(this, "years")).add(validator));
        add(new TextField<>("months", new PropertyModel<>(this, "months")).add(validator));
        add(new TextField<>("weeks", new PropertyModel<>(this, "weeks")).add(validator));
        add(new TextField<>("days", new PropertyModel<>(this, "days")).add(validator));
        add(new TextField<>("hours", new PropertyModel<>(this, "hours")).add(validator));
        add(new TextField<>("minutes", new PropertyModel<>(this, "minutes")).add(validator));
        add(new TextField<>("seconds", new PropertyModel<>(this, "seconds")).add(validator));
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        final BigDecimal modelObject = getModelObject();
        long time;
        if (modelObject != null) {
            time = modelObject.longValue();
        } else {
            time = 0;
        }
        years = (int) (time / yearMS);
        time %= yearMS;
        months = (int) (time / monthMS);
        time %= monthMS;
        weeks = (int) (time / weekMS);
        time %= weekMS;
        days = (int) (time / dayMS);
        time %= dayMS;
        hours = (int) (time / hourMS);
        time %= hourMS;
        minutes = (int) (time / minuteMS);
        time %= minuteMS;
        seconds = (int) (time / secondMS);
    }

    @Override
    public void convertInput() {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField) component).processInput();
                });

        long time =
                seconds * secondMS
                        + minutes * minuteMS
                        + hours * hourMS
                        + days * dayMS
                        + weeks * weekMS
                        + months * monthMS
                        + years * yearMS;
        setConvertedInput(new BigDecimal(time));
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
}
