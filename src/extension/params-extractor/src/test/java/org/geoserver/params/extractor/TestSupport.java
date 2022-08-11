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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.util.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public abstract class TestSupport {

    private static final File TEST_DIRECTORY =
            new File(System.getProperty("java.io.tmpdir"), "params-extractor-data-directory");

    protected static final ApplicationContext APPLICATION_CONTEXT =
            new FileSystemXmlApplicationContext(
                    "file:"
                            + TestSupport.class
                                    .getClassLoader()
                                    .getResource("testApplicationContext.xml")
                                    .getFile());

    protected ResourceStore resourceStore;

    @Before
    public void voidSetup() throws IOException {
        deleteTestDirectoryQuietly();
        TEST_DIRECTORY.mkdir();
        resourceStore = new FileSystemResourceStore(TEST_DIRECTORY);
    }

    @After
    public void voidClean() throws IOException {
        deleteTestDirectoryQuietly();
    }

    private void deleteTestDirectoryQuietly() {
        try {
            IOUtils.delete(TEST_DIRECTORY);
        } catch (Exception exception) {
        }
    }

    protected static void doWork(String resourcePath, Consumer<InputStream> consumer)
            throws Exception {
        URL resource = EchoParametersDaoTest.class.getClassLoader().getResource(resourcePath);
        assertThat(resource, notNullValue());
        File file = new File(resource.getFile());
        assertThat(file.exists(), is(true));
        try (InputStream inputStream = new FileInputStream(file)) {
            if (inputStream.available() == 0) {
                return;
            }
            consumer.accept(inputStream);
        }
    }

    protected void checkRule(Rule ruleA, Rule ruleB) {
        assertThat(ruleA, notNullValue());
        assertThat(ruleB, notNullValue());
        checkValue(ruleA, ruleB, Rule::getId);
        checkValue(ruleA, ruleB, Rule::getActivated);
        checkValue(ruleA, ruleB, Rule::getPosition);
        checkValue(ruleA, ruleB, Rule::getMatch);
        checkValue(ruleA, ruleB, Rule::getParameter);
        checkValue(ruleA, ruleB, Rule::getActivation);
        checkValue(ruleA, ruleB, Rule::getTransform);
        checkValue(ruleA, ruleB, Rule::getRemove);
        checkValue(ruleA, ruleB, Rule::getCombine);
    }

    protected void checkEchoParameter(EchoParameter echoParameterA, EchoParameter echoParameterB) {
        assertThat(echoParameterA, notNullValue());
        assertThat(echoParameterB, notNullValue());
        checkValue(echoParameterA, echoParameterB, EchoParameter::getId);
        checkValue(echoParameterA, echoParameterB, EchoParameter::getActivated);
        checkValue(echoParameterA, echoParameterB, EchoParameter::getParameter);
    }

    protected <T, R> void checkValue(T objectA, T objectB, Function<T, R> getter) {
        R valueA = getter.apply(objectA);
        R valueB = getter.apply(objectB);
        if (valueA == null) {
            assertThat(valueB, nullValue());
        } else {
            assertThat(valueB, notNullValue());
            assertThat(valueA, is(valueB));
        }
    }

    protected EchoParameter findEchoParameter(String id, List<EchoParameter> rules) {
        return rules.stream()
                .filter(echoParameter -> echoParameter.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    protected Rule findRule(String id, List<Rule> rules) {
        return rules.stream().filter(rule -> rule.getId().equals(id)).findFirst().orElse(null);
    }
}
