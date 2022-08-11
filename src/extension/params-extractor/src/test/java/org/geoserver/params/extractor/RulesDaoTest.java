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

import java.io.InputStream;
import java.util.List;
import org.junit.Test;

public final class RulesDaoTest extends TestSupport {

    @Test
    public void testParsingEmptyFile() throws Exception {
        doWork(
                "data/rules1.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(() -> inputStream);
                    assertThat(rules.size(), is(0));
                });
    }

    @Test
    public void testParsingEmptyRules() throws Exception {
        doWork(
                "data/rules2.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(() -> inputStream);
                    assertThat(rules.size(), is(0));
                });
    }

    @Test
    public void testParsingPositionRule() throws Exception {
        doWork(
                "data/rules3.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(() -> inputStream);
                    assertThat(rules.size(), is(1));
                    checkRule(
                            rules.get(0),
                            new RuleBuilder()
                                    .withId("0")
                                    .withPosition(3)
                                    .withParameter("cql_filter")
                                    .withRemove(1)
                                    .withTransform("seq='$2'")
                                    .build());
                });
    }

    @Test
    public void testParsingMatchRule() throws Exception {
        doWork(
                "data/rules4.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(() -> inputStream);
                    assertThat(rules.size(), is(1));
                    checkRule(
                            rules.get(0),
                            new RuleBuilder()
                                    .withId("0")
                                    .withMatch("^.*?(/([^/]+?))/[^/]+$")
                                    .withParameter("cql_filter")
                                    .withRemove(1)
                                    .withTransform("seq='$2'")
                                    .build());
                });
    }

    @Test
    public void testParsingMultipleRules() throws Exception {
        doWork(
                "data/rules5.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(() -> inputStream);
                    assertThat(rules.size(), is(3));
                    checkRule(
                            findRule("0", rules),
                            new RuleBuilder()
                                    .withId("0")
                                    .withPosition(3)
                                    .withParameter("cql_filter")
                                    .withRemove(1)
                                    .withTransform("seq='$2'")
                                    .build());
                    checkRule(
                            findRule("1", rules),
                            new RuleBuilder()
                                    .withId("1")
                                    .withMatch("^.*?(/([^/]+?))/[^/]+$")
                                    .withParameter("cql_filter")
                                    .withRemove(2)
                                    .withTransform("seq='$2'")
                                    .build());
                    checkRule(
                            findRule("2", rules),
                            new RuleBuilder()
                                    .withId("2")
                                    .withPosition(4)
                                    .withParameter("cql_filter")
                                    .withRemove(null)
                                    .withTransform("seq='$2'")
                                    .build());
                });
    }

    @Test
    public void testParsingCombineRepeatRule() throws Exception {
        doWork(
                "data/rules6.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(() -> inputStream);
                    assertThat(rules.size(), is(1));
                    checkRule(
                            rules.get(0),
                            new RuleBuilder()
                                    .withId("0")
                                    .withMatch("^.*?(/([^/]+?))/[^/]+$")
                                    .withParameter("cql_filter")
                                    .withRemove(1)
                                    .withTransform("seq='$2'")
                                    .withCombine("$1;$2")
                                    .withRepeat(true)
                                    .build());
                });
    }

    @Test
    public void testRuleCrud() {
        // create the rules to be used, rule C is an update of rule B (the id is the same)
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
                        .withId("1")
                        .withActivated(true)
                        .withMatch("^(?:/[^/]*){3}(/([^/]+)).*$")
                        .withParameter("cql_filter")
                        .withActivation("^.*$")
                        .withTransform("CFCC='$2'")
                        .withRemove(1)
                        .withCombine("$1 AND $2")
                        .build();
        Rule ruleC =
                new RuleBuilder()
                        .withId("1")
                        .withActivated(false)
                        .withMatch("^(?:/[^/]*){4}(/([^/]+)).*$")
                        .withParameter("cql_filter")
                        .withActivation("^.*$")
                        .withTransform("CFCC='$2'")
                        .withRemove(1)
                        .withCombine("$1 OR $2")
                        .build();
        // get the existing rules, this should return an empty list
        List<Rule> rules = RulesDao.getRules();
        assertThat(rules.size(), is(0));
        // we save rules A and B
        RulesDao.saveOrUpdateRule(ruleA);
        RulesDao.saveOrUpdateRule(ruleB);
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(2));
        checkRule(ruleA, findRule("0", rules));
        checkRule(ruleB, findRule("1", rules));
        // we update rule B using rule C
        RulesDao.saveOrUpdateRule(ruleC);
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(2));
        checkRule(ruleA, findRule("0", rules));
        checkRule(ruleC, findRule("1", rules));
        // we delete rule A
        RulesDao.deleteRules("0");
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleC, findRule("1", rules));
    }
}
