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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

/**
 * A TextField for {@code java.lang.Double} representation as a percentage.
 *
 * @author Jody Garnett (Boundless)
 */
public class PercentageTextField extends TextField<Double> {
    private static final long serialVersionUID = -4589385113632745745L;

    private int maximumFractionDigits = 1;

    private IConverter<Double> percentConverter =
            new IConverter<Double>() {
                private static final long serialVersionUID = -8409029711658542273L;

                @Override
                public String convertToString(Double value, Locale locale) {
                    NumberFormat format = formatter(locale);
                    return value == null ? null : format.format(value);
                }

                @Override
                public Double convertToObject(String value, Locale locale) {
                    if (value == null || value.trim().length() == 0) {
                        return null;
                    }
                    if (!value.endsWith("%")) {
                        value += "%";
                    }
                    NumberFormat format = formatter(locale);
                    Number parsed;
                    try {
                        parsed = format.parse(value);
                    } catch (ParseException e) {
                        error(e.getMessage());
                        return null;
                    }
                    return Double.valueOf(parsed.doubleValue());
                }
            };

    public PercentageTextField(String id) {
        super(id, Double.class);
    }

    public PercentageTextField(String id, IModel<Double> model) {
        super(id, model, Double.class);
    }

    private NumberFormat formatter(Locale locale) {
        NumberFormat format = NumberFormat.getPercentInstance(locale);
        format.setMaximumFractionDigits(maximumFractionDigits);

        return format;
    }

    public void setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (Double.class.isAssignableFrom(type)) {
            return (IConverter<C>) percentConverter;
        }
        return super.getConverter(type);
    }
}
