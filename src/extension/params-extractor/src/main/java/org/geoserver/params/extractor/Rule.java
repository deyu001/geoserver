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

import static org.geoserver.ows.util.ResponseUtils.urlDecode;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotools.util.logging.Logging;

public final class Rule {

    private static final Logger LOGGER = Logging.getLogger(Rule.class);

    private final String id;
    private final Boolean activated;
    private final Integer position;
    private final String match;
    private final String activation;
    private final String parameter;
    private final String transform;
    private final Integer remove;
    private final String combine;

    private final Pattern matchPattern;
    private final Pattern activationPattern;

    private final Boolean repeat;

    public Rule(
            String id,
            Boolean activated,
            Integer position,
            String match,
            String activation,
            String parameter,
            String transform,
            Integer remove,
            String combine,
            Boolean repeat,
            Pattern matchPattern,
            Pattern activationPattern) {
        this.id = id;
        this.activated = activated;
        this.position = position;
        this.match = match;
        this.activation = activation;
        this.parameter = parameter;
        this.transform = transform;
        this.remove = remove;
        this.combine = combine;
        this.repeat = repeat;
        this.matchPattern = matchPattern;
        this.activationPattern = activationPattern;
    }

    public UrlTransform apply(UrlTransform urlTransform) {
        if (!activated) {
            Utils.debug(LOGGER, "Rule %s is deactivated.", id, urlTransform);
            return urlTransform;
        }
        Utils.debug(LOGGER, "Start applying rule %s to URL '%s'.", id, urlTransform);
        if (activationPattern != null) {
            if (!activationPattern.matcher(urlTransform.getOriginalRequestUri()).matches()) {
                Utils.debug(LOGGER, "Rule %s doesn't apply to URL '%s'.", id, urlTransform);
                return urlTransform;
            }
        }
        Matcher matcher = matchPattern.matcher(urlTransform.getOriginalRequestUri());
        if (!matcher.matches()) {
            Utils.debug(LOGGER, "Rule %s doesn't match URL '%s'.", id, urlTransform);
            return urlTransform;
        }
        urlTransform.removeMatch(matcher.group(remove != null ? remove : 1));
        urlTransform.addParameter(
                parameter, urlDecode(matcher.replaceAll(transform)), combine, repeat);
        return urlTransform;
    }

    public String getId() {
        return id;
    }

    public Boolean getActivated() {
        return activated;
    }

    public Integer getPosition() {
        return position;
    }

    public String getMatch() {
        return match;
    }

    public String getActivation() {
        return activation;
    }

    public String getParameter() {
        return parameter;
    }

    public String getTransform() {
        return transform;
    }

    public Integer getRemove() {
        return remove;
    }

    public String getCombine() {
        return combine;
    }

    public Boolean getRepeat() {
        return repeat;
    }
}
