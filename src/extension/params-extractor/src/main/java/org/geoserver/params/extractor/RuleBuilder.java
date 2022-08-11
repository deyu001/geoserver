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

import java.util.regex.Pattern;

public final class RuleBuilder {

    private String id;
    private Boolean activated;
    private Integer position;
    private String match;
    private String parameter;
    private String activation;
    private String transform;
    private Integer remove;
    private String combine;
    private Boolean repeat;

    private Pattern matchPattern;
    private Pattern activationPattern;

    public RuleBuilder copy(Rule other) {
        this.id = other.getId();
        this.activated = other.getActivated();
        this.position = other.getPosition();
        if (position != null) {
            this.matchPattern =
                    Pattern.compile(String.format("^(?:/[^/]*){%d}(/([^/]+)).*$", position));
        } else {
            this.matchPattern = null;
        }
        this.match = other.getMatch();
        this.parameter = other.getParameter();
        this.activation = other.getActivation();
        if (activation != null) {
            this.activationPattern = Pattern.compile(activation);
        }
        this.transform = other.getTransform();
        this.remove = other.getRemove();
        this.combine = other.getCombine();

        return this;
    }

    public RuleBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RuleBuilder withActivated(Boolean activated) {
        this.activated = activated;
        return this;
    }

    public RuleBuilder withPosition(Integer position) {
        if (position != null) {
            this.position = position;
            matchPattern = Pattern.compile(String.format("^(?:/[^/]*){%d}(/([^/]+)).*$", position));
        }
        return this;
    }

    public RuleBuilder withMatch(String match) {
        if (match != null) {
            this.match = match;
            matchPattern = Pattern.compile(match);
        }
        return this;
    }

    public RuleBuilder withParameter(String parameter) {
        this.parameter = parameter;
        return this;
    }

    public RuleBuilder withActivation(String activation) {
        if (activation != null) {
            activationPattern = Pattern.compile(activation);
            this.activation = activation;
        }
        return this;
    }

    public RuleBuilder withRemove(Integer remove) {
        this.remove = remove;
        return this;
    }

    public RuleBuilder withTransform(String transform) {
        this.transform = transform;
        return this;
    }

    public RuleBuilder withCombine(String combine) {
        this.combine = combine;
        return this;
    }

    public RuleBuilder withRepeat(Boolean repeat) {
        if (repeat != null) {
            this.repeat = repeat;
        }
        return this;
    }

    public Rule build() {
        Utils.checkCondition(
                position == null || match == null,
                "Only one of the attributes position and match can be selected.");
        Utils.checkCondition(id != null && !id.isEmpty(), "Rule id cannot be NULL or EMPTY.");
        Utils.checkCondition(
                matchPattern != null, "Both attributes position or match cannot be NULL.");
        Utils.checkCondition(
                parameter != null && !parameter.isEmpty(),
                "Parameter attribute is mandatory it cannot be NULL or EMPTY.");
        Utils.checkCondition(
                transform != null && !transform.isEmpty(),
                "Transform attribute is mandatory it cannot be NULL or EMPTY.");
        return new Rule(
                id,
                Utils.withDefault(activated, true),
                position,
                match,
                activation,
                parameter,
                transform,
                remove,
                combine,
                Utils.withDefault(repeat, false),
                matchPattern,
                activationPattern);
    }
}
