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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public final class RulesDao {

    private static final Logger LOGGER = Logging.getLogger(RulesDao.class);
    private static final SecureXStream xStream;

    static {
        xStream = new SecureXStream();
        xStream.registerConverter(new RuleConverter());
        xStream.alias("Rule", Rule.class);
        xStream.alias("Rules", RuleList.class);
        xStream.addImplicitCollection(RuleList.class, "rules");
        xStream.allowTypes(new Class[] {Rule.class, RuleList.class});
    }

    public static String getRulesPath() {
        return "params-extractor/extraction-rules.xml";
    }

    public static String getTempRulesPath() {
        return String.format("params-extractor/%s-extraction-rules.xml", UUID.randomUUID());
    }

    public static List<Rule> getRules() {
        Resource rules = getDataDirectory().get(getRulesPath());
        return getRules(() -> rules.in());
    }

    private static GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");
    }

    public static List<Rule> getRules(Supplier<InputStream> iss) {
        try (InputStream inputStream = iss.get()) {
            if (inputStream.available() == 0) {
                Utils.info(LOGGER, "Parameters extractor rules file seems to be empty.");
                return new ArrayList<>();
            }

            RuleList list = (RuleList) xStream.fromXML(inputStream);
            List<Rule> rules = list.rules == null ? new ArrayList<>() : list.rules;
            Utils.info(LOGGER, "Parameters extractor loaded %d rules.", rules.size());
            return rules;
        } catch (Exception exception) {
            throw Utils.exception(exception, "Error parsing rules files.");
        }
    }

    public static void saveOrUpdateRule(Rule rule) {
        Resource rules = getDataDirectory().get(getRulesPath());
        Resource tmpRules = getDataDirectory().get(getTempRulesPath());
        saveOrUpdateRule(rule, () -> rules.in(), () -> tmpRules.out());
        rules.delete();
        tmpRules.renameTo(rules);
    }

    public static void saveOrUpdateRule(
            Rule rule, Supplier<InputStream> iss, Supplier<OutputStream> oss) {
        List<Rule> rules = getRules(iss);
        boolean exists = false;
        for (int i = 0; i < rules.size() && !exists; i++) {
            if (rules.get(i).getId().equals(rule.getId())) {
                rules.set(i, rule);
                exists = true;
            }
        }
        if (!exists) {
            rules.add(rule);
        }
        writeRules(rules, oss);
        Utils.info(LOGGER, "Parameters extractor rules updated.");
    }

    public static void deleteRules(String... rulesIds) {
        Resource rules = getDataDirectory().get(getRulesPath());
        Resource tmpRules = getDataDirectory().get(getTempRulesPath());
        deleteRules(() -> rules.in(), () -> tmpRules.out(), rulesIds);
        rules.delete();
        tmpRules.renameTo(rules);
    }

    public static void deleteRules(
            Supplier<InputStream> inputStream,
            Supplier<OutputStream> outputStream,
            String... ruleIds) {
        List<Rule> rules =
                getRules(inputStream)
                        .stream()
                        .filter(rule -> !matchesAnyRuleId(rule, ruleIds))
                        .collect(Collectors.toList());
        writeRules(rules, outputStream);
        Utils.info(LOGGER, "Deleted one or more parameters extractor rules.");
    }

    private static boolean matchesAnyRuleId(Rule rule, String[] ruleIds) {
        return Arrays.stream(ruleIds).anyMatch(ruleId -> ruleId.equals(rule.getId()));
    }

    private static void writeRules(List<Rule> rules, Supplier<OutputStream> oss) {
        try (OutputStream outputStream = oss.get()) {
            xStream.toXML(new RuleList(rules), outputStream);
        } catch (Exception exception) {
            throw Utils.exception(exception, "Something bad happen when writing rules.");
        }
    }

    /** Support class for XStream serialization */
    static final class RuleList {
        List<Rule> rules;

        public RuleList(List<Rule> rules) {
            this.rules = rules;
        }
    }
}
