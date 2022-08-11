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

package org.geoserver.wfs.response.dxf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Class storing a linetype description.
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public class LineType {
    String name = "";

    String description = "";

    LineTypeItem[] items = new LineTypeItem[0];

    public LineType(String name, String description) {
        super();
        this.name = name;
        this.description = description;
    }

    /** Gets the DXF name of the line type. */
    public String getName() {
        return name;
    }

    /** Sets DXF name of the line type. */
    public void setName(String name) {
        this.name = name;
    }

    /** Gets the pattern description for the line type. */
    public String getDescription() {
        return description;
    }

    /** Sets the pattern description for the line type. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Gets the pattern items. */
    public LineTypeItem[] getItems() {
        return items;
    }

    /** Sets the pattern items. */
    public void setItems(LineTypeItem[] items) {
        this.items = items;
    }

    /** Gets the complete length of the pattern. */
    public double getLength() {
        double len = 0.0;
        for (LineTypeItem item : items) len += Math.abs(item.getLength());
        return len;
    }

    /**
     * Parse a line type descriptor and returns a fully configured LineType object. A descriptor has
     * the following format: <name>!<repeatable pattern>[!<base length>], where <name> is the name
     * assigned to the line type, <base length> (optional) is a real number that tells how long is
     * each part of the line pattern (defaults to 0.125), and <repeatable pattern> is a visual
     * description of the repeatable part of the line pattern, as a sequence of - (solid line), *
     * (dot) and _ (empty space).
     */
    public static LineType parse(String ltype) {
        // split the descriptor in 2/3 parts
        String[] parts = ltype.split("!");
        // get the name
        String name = parts[0];
        // get the pattern/description
        String description = name;
        List<LineTypeItem> items = new ArrayList<>();
        // default base length
        double baseLen = 0.125;
        if (parts.length > 1) {
            // put spaces instead of underscores in description
            description = StringUtils.repeat(parts[1].replace('_', ' '), 5);
            // get the custom base length, if available
            if (parts.length > 2) baseLen = Double.parseDouble(parts[2]);
            // split the pattern using a regular expression
            Pattern p = Pattern.compile("[-]+|[*]+|[_]+");
            Matcher m = p.matcher(parts[1]);
            // analyze each part and build a LineTypeItem
            while (m.find()) {
                String piece = m.group(0);
                int type =
                        piece.startsWith("-")
                                ? LineTypeItem.DASH
                                : (piece.startsWith("*") ? LineTypeItem.DOT : LineTypeItem.EMPTY);
                LineTypeItem item = new LineTypeItem(type, piece.length() * baseLen);
                items.add(item);
            }
        }
        LineType result = new LineType(name, description);
        result.setItems(items.toArray(new LineTypeItem[] {}));
        return result;
    }
}
