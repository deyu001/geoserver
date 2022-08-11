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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.geoserver.params.extractor.web.RuleModel;
import org.geoserver.params.extractor.web.RulesModel;
import org.junit.Test;

public final class RulesModelTest extends TestSupport {

    @Test
    public void testCrudRuleModel() throws Exception {
        // create rules and echo parameters to be used (the rules have all the same)
        Rule ruleA =
                new RuleBuilder()
                        .withId("0")
                        .withActivated(true)
                        .withPosition(3)
                        .withParameter("cql_filter")
                        .withTransform("CFCC='$2'")
                        .build();
        Rule ruleB =
                new RuleBuilder()
                        .withId("0")
                        .withActivated(false)
                        .withPosition(2)
                        .withParameter("cql_filter")
                        .withTransform("CFCC='$2'")
                        .build();
        EchoParameter echoParameterA =
                new EchoParameterBuilder()
                        .withId("0")
                        .withParameter("cql_filter")
                        .withActivated(false)
                        .build();
        EchoParameter echoParameterB =
                new EchoParameterBuilder()
                        .withId("0")
                        .withParameter("cql_filter")
                        .withActivated(false)
                        .build();
        // save rule A
        RuleModel ruleModelA = new RuleModel(ruleA);
        checkRule(ruleA, ruleModelA.toRule());
        RulesModel.saveOrUpdate(ruleModelA);
        List<RuleModel> rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleA, rulesModels.get(0).toRule());
        List<Rule> rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleA, rules.get(0));
        // update rule A with rule B, an echo parameter should be produced
        RuleModel ruleModelB = new RuleModel(ruleB);
        ruleModelB.setEcho(true);
        checkRule(ruleB, ruleModelB.toRule());
        checkEchoParameter(echoParameterA, ruleModelB.toEchoParameter());
        RulesModel.saveOrUpdate(ruleModelB);
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleB, rulesModels.get(0).toRule());
        checkEchoParameter(echoParameterA, rulesModels.get(0).toEchoParameter());
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleB, rules.get(0));
        List<EchoParameter> echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(1));
        checkEchoParameter(echoParameterA, echoParameters.get(0));
        // updating the rule to make the parameter no echoed, the echo parameter should be removed
        ruleModelB.setEcho(false);
        RulesModel.saveOrUpdate(ruleModelB);
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleB, rulesModels.get(0).toRule());
        assertThat(rulesModels.get(0).getEcho(), is(false));
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleB, rules.get(0));
        echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(0));
        // creating echo parameter B, since the ids are the same the rule should contain an echo
        // parameter
        EchoParametersDao.saveOrUpdateEchoParameter(echoParameterB);
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleB, rulesModels.get(0).toRule());
        checkEchoParameter(echoParameterB, rulesModels.get(0).toEchoParameter());
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(1));
        // deleting rule everything should be deleted in cascade
        RulesModel.delete("0");
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(0));
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(0));
        echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(0));
    }
}
