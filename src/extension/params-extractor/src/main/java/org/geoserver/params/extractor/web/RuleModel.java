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

package org.geoserver.params.extractor.web;

import java.io.Serializable;
import java.util.UUID;
import org.geoserver.params.extractor.EchoParameter;
import org.geoserver.params.extractor.EchoParameterBuilder;
import org.geoserver.params.extractor.Rule;
import org.geoserver.params.extractor.RuleBuilder;

public class RuleModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private Integer position;
    private String match;
    private String activation;
    private String parameter;
    private String transform;
    private Integer remove;
    private String combine;
    private Boolean repeat;
    private boolean activated;
    private boolean echo;

    private boolean isForwardOnly;

    public RuleModel() {
        this(false);
    }

    public RuleModel(boolean isForwardOnly) {
        id = UUID.randomUUID().toString();
        activated = true;
        this.isForwardOnly = isForwardOnly;
    }

    public RuleModel(Rule rule) {
        id = rule.getId();
        activated = rule.getActivated();
        position = rule.getPosition();
        match = rule.getMatch();
        activation = rule.getActivation();
        parameter = rule.getParameter();
        transform = rule.getTransform();
        remove = rule.getRemove();
        combine = rule.getCombine();
        repeat = rule.getRepeat();
        if (position != null && transform != null) {
            transform = transform.replace("$2", "{PARAMETER}");
        }
    }

    public RuleModel(EchoParameter echoParameter) {
        id = echoParameter.getId();
        parameter = echoParameter.getParameter();
        activated = echoParameter.getActivated();
        echo = true;
        isForwardOnly = true;
    }

    public Rule toRule() {
        RuleBuilder ruleBuilder =
                new RuleBuilder()
                        .withId(id)
                        .withActivated(activated)
                        .withPosition(position)
                        .withMatch(match)
                        .withActivation(activation)
                        .withParameter(parameter)
                        .withRemove(remove)
                        .withCombine(combine)
                        .withRepeat(repeat);
        if (position != null && transform != null) {
            ruleBuilder.withTransform(transform.replace("{PARAMETER}", "$2"));
        } else {
            ruleBuilder.withTransform(transform);
        }
        return ruleBuilder.build();
    }

    public EchoParameter toEchoParameter() {
        return new EchoParameterBuilder()
                .withId(id)
                .withParameter(parameter)
                .withActivated(activated)
                .build();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getActivation() {
        return activation;
    }

    public void setActivation(String activation) {
        this.activation = activation;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public Integer getRemove() {
        return remove;
    }

    public void setRemove(Integer remove) {
        this.remove = remove;
    }

    public String getCombine() {
        return combine;
    }

    public void setCombine(String combine) {
        this.combine = combine;
    }

    public Boolean getRepeat() {
        return repeat;
    }

    public void setRepeat(Boolean repeat) {
        this.repeat = repeat;
    }

    public boolean getActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean getEcho() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    public boolean isEchoOnly() {
        return isForwardOnly;
    }
}
