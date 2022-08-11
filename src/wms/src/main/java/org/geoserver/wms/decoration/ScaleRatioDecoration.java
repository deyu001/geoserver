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

package org.geoserver.wms.decoration;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import org.geoserver.wms.WMSMapContent;

public class ScaleRatioDecoration implements MapDecoration {

    String format = null;
    String formatLanguage = null;

    public void loadOptions(Map<String, String> options) {
        String format = options.get("format");
        if (format != null) {
            this.format = format;
        }
        String formatLanguage = options.get("formatLanguage");
        if (format != null) {
            this.formatLanguage = formatLanguage;
        }
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) {
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        return new Dimension(metrics.stringWidth(getScaleText(mapContent)), metrics.getHeight());
    }

    public double getScale(WMSMapContent mapContent) {
        return mapContent.getScaleDenominator(true);
    }

    public String getScaleText(WMSMapContent mapContent) {
        final double scale = getScale(mapContent);
        if (format == null) {
            return String.format("1 : %0$1.0f", scale);
        } else {
            DecimalFormatSymbols decimalFormatSymbols;
            if (formatLanguage != null) {
                decimalFormatSymbols = DecimalFormatSymbols.getInstance(new Locale(formatLanguage));
            } else {
                decimalFormatSymbols = DecimalFormatSymbols.getInstance();
            }
            return "1 : " + new DecimalFormat(format, decimalFormatSymbols).format(scale);
        }
    }

    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContent)
            throws Exception {
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        Dimension d =
                new Dimension(metrics.stringWidth(getScaleText(mapContent)), metrics.getHeight());
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();

        float x = (float) (paintArea.getMinX() + (paintArea.getWidth() - d.getWidth()) / 2.0);
        float y = (float) (paintArea.getMaxY() - (paintArea.getHeight() - d.getHeight()) / 2.0);
        Rectangle2D bgRect =
                new Rectangle2D.Double(
                        x - 3.0, y - d.getHeight(), d.getWidth() + 6.0, d.getHeight() + 6.0);
        g2d.setColor(Color.WHITE);
        g2d.fill(bgRect);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));

        g2d.drawString(getScaleText(mapContent), x, y);
        g2d.draw(bgRect);

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }
}
