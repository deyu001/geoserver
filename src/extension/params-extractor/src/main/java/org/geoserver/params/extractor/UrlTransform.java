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

package org.geoserver.params.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.ows.util.ResponseUtils;

public class UrlTransform {

    private final Map<String, String> normalizedNames = new HashMap<>();
    private final Map<String, String[]> parameters = new HashMap<>();
    private final String requestUri;

    private final List<String> replacements = new ArrayList<>();

    public UrlTransform(String requestUri, Map<String, String[]> parameters) {
        this.requestUri = requestUri;
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            normalizedNames.put(entry.getKey().toLowerCase(), entry.getKey());
            this.parameters.put(
                    entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
        }
    }

    public String getOriginalRequestUri() {
        return requestUri;
    }

    public String getRequestUri() {
        String updatedRequestUri = requestUri;
        for (String replacement : replacements) {
            updatedRequestUri = updatedRequestUri.replace(replacement, "");
        }
        return updatedRequestUri;
    }

    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, String[]> parameter : parameters.entrySet()) {
            queryStringBuilder
                    .append(parameter.getKey())
                    .append("=")
                    .append(ResponseUtils.urlEncode(parameter.getValue()[0]))
                    .append("&");
        }
        if (queryStringBuilder.length() == 0) {
            return "";
        }
        queryStringBuilder.deleteCharAt(queryStringBuilder.length() - 1);
        return queryStringBuilder.toString();
    }

    public void addParameter(String name, String value, String combine, Boolean repeat) {
        String rawName = getRawName(name);
        String layersRawName = getRawName("layers");
        String[] existingValues = parameters.get(rawName);
        if ((existingValues != null || repeat) && combine != null) {
            int num = 1;
            if (repeat
                    && parameters.containsKey(layersRawName)
                    && parameters.get(layersRawName) != null) {
                num = parameters.get(layersRawName)[0].split(",").length;
            }
            String existingValue = existingValues == null ? null : existingValues[0];
            for (int count = 0; count < num; count++) {
                String combinedValue =
                        existingValue == null ? "$2" : combine.replace("$1", existingValue);
                combinedValue = combinedValue.replace("$2", value);
                existingValue = combinedValue;
            }
            parameters.put(rawName, new String[] {existingValue});
        } else {
            parameters.put(rawName, new String[] {value});
        }
    }

    private String getRawName(String name) {
        String rawName = normalizedNames.get(name.toLowerCase());
        if (rawName != null) {
            return rawName;
        }
        normalizedNames.put(name.toLowerCase(), name);
        return name;
    }

    public void removeMatch(String matchedText) {
        replacements.add(matchedText);
    }

    public Map<String, String[]> getParameters() {
        return parameters;
    }

    public boolean haveChanged() {
        return !(replacements.isEmpty());
    }

    @Override
    public String toString() {
        String updatedQueryString = getQueryString();
        if (updatedQueryString.isEmpty()) {
            return getRequestUri();
        }
        return getRequestUri() + "?" + getQueryString();
    }
}
