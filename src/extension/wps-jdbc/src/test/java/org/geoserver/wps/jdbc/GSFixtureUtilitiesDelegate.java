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

package org.geoserver.wps.jdbc;

import java.io.File;
import java.util.Properties;
import org.geotools.test.FixtureUtilities;

/**
 * Static methods to support the implementation of tests that use fixture configuration files. See
 * {@link OnlineTestCase} and {@link OnlineTestSupport} for details. This slightly differ from
 * org.geotools.test.FixtureUtilities as it points to a different directory. This utilities delegate
 * most of its method call to FixtureUtilities except where directory location is concerned. Note:
 * Static method cannot be overridden hence this implementation.
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public class GSFixtureUtilitiesDelegate {

    /** Load {@link Properties} from a {@link File}. */
    public static Properties loadProperties(File file) {
        return FixtureUtilities.loadProperties(file);
    }

    /**
     * Return the directory containing GeoServer test fixture configuration files. This is
     * ".geoserver" in the user home directory.
     */
    public static File getFixtureDirectory() {
        return new File(System.getProperty("user.home") + File.separator + ".geoserver");
    }

    /**
     * Return the file that should contain the fixture configuration properties. It is not
     * guaranteed to exist.
     *
     * <p>
     *
     * Dots "." in the fixture id represent a subdirectory path under the GeoTools configuration
     * file directory. For example, an id <code>a.b.foo</code> would be resolved to
     * <code>.geotools/a/b/foo.properties<code>.
     *
     *            the base fixture configuration file directory, typically ".geotools" in the user
     *            home directory.
     *            the fixture id
     */
    public static File getFixtureFile(File fixtureDirectory, String fixtureId) {
        return FixtureUtilities.getFixtureFile(fixtureDirectory, fixtureId);
    }

    /**
     * Print a notice that tests are being skipped, identifying the property file whose absence is
     * responsible.
     *
     * @param fixtureId the fixture id
     * @param fixtureFile the missing fixture configuration file
     */
    public static void printSkipNotice(String fixtureId, File fixtureFile) {
        FixtureUtilities.printSkipNotice(fixtureId, fixtureFile);
    }

    /**
     * Return Properties loaded from a fixture configuration file, or null if not found.
     *
     * <p>If a fixture configuration file is not found, a notice is printed to standard output
     * stating that tests for this fixture id are skipped.
     *
     * <p>This method allows tests that cannot extend {@link OnlineTestCase} or {@link
     * OnlineTestSupport} because they already extend another class (for example, a non-online test
     * framework) to access fixture configuration files in the same way that those classes do. Only
     * basic fixture configuration loading is supported. This method does not support the extra
     * services such as fixture caching and connection testing provided by {@link OnlineTestCase}
     * and {@link OnlineTestSupport}.
     *
     * <p>A JUnit 4 test fixture can readily be disabled in the absence of a fixture configuration
     * file by placing <code>Assume.assumeNotNull(FixtureUtilities.loadFixture(fixtureId))</code> or
     * similar in its <code>@BeforeClass</code> method.
     *
     * @param fixtureId the fixture id, where dots "." are converted to subdirectories.
     * @return the fixture Properties or null
     * @see OnlineTestCase
     * @see OnlineTestSupport
     */
    public static Properties loadFixture(String fixtureId) {
        File fixtureFile = getFixtureFile(getFixtureDirectory(), fixtureId);
        if (fixtureFile.exists()) {
            return loadProperties(fixtureFile);
        } else {
            printSkipNotice(fixtureId, fixtureFile);
            return null;
        }
    }
}
