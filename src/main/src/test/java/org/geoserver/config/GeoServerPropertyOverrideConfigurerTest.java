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

package org.geoserver.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.easymock.EasyMock;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Assume;
import org.junit.Test;

public class GeoServerPropertyOverrideConfigurerTest {

    @Test
    public void testPropertyOverrider() {
        // on easymock 3.6 + jdk11 this test does not work, waiting for 3.7. to be released
        Assume.assumeFalse(SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9));

        // corner cases
        testPropertyOverride("", "", "");
        testPropertyOverride("some text", "data dir", "some text");
        testPropertyOverride("some ${GEOSERVER_DATA_DIR} text", "", "some  text");
        testPropertyOverride("some ${GEOSERVER_DATA_DIR} text", "\\$/", "some \\$/ text");

        // linux paths
        testPropertyOverride("before/${GEOSERVER_DATA_DIR}/after", "", "before//after");
        testPropertyOverride("${GEOSERVER_DATA_DIR}", "/linux/path/", "/linux/path/");
        testPropertyOverride(
                "before/${GEOSERVER_DATA_DIR}/after", "linux/path", "before/linux/path/after");
        testPropertyOverride(
                "before/a space/${GEOSERVER_DATA_DIR}/after/another space",
                "linux/path",
                "before/a space/linux/path/after/another space");
        testPropertyOverride(
                "before/a space/${GEOSERVER_DATA_DIR}/after/another space",
                "linux/a space/path",
                "before/a space/linux/a space/path/after/another space");

        // windows paths
        testPropertyOverride("before\\${GEOSERVER_DATA_DIR}\\after", "", "before\\\\after");
        testPropertyOverride("${GEOSERVER_DATA_DIR}", "\\linux\\path\\", "\\linux\\path\\");
        testPropertyOverride(
                "before\\${GEOSERVER_DATA_DIR}\\after",
                "linux\\path",
                "before\\linux\\path\\after");
        testPropertyOverride(
                "before\\a space\\${GEOSERVER_DATA_DIR}\\after\\another space",
                "linux\\path",
                "before\\a space\\linux\\path\\after\\another space");
        testPropertyOverride(
                "before\\a space\\${GEOSERVER_DATA_DIR}\\after\\another space",
                "linux\\a space\\path",
                "before\\a space\\linux\\a space\\path\\after\\another space");

        // non ascii paths
        testPropertyOverride(
                "/Entit\u00E9G\u00E9n\u00E9rique/${GEOSERVER_DATA_DIR}/\u901A\u7528\u5B9E\u4F53",
                "some\u00E4/\u00DFtext",
                "/Entit\u00E9G\u00E9n\u00E9rique/some\u00E4/\u00DFtext/\u901A\u7528\u5B9E\u4F53");
        testPropertyOverride(
                "\\Entit\u00E9G\u00E9n\u00E9rique\\${GEOSERVER_DATA_DIR}\\\u901A\u7528\u5B9E\u4F53",
                "some\u00E4\\\u00DFtext",
                "\\Entit\u00E9G\u00E9n\u00E9rique\\some\u00E4\\\u00DFtext\\\u901A\u7528\u5B9E\u4F53");
    }

    // Helper method that test that a GEOSERVER_DATA_DIR placeholder is correctly overridden by a
    // specific path
    private void testPropertyOverride(
            String property, String dataDirectoryPath, String expectedResult) {
        GeoServerPropertyOverrideConfigurer overrider = getOverriderForPath(dataDirectoryPath);
        String result = overrider.convertPropertyValue(property);
        if (expectedResult == null) {
            assertThat(result, nullValue());
        } else {
            assertThat(result, notNullValue());
            assertThat(result, is(expectedResult));
        }
    }

    // Helper method that creates an overrider instance that will use the specified data directory
    // path
    private GeoServerPropertyOverrideConfigurer getOverriderForPath(String dataDirectoryPath) {
        GeoServerDataDirectory dataDirectory = createGeoServerDataDirectoryMock(dataDirectoryPath);
        return new GeoServerPropertyOverrideConfigurer(dataDirectory);
    }

    // Helper method that creates a mocked GeoServer data directory allowing us to use a specific
    // path
    private GeoServerDataDirectory createGeoServerDataDirectoryMock(String path) {
        // we mock the file so linux paths are not convert in windows paths and vice-versa
        File mockedPath = EasyMock.createMock(File.class);
        EasyMock.expect(mockedPath.getPath()).andReturn(path).anyTimes();
        // mocked data directory that will use our mocked file
        GeoServerResourceLoader resourceLoader = EasyMock.createMock(GeoServerResourceLoader.class);
        EasyMock.expect(resourceLoader.getBaseDirectory()).andReturn(mockedPath).anyTimes();
        EasyMock.replay(mockedPath, resourceLoader);
        return new GeoServerDataDirectory(resourceLoader);
    }
}
